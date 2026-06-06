package com.mj.yaja.data.storage

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object JournalCacheFileOps {
    fun writeTextCrashSafely(target: File, backup: File, content: String) {
        val parent = target.parentFile ?: File(".")
        if (!parent.exists()) parent.mkdirs()
        val tempFile = File(parent, ".${target.name}.${System.nanoTime()}.tmp")
        FileOutputStream(tempFile).use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (target.exists()) {
            target.copyTo(backup, overwrite = true)
        }
        moveReplacing(tempFile, target)
    }

    private fun moveReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
