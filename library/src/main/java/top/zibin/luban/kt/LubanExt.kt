package top.zibin.luban.kt

import java.io.File

enum class MemoryUnit {
    BYTE,
    KB,
    MB,
}


internal fun File?.ensureDir() {
    if (this == null) return
    if (exists()) {
        if (isFile) {
            deleteOnExit()
            mkdirs()
        }
    } else {
        mkdirs()
    }
}