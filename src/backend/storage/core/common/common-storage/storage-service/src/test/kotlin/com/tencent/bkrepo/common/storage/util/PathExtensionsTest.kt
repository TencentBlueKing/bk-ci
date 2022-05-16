package com.tencent.bkrepo.common.storage.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class PathExtensionsTest {
    private val dirPath: Path = createTempDir().toPath()

    @DisplayName("测试并发创建outputStream")
    @Test
    fun createNewOutputStream() {
        val filePath = dirPath.resolve("tmp")
        Assertions.assertEquals(false, Files.exists(filePath))
        val create = AtomicInteger()
        val failed = AtomicInteger()
        val count = 10
        val latch = CountDownLatch(count)
        repeat(count) {
            thread {
                try {
                    filePath.createNewOutputStream()
                    create.incrementAndGet()
                } catch (ex: FileAlreadyExistsException) {
                    failed.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        Assertions.assertEquals(true, Files.exists(filePath))
        Assertions.assertEquals(1, create.get())
        Assertions.assertEquals(count - 1, failed.get())
    }

    @DisplayName("测试删除路径")
    @Test
    fun deleteTest() {
        Assertions.assertEquals(true, Files.exists(dirPath))
        val filePath = dirPath.resolve("tmp")
        filePath.createFile()
        Assertions.assertEquals(false, dirPath.delete())
        Assertions.assertEquals(true, filePath.delete())
        Assertions.assertEquals(true, dirPath.delete())
        Assertions.assertEquals(false, Files.exists(dirPath))
    }
}
