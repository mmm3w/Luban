package top.zibin.luban.bumptech

import android.annotation.SuppressLint
import top.zibin.luban.bumptech.arraypool.LruArrayPool

internal object ArrayPoolProvider {
    @SuppressLint("VisibleForTests")
    val pool = LruArrayPool()

    fun get(bufferSize: Int): ByteArray {
        return pool.get(bufferSize, ByteArray::class.java)
    }

    fun put(ba: ByteArray) {
        pool.put(ba)
    }
}