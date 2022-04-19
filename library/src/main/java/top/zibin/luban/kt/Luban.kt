package top.zibin.luban.kt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import top.zibin.luban.Engine
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Suppress("unused", "UNUSED_PARAMETER")
class Luban(private var outputDir: File) {

    private lateinit var mInputStream: InputStream
    private var mLeastCompressSize: Long = 100 * 1024

    private var mReName: ((String) -> String)? = null
    private var mOutput: OutputStream? = null
    private var mFormat: Bitmap.CompressFormat? = null
    private var isBilinearInterpolationEnable = false
    private var mMaxSize: Long = 0L
    private var mQuality: Int = 60
    private var mKeepConfig = false

    companion object {
        const val DEFAULT_DISK_CACHE_DIR = "luban_disk_cache"

        fun with(context: Context): Luban {
            return Luban(File(context.cacheDir, DEFAULT_DISK_CACHE_DIR).apply { ensureDir() })
        }

        fun cacheDir(context: Context): File {
            return File(context.cacheDir, DEFAULT_DISK_CACHE_DIR)
        }
    }

    /**
     * 压缩
     */
    fun compress(func: Luban.() -> Unit): File? = run {
        this.func()
        this.get()
    }

    /**
     * 为适配分区存储，请不要传入外部存储的File或文件路径。
     * 应该直接使用uri或者请求相关权限获取inputStream后使用inputStream进行操作
     */
    fun input(
        inputStream: InputStream? = null,
        uri: Uri? = null,
        file: File? = null,
        path: String? = null,
        base64: String? = null, //waiting to support
        bitmap: String? = null,//waiting to support
        context: Context? = null,
    ) {
        mInputStream = inputStream
            ?: uri?.run {
                context?.contentResolver?.openInputStream(this)
                    ?: throw IllegalArgumentException("You must specify context")
            }
                    ?: file?.inputStream()
                    ?: path?.run { File(this) }?.inputStream()
                    ?: throw IllegalArgumentException("You need to specify at least one parameter")
    }

    /**
     * 存储目录
     * 默认cache/luban_disk_cache
     * 注：考虑到适配分区存储，请勿传入外部存储路径，应当申请相关权限后传入outputStream，在传入outputStream后不再向缓存目录输出文件
     */
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun output(
        dir: File? = null,
        path: String? = null,
        outputStream: OutputStream? = null,
    ) {
        if (dir == null && path.isNullOrEmpty() && outputStream == null) {
            throw IllegalArgumentException("You need to specify at least one parameter")
        }
        mOutput = outputStream ?: run {
            outputDir = dir ?: File(path)
            outputDir.ensureDir()
            null
        }
    }

    /**
     * 大小忽略
     */
    fun ignoreBy(size: Long, unit: MemoryUnit = MemoryUnit.KB) {
        this.mLeastCompressSize = when (unit) {
            MemoryUnit.BYTE -> size
            MemoryUnit.KB -> size * 1024
            MemoryUnit.MB -> size * 1048576
        }
    }

    /**
     * 重命名
     */
    fun rename(rename: (String) -> String) {
        this.mReName = rename
    }

    /**
     * 压缩格式
     */
    fun format(format: Bitmap.CompressFormat) {
        this.mFormat = format
    }

    /**
     * 限制最大大小
     */
    fun maxSize(size: Long, unit: MemoryUnit = MemoryUnit.KB) {
        this.mMaxSize = when (unit) {
            MemoryUnit.BYTE -> size
            MemoryUnit.KB -> size * 1024
            MemoryUnit.MB -> size * 1048576
        }
    }

    /**
     * 双线性压缩
     */
    fun bilinear(enable: Boolean = true) {
        this.isBilinearInterpolationEnable = enable
    }

    /**
     * 压缩质量 默认60
     */
    fun quality(q: Int) {
        if (q in 0..100) {
            this.mQuality = q
        } else {
            throw IllegalArgumentException("quality: You must specify value between 0 to 100")
        }
    }

    /**
     * 保持原有编码，比如不带透明的png会依然输出png，而不是为了保证大小压成jpg
     * 但会让自动降级策略失效
     */
    fun keepConfig(isKeep: Boolean) {
        this.mKeepConfig = isKeep
    }

    /**
     * 压缩数据
     */
    fun get(): File? {
        if (!this::mInputStream.isInitialized) {
            throw IllegalArgumentException("get: You must specify input stream from load method")
        }
        val length = mInputStream.available()

        val engine = Engine(
            mInputStream,
            outputDir,
            mReName,
            mOutput,
            mFormat,
            mQuality,
            mMaxSize,
            isBilinearInterpolationEnable,
            mKeepConfig
        )
        return if (length > mLeastCompressSize) {
            //compress
            engine.compress()
        } else {
            //copy
            engine.copy()
        }
    }
}