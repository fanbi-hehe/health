package com.example.health.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
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
        // 1. 先读边界计算采样率，避免全尺寸解码 OOM
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw IllegalArgumentException("无法读取图片")

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_EDGE)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalArgumentException("无法解码图片")

        // 2. 按 EXIF 方向旋转（拍照照片方向修正）
        val rotation = readExifRotation(context, sourceUri)
        val oriented = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        // 3. 等比例缩放到长边上限
        val compressed = compressBitmap(oriented)
        if (compressed !== oriented) oriented.recycle()

        // 写入 cache 目录
        val tempFile = File(context.cacheDir, "food_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        compressed.recycle()

        return tempFile
    }

    /**
     * 计算采样率：在解码前按长边上限缩小，控制内存占用。
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxEdge && h / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /** 读取 EXIF 旋转角度（0/90/180/270）。 */
    private fun readExifRotation(context: Context, uri: Uri): Int {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it)
            } ?: return 0
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
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
