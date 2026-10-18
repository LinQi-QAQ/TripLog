package com.linqi.triplog.data.backup

import android.content.Context
import android.net.Uri
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDao
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.image.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 整库备份：导出一个 zip，里面含 data.json（全部行程）+ tickets/ 目录（车票图）。
 * 图片单独拷贝，json 里只存文件名，这样换机后路径变了也能对上。
 */
object BackupManager {

    const val JSON_NAME = "data.json"
    const val IMAGE_DIR = "tickets"
    const val MIME = "application/zip"
    const val FILE_PREFIX = "triplog-backup"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class BackupFile(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val trips: List<Trip> = emptyList()
    )

    data class ExportResult(val count: Int, val imageCount: Int, val bytes: Long)

    /**
     * 导出到用户选定的 uri（SAF），返回条数和图片数。
     * dao 参数默认取真实数据库，测试时传内存库的 dao。
     */
    suspend fun export(
        context: Context,
        target: Uri,
        dao: TripDao = TripDatabase.get(context).tripDao()
    ): ExportResult = withContext(Dispatchers.IO) {
        val trips = dao.getAll()

        // json 里的图片路径改成纯文件名，去掉本机绝对路径
        val portable = trips.map { trip ->
            trip.copy(ticketImagePath = trip.ticketImagePath.substringAfterLast('/'))
        }
        val payload = BackupFile(trips = portable)
        val jsonBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)

        var imageCount = 0
        context.contentResolver.openOutputStream(target)?.use { os ->
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_NAME))
                zip.write(jsonBytes)
                zip.closeEntry()

                for (trip in trips) {
                    val path = trip.ticketImagePath
                    if (path.isBlank()) continue
                    val file = File(path)
                    if (!file.exists()) continue
                    val name = file.name
                    zip.putNextEntry(ZipEntry("$IMAGE_DIR/$name"))
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                    imageCount++
                }
            }
        } ?: error("无法写入目标文件")

        ExportResult(trips.size, imageCount, jsonBytes.size.toLong())
    }

    /** 生成默认备份文件名，带时间戳，方便在 Downloads 里认。 */
    fun suggestedFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "$FILE_PREFIX-$stamp.zip"
    }

    data class ImportResult(val imported: Int, val imagesRestored: Int, val skipped: Int)

    /**
     * 从 zip 恢复。策略：清空现有数据再导入（自用 app，不做合并，
     * 合并逻辑复杂且容易产生重复，不如让用户先导出再导入）。
     */
    suspend fun import(
        context: Context,
        source: Uri,
        dao: TripDao = TripDatabase.get(context).tripDao()
    ): ImportResult = withContext(Dispatchers.IO) {

        var jsonText: String? = null
        val images = mutableMapOf<String, ByteArray>()

        context.contentResolver.openInputStream(source)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()
                        if (entry.name == JSON_NAME) {
                            jsonText = String(bytes, Charsets.UTF_8)
                        } else if (entry.name.startsWith("$IMAGE_DIR/")) {
                            images[entry.name.substringAfterLast('/')] = bytes
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("无法读取备份文件")

        val text = jsonText ?: error("备份文件里没有 $JSON_NAME，可能不是本 app 导出的")
        val parsed = json.decodeFromString<BackupFile>(text)

        // 图片先落盘，拿到新的绝对路径
        val imageDir = File(context.filesDir, ImageStore.DIR_NAME).apply { mkdirs() }
        val restoredPaths = mutableMapOf<String, String>()
        for ((name, bytes) in images) {
            val out = File(imageDir, name)
            out.writeBytes(bytes)
            restoredPaths[name] = out.absolutePath
        }

        // 清空旧数据，写入导入数据
        dao.clear()
        var imported = 0
        var imagesRestored = 0
        for (trip in parsed.trips) {
            val name = trip.ticketImagePath.substringAfterLast('/')
            val newPath = if (trip.ticketImagePath.isBlank()) {
                ""
            } else {
                restoredPaths[name] ?: ""
            }
            if (newPath.isNotBlank()) imagesRestored++
            dao.insert(
                trip.copy(
                    id = 0,
                    ticketImagePath = newPath,
                    reminded = false
                )
            )
            imported++
        }

        ImportResult(imported, imagesRestored, images.size - imagesRestored)
    }
}
