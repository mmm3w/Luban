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
    fun computeScaleSize(
        width: Int, //图片的宽度
        height: Int, //图片的高度
        baseLine: Float = 1280f, // 正常比例图片的缩放基准值
        //之后再额外增加长条图的基线数值
    ): Float {
        var scale = 1f //1920 * 1080
        val max = max(width, height)
        val min = min(width, height)
        val ratio = min / (max * 1f)
        if (ratio >= 0.5f) {
            if (max > baseLine) scale = baseLine / (max * 1f)
        } else { //长边是短边2倍以上长度的情况
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

    fun computeSize(
        width: Int, //图片的宽度
        height: Int, //图片的高度
        baseLine: Int = 1280,
    ): Int {
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
                else -> longSide / baseLine
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            if (longSide / baseLine == 0) 1 else longSide / baseLine
        } else {
            ceil(longSide / (baseLine.toDouble() / scale)).toInt()
        }
    }

    @Suppress("DEPRECATION")
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