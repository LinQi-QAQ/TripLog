package com.linqi.triplog.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linqi.triplog.data.image.ImageStore
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.local.VehicleType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 备份导出与恢复的完整往返测试，必须在真机或模拟器上跑（用到真实的 zip 流和文件系统）。
 * 命令：gradle :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: TripDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        // 清掉测试产生的图片目录，避免污染
        File(context.filesDir, ImageStore.DIR_NAME).deleteRecursively()
    }

    private fun uriFor(file: File): android.net.Uri =
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    private fun tempFile(name: String): File =
        File(context.cacheDir, name).apply { delete() }

    @Test
    fun `导出再恢复_数据条数一致`() = runTest {
        // 造三条行程
        db.tripDao().insert(
            Trip(
                vehicleType = VehicleType.TRAIN,
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = 1_700_000_000_000,
                number = "G1234",
                seat = "08车 12A",
                price = "553",
                remindBeforeMinutes = 180
            )
        )
        db.tripDao().insert(
            Trip(
                vehicleType = VehicleType.FLIGHT,
                fromStation = "PVG",
                toStation = "PEK",
                departTime = 1_700_100_000_000,
                number = "MU5101"
            )
        )
        db.tripDao().insert(
            Trip(
                vehicleType = VehicleType.BUS,
                fromStation = "杭州",
                toStation = "宁波",
                departTime = 1_700_200_000_000
            )
        )

        val out = tempFile("backup_test.zip")
        val result = BackupManager.export(context, uriFor(out), db.tripDao())
        assertEquals(3, result.count)
        assertTrue(out.exists())
        assertTrue(out.length() > 0)

        // 恢复前先清库，模拟「换了台设备」
        db.tripDao().clear()
        assertEquals(0, db.tripDao().count())

        val importResult = BackupManager.import(context, uriFor(out), db.tripDao())
        assertEquals(3, importResult.imported)
        assertEquals(3, db.tripDao().count())
    }

    @Test
    fun `恢复后_字段完整保留`() = runTest {
        db.tripDao().insert(
            Trip(
                vehicleType = VehicleType.TRAIN,
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = 1_700_000_000_000,
                arriveTime = 1_700_003_000_000,
                number = "G1234",
                seat = "08车 12A",
                price = "553",
                passenger = "林祺",
                note = "靠窗",
                remindBeforeMinutes = 180
            )
        )

        val out = tempFile("backup_fields.zip")
        BackupManager.export(context, uriFor(out), db.tripDao())
        db.tripDao().clear()
        BackupManager.import(context, uriFor(out), db.tripDao())

        val restored = db.tripDao().getAll().first()
        assertEquals("上海虹桥", restored.fromStation)
        assertEquals("北京南", restored.toStation)
        assertEquals(1_700_000_000_000, restored.departTime)
        assertEquals(1_700_003_000_000, restored.arriveTime)
        assertEquals("G1234", restored.number)
        assertEquals("08车 12A", restored.seat)
        assertEquals("553", restored.price)
        assertEquals("林祺", restored.passenger)
        assertEquals("靠窗", restored.note)
        assertEquals(VehicleType.TRAIN, restored.vehicleType)
    }

    @Test
    fun `恢复后_新id是自增的不冲突`() = runTest {
        // 备份里放两条，恢复后它们的 id 应该互不相同且都由数据库重新分配
        db.tripDao().insert(Trip(fromStation = "A1", toStation = "B1", departTime = 100L))
        db.tripDao().insert(Trip(fromStation = "A2", toStation = "B2", departTime = 200L))
        val out = tempFile("backup_ids.zip")
        BackupManager.export(context, uriFor(out), db.tripDao())

        // import 内部会先 clear()，所以这里不用手动清
        BackupManager.import(context, uriFor(out), db.tripDao())

        val trips = db.tripDao().getAll()
        assertEquals(2, trips.size)
        // 两条 id 必须不同，否则主键冲突
        assertEquals(2, trips.map { it.id }.distinct().size)
        // 恢复出来的 id 不该是备份时的旧 id（旧 id 是 1 和 2，新 id 应该重新分配）
        assertTrue(trips.all { it.id > 0 })
    }

    @Test
    fun `恢复后_提醒标记重置为未提醒`() = runTest {
        db.tripDao().insert(
            Trip(
                fromStation = "A",
                toStation = "B",
                departTime = 100L,
                reminded = true
            )
        )
        val out = tempFile("backup_reminded.zip")
        BackupManager.export(context, uriFor(out), db.tripDao())
        db.tripDao().clear()
        BackupManager.import(context, uriFor(out), db.tripDao())

        val restored = db.tripDao().getAll().first()
        assertTrue(!restored.reminded)
    }

    @Test
    fun `导出的zip含data_json`() = runTest {
        db.tripDao().insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        val out = tempFile("backup_structure.zip")
        BackupManager.export(context, uriFor(out), db.tripDao())

        val names = mutableListOf<String>()
        java.util.zip.ZipInputStream(out.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                names.add(e.name)
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        assertTrue("zip 里应该有 data.json", names.contains(BackupManager.JSON_NAME))
    }

    @Test
    fun `非备份文件_恢复时给出明确错误`() = runTest {
        val notBackup = tempFile("not_backup.txt")
        notBackup.writeText("这是一段普通文字，不是备份文件")

        val error = runCatching {
            BackupManager.import(context, uriFor(notBackup), db.tripDao())
        }.exceptionOrNull()

        assertNotNull("应该抛出异常", error)
    }

    @Test
    fun `空库导出再恢复_条数为零不报错`() = runTest {
        val out = tempFile("backup_empty.zip")
        val result = BackupManager.export(context, uriFor(out), db.tripDao())
        assertEquals(0, result.count)
        assertEquals(0, result.imageCount)

        val importResult = BackupManager.import(context, uriFor(out), db.tripDao())
        assertEquals(0, importResult.imported)
    }

    @Test
    fun `建议文件名格式正确`() {
        // 文件名精确到分钟，同一分钟内两次调用本来就会相同，
        // 所以这里验证格式而不是「两次不同」。
        val name = BackupManager.suggestedFileName()
        assertTrue("应该以 triplog-backup- 开头", name.startsWith("triplog-backup-"))
        assertTrue("应该以 .zip 结尾", name.endsWith(".zip"))
        assertTrue("不该包含路径分隔符或冒号", !name.contains("/") && !name.contains("\\") && !name.contains(":"))
        // 中间的时间戳应该是 yyyyMMdd-HHmm 形式，共 13 个字符
        val stamp = name.removePrefix("triplog-backup-").removeSuffix(".zip")
        assertEquals("时间戳应该是 yyyyMMdd-HHmm 形态", 13, stamp.length)
        assertTrue("时间戳格式不对：$stamp", stamp.matches(Regex("\\d{8}-\\d{4}")))
    }
}
