package top.zibin.luban

import android.graphics.Bitmap
import android.os.Build
import top.zibin.luban.bumptech.ImageHeaderParser
import top.zibin.luban.kt.ensureDir
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal object InternalCompact {

    fun computeScaleSize(width: Int, height: Int): Float {
        var scale = 1f
        val max = max(width, height)
        val min = min(width, height)
        val ratio = min / (max * 1f)
        if (ratio >= 0.5f) {
            if (max > 1280f) scale = 1280f / (max * 1f)
        } else {
            val multiple = max / min
            if (multiple < 10) {
                if (min > 1000f && (1f - ratio / 2f) * min > 1000f) {
                    scale = 1f - ratio / 2f
                }
            } else {
                val arg = multiple.toDouble().pow(2.0).toInt()
                scale = 1f - arg / 1000f + if (multiple > 10) 0.01f else 0.03f
                if (min * scale < 640f) {
                    scale = 1f
                }
            }
        }
        return scale
    }

    fun computeSize(width: Int, height: Int): Int {
        val srcWidth = if (width % 2 == 1) width + 1 else width
        val srcHeight = if (height % 2 == 1) height + 1 else height

        val longSide: Int = srcWidth.coerceAtLeast(srcHeight)
        val shortSide: Int = srcWidth.coerceAtMost(srcHeight)

        val scale = shortSide.toFloat() / longSide

        return if (scale <= 1 && scale > 0.5625) {
            when {
                longSide < 1664 -> 1
                longSide < 4990 -> 2
                longSide in 4991..10239 -> 4
                else -> longSide / 1280
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            ceil(longSide / (1280.0 / scale)).toInt()
        }
    }

    fun formatHit(type: ImageHeaderParser.ImageType): Bitmap.CompressFormat {
        return when (type) {
            ImageHeaderParser.ImageType.PNG,
            ImageHeaderParser.ImageType.PNG_A,
            ImageHeaderParser.ImageType.GIF -> Bitmap.CompressFormat.PNG
            ImageHeaderParser.ImageType.WEBP_A,
            ImageHeaderParser.ImageType.WEBP
            -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            ImageHeaderParser.ImageType.JPEG,
            ImageHeaderParser.ImageType.RAW,
            ImageHeaderParser.ImageType.UNKNOWN -> Bitmap.CompressFormat.JPEG
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    fun suffixHit(type: ImageHeaderParser.ImageType): String {
        return when (type) {
            ImageHeaderParser.ImageType.PNG,
            ImageHeaderParser.ImageType.PNG_A -> "png"
            ImageHeaderParser.ImageType.GIF -> "gif"
            ImageHeaderParser.ImageType.WEBP_A,
            ImageHeaderParser.ImageType.WEBP -> "webp"
            ImageHeaderParser.ImageType.JPEG -> "jpg"
            ImageHeaderParser.ImageType.RAW -> "raw"
            ImageHeaderParser.ImageType.UNKNOWN -> "jpeg"
            else -> "jpeg"
        }
    }

    fun hasEnoughMemory(width: Int, height: Int, isAlpha32: Boolean) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            true
        } else {
            val runtime = Runtime.getRuntime()
            val free = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()
            val allocation = width * height shl if (isAlpha32) 2 else 1
            allocation < free
        }

    fun ensureDir(file: File) {
        file.ensureDir()
    }
}