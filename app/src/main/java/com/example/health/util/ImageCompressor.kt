package com.example.health.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
     *
     * 与 v0.1 行为一致：直接解码后缩放压缩（不采样、不处理 EXIF），
     * 避免部分机型在二次采样/EXIF 旋转时解码失败。
     */
    fun compress(context: Context, sourceUri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("无法读取图片")
        val original = inputStream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("无法解码图片")

        val compressed = compressBitmap(original)
        if (compressed !== original) original.recycle()

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
     * File → Base64。
     */
    fun fileToBase64(file: File): String {
        val bytes = file.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
