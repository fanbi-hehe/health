package com.example.health.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 图片压缩工具 —— 长边缩放 + JPEG 质量压缩，目标 100-300KB。
 */
object ImageCompressor {

    private const val MAX_LONG_EDGE = 1080
    private const val JPEG_QUALITY = 70

    /**
     * 从 Uri 读取、压缩、写入临时文件，返回临时文件。
     */
    fun compress(context: Context, sourceUri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val compressed = compressBitmap(original)
        original.recycle()

        // 写入 cache 目录
        val tempFile = File(context.cacheDir, "food_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        compressed.recycle()

        return tempFile
    }

    /**
     * 等比例缩放至长边不超过 MAX_LONG_EDGE。
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxEdge = maxOf(width, height)

        if (maxEdge <= MAX_LONG_EDGE) return bitmap

        val ratio = MAX_LONG_EDGE.toFloat() / maxEdge
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Bitmap → Base64（用于 AI API 调用）。
     */
    fun toBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
    }

    /**
     * File → Base64。
     */
    fun fileToBase64(file: File): String {
        val bytes = file.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
