package com.linqi.triplog.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片压缩与存储。
 * 识别用的图长边压到 1568px 以内（多数视觉模型的甜点区），JPEG 85 质量，
 * 存到应用私有目录 files/tickets/ 下，返回绝对路径。
 */
object ImageStore {

    /** 车票图存放目录名，备份模块也要用，暴露出来。 */
    const val DIR_NAME = "tickets"

    private const val MAX_EDGE = 1568
    private const val QUALITY = 85

    suspend fun saveCompressed(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(source) ?: return@withContext null
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val decoded = raw.use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext null
            val upright = applyExifRotation(context, source, decoded)
            val scaled = downscale(upright, MAX_EDGE)

            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val out = File(dir, "ticket_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos)
            }

            if (scaled !== decoded) scaled.recycle()
            if (upright !== decoded) upright.recycle()
            decoded.recycle()

            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            val degree = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degree == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(degree) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun downscale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longEdge = maxOf(w, h)
        if (longEdge <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longEdge.toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return bitmap.scale(nw, nh)
    }
}
