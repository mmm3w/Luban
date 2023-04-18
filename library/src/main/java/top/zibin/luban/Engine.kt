package top.zibin.luban

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import top.zibin.luban.bumptech.ArrayPoolProvider
import top.zibin.luban.bumptech.DefaultImageHeaderParser
import top.zibin.luban.bumptech.RecyclableBufferedInputStream
import java.io.*

class Engine(
    inputStream: InputStream,
    private val targetDir: File,
    private val rename: ((String) -> String)?,
    private val format: Bitmap.CompressFormat?,
    private val quality: Int,
    private val maxSize: Long = 0,
    private val bilinear: Boolean,
    private val keepConfig: Boolean,
    private val keepResolution: Boolean,
    private val baseSize: Int,
) {
    private val mInputStream: RecyclableBufferedInputStream =
        RecyclableBufferedInputStream(inputStream).apply {
            mark(5 * 1024 * 1024)
        }
    private val imageHeaderParser = DefaultImageHeaderParser()

    private var saveFile: File? = null

    @Suppress("ConvertTryFinallyToUseCall")
    fun compress(): ByteArrayOutputStream {
        mInputStream.reset()
        val type = imageHeaderParser.getType(mInputStream)

        mInputStream.reset()
        val angle = imageHeaderParser.getOrientation(mInputStream)

        mInputStream.reset()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(mInputStream, null, options)

        //邻近采样比例
        val inSampleSize =
            if (keepResolution) {
                1
            } else {
                if (bilinear) 1 else InternalCompact.computeSize(
                    options.outWidth,
                    options.outHeight,
                    baseSize
                )
                    .coerceAtLeast(1)
            }

        val scale =
            if (keepResolution) {
                1F
            } else {
                if (bilinear) InternalCompact.computeScaleSize(
                    options.outWidth,
                    options.outHeight,
                    baseSize.toFloat()
                ) else 1F
            }

        var compressConfig = if (keepConfig) {
            options.inPreferredConfig
        } else {
            if (type.hasAlpha()) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }

        val isAlpha = compressConfig == Bitmap.Config.ARGB_8888
        if (!InternalCompact.hasEnoughMemory(
                options.outWidth / inSampleSize,
                options.outHeight / inSampleSize,
                isAlpha
            )
        ) {
            //减低像素 减低内存
            if (keepConfig || !isAlpha || !InternalCompact.hasEnoughMemory(
                    options.outWidth / inSampleSize,
                    options.outHeight / inSampleSize,
                    false
                )
            ) {
                throw IllegalAccessException("image memory is too large")
            } else {
                compressConfig = Bitmap.Config.RGB_565
            }
        }

        val compressFormat = format ?: InternalCompact.formatHit(type)
        val bytes4Option = ArrayPoolProvider.get(16 * 1024)

        options.inPreferredConfig = compressConfig
        options.inSampleSize = inSampleSize
        options.inTempStorage = bytes4Option
        options.inJustDecodeBounds = false

        mInputStream.reset()
        var bitmap = BitmapFactory.decodeStream(mInputStream, null, options)
            ?: throw IOException("decodeStream error")
        bitmap = transformBitmap(bitmap, scale, angle)

        val fileName = "${System.nanoTime()}.${InternalCompact.suffixHit(type)}".let {
            rename?.invoke(it) ?: it
        }
        saveFile = File(targetDir, fileName)

        val stream = ByteArrayOutputStream()
        //质量压缩开始 这一步可能出错 比如手机只支持4096*4096做大尺寸
        try {
            bitmap.compress(compressFormat, quality, stream)
            //PNG等无损格式不支持压缩
            if (compressFormat != Bitmap.CompressFormat.PNG && maxSize > 0) {
                var tempQuality = quality
                //耗时由此处触发 每次降低6个点  图像显示效果和大小不能同时兼得 这里还要优化
                while (stream.size() > maxSize) {
                    tempQuality -= 5
                    if (tempQuality < 50) break
                    stream.reset()
                    bitmap.compress(compressFormat, tempQuality, stream)
                }
            }
        } finally {
            //位图释放
            bitmap.recycle()
            ArrayPoolProvider.put(bytes4Option)
        }

        try {
            return stream
        } finally {
            mInputStream.close()
        }
    }


    private fun transformBitmap(bitmap: Bitmap, scale: Float, angle: Int): Bitmap {
        if (scale == 1f && angle <= 0) return bitmap
        return try {
            val matrix = Matrix()
            //双线性压缩
            if (scale != 1f) {
                matrix.setScale(scale, scale)
            }
            //旋转角度处理
            if (angle > 0) {
                matrix.postRotate(angle.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } finally {
            System.gc()
        }
    }


    fun toBitmap(): Bitmap {
        return compress().use { stream ->
            val byteArray = stream.toByteArray()
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }

    fun copyBitmap(): Bitmap {
        return mInputStream.use { mInputStream ->
            BitmapFactory.decodeStream(mInputStream)
        }
    }

    fun toFile(): File {
        return compress().use { stream ->
            saveFile?.also {
                FileOutputStream(it).use { fos ->
                    stream.writeTo(fos)
                    fos.flush()
                }
            } ?: throw IllegalStateException()
        }
    }

    fun copyFile(): File {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(mInputStream, null, options)

        val suffix = options.outMimeType.replace("image/", "")
        val fileName = "${System.nanoTime()}.$suffix".let {
            rename?.invoke(it) ?: it
        }
        val cacheFile = File(targetDir, fileName)

        mInputStream.reset()
        mInputStream.use { mInputStream ->
            FileOutputStream(cacheFile).use { fos ->
                mInputStream.copyTo(fos)
            }
            return cacheFile
        }
    }

    fun toOutputStream(outputStream: OutputStream) {
        compress().use { bos ->
            outputStream.use { fos ->
                bos.writeTo(fos)
                fos.flush()
            }
        }
    }

    fun copyOutputStream(outputStream: OutputStream) {
        mInputStream.use { mInputStream ->
            outputStream.use { fos ->
                mInputStream.copyTo(fos)
            }
        }
    }

    fun toBase64(): String {
        return compress().use { stream ->
            Base64.encodeToString(stream.toByteArray(), Base64.NO_PADDING)
        }
    }

    fun copyBase64(): String {
        return mInputStream.use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_PADDING)
            }
        }
    }


}