package com.tencent.devops.worker.common.exception

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class TaskExecuteExceptionDecoratorTest {

    @Test
    fun testUserException() {

        IOException("There is not enough space on the disk").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.USER, trustedException.errorType)
        }

        IllegalStateException("Process error").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.USER, trustedException.errorType)
        }

        FileNotFoundException("File not found").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.USER, trustedException.errorType)
        }

        FileSystemException(File(""), other = null, reason = " No space left on device").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.USER, trustedException.errorType)
        }
    }

    @Test
    fun testMaybeThirdPartyException() {
        RemoteServiceException("Service error").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.THIRD_PARTY, trustedException.errorType)
        }
    }

    @Test
    fun testMaybePluginException() {
        val mockJson = "{}"
        val mockMapper = ObjectMapper()
        val mockJsonParser = mockMapper.createParser(mockJson)

        InvalidFormatException(mockJsonParser, "mock", "mock", InvalidFormatException::class.java).let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.PLUGIN, trustedException.errorType)
        }

        val mockType = mockMapper.constructType(object : TypeReference<Map<String, Any>>() {})
        MismatchedInputException.from(mockJsonParser, mockType, "mock").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
            assertEquals(ErrorType.PLUGIN, trustedException.errorType)
        }
    }

    @Test
    fun testTaskExecuteException() {
        val taskExecuteException = TaskExecuteException(
            errorMsg = "default",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
        ).let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException)
            throwable
        }

        // test pack
        Throwable(taskExecuteException).let { throwable ->
            val trustedException2 = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException2)
            assertEquals(taskExecuteException, trustedException2)
        }
    }

    @Test
    fun testThrowablePackBaseException() {
        Throwable("bad").let { throwable ->
            val trustedException = TaskExecuteExceptionDecorator.decorate(throwable)
            assertNotNull(trustedException)
            assertEquals(throwable, trustedException.cause)
        }

        val msgNull: String? = null
        Throwable(msgNull).let { nullMsg ->
            val trustedException2 = TaskExecuteExceptionDecorator.decorate(nullMsg)
            assertNotNull(trustedException2)
            assertEquals(nullMsg, trustedException2.cause)
        }

        Throwable(Error("unknown")).let { unknownException ->
            val trustedException2 = TaskExecuteExceptionDecorator.decorate(unknownException)
            assertNotNull(trustedException2)
            assertEquals(unknownException, trustedException2.cause)
        }

        // 解析一层，发现
        Throwable(IOException("file not found")).let { fileNotFoundException ->
            val trustedException2 = TaskExecuteExceptionDecorator.decorate(fileNotFoundException)
            assertNotNull(trustedException2)
            assertEquals(fileNotFoundException.cause, trustedException2.cause)
        }
    }
}
