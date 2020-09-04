package com.tencent.devops.plugin.worker.task.scm.util

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object DirectoryUtil {

    fun deleteRecursively(path: Path) {
        Files.walkFileTree(
            path,
            object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun postVisitDirectory(
                    dir: Path,
                    e: IOException?
                ): FileVisitResult? {
                    return if (e == null) {
                        Files.delete(dir)
                        FileVisitResult.CONTINUE
                    } else {
                        // directory iteration failed
                        throw e
                    }
                }
            })
    }
}