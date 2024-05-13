package com.tencent.devops.worker.common.exception

import com.tencent.devops.common.api.pojo.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

class TaskExecuteExceptionDecoratorTest {

    @Test
    fun testException() {
        val e = IOException("There is not enough space on the disk")
        val trustedException = TaskExecuteExceptionDecorator.decorate(e)
        assertNotNull(trustedException)
        assertEquals(trustedException!!.cause, e)
        assertEquals(trustedException.errorCode, ErrorCode.USER_RESOURCE_NOT_FOUND)
    }

    @Test
    fun testFileSystemException() {
        val file = File("")
        val e = FileSystemException(file, other = null, reason = " No space left on device")
        val trustedException = TaskExecuteExceptionDecorator.decorate(e)
        assertNotNull(trustedException)
        assertEquals(trustedException!!.cause, e)
        assertEquals(trustedException.errorCode, ErrorCode.PLUGIN_DEFAULT_ERROR)
    }
}
