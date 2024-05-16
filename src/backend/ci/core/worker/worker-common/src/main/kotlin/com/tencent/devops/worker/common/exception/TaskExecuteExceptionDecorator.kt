package com.tencent.devops.worker.common.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteExceptionDecorator.LOGGER
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException

/**
 * 错误处理
 */
object TaskExecuteExceptionDecorator {

    val LOGGER: Logger = LoggerFactory.getLogger(TaskExecuteExceptionDecorator::class.java)

    private val defaultExceptionBase = DefaultExceptionBase()

    private val factory = mutableMapOf(
        IllegalStateException::class to IllegalStateExceptionD(),
        InvalidFormatException::class to InvalidFormatExceptionD(),
        FileNotFoundException::class to FileNotFoundExceptionD(),
        RemoteServiceException::class to RemoteServiceExceptionD(),
        MismatchedInputException::class to MismatchedInputExceptionD(),
        IOException::class to IOExceptionD(),
        FileSystemException::class to FileSystemExceptionD()
    )

    @Suppress("UNCHECKED_CAST")
    fun decorate(throwable: Throwable): TaskExecuteException {
        var exception = throwable
        return (factory[exception::class] as ExceptionDecorator<Throwable>? ?: exception.cause?.let { cause ->
            (factory[cause::class] as ExceptionDecorator<Throwable>?)?.let {
                exception = cause
                it
            }
        } ?: defaultExceptionBase).decorate(exception)
    }
}

class DefaultExceptionBase : ExceptionDecorator<Throwable> {
    override fun decorate(exception: Throwable): TaskExecuteException {
        return when {
            exception is TaskExecuteException -> exception
            // TEE只有一层，所以不遍历cause，防止InputMismatchException 无限循环。
            exception.cause is TaskExecuteException -> exception.cause as TaskExecuteException
            else -> {
                LOGGER.warn("[Worker Error]: ", exception)
                val defaultMessage = StringBuilder("Unknown system error has occurred with StackTrace:\n")
                defaultMessage.append(exception.toString())
                exception.stackTrace.forEach {
                    with(it) {
                        defaultMessage.append(
                            "\n    at $className.$methodName($fileName:$lineNumber)"
                        )
                    }
                }
                TaskExecuteException(
                    errorMsg = exception.message ?: defaultMessage.toString(),
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR,
                    cause = exception
                )
            }
        }
    }
}

interface ExceptionDecorator<T : Throwable> {
    fun decorate(exception: T): TaskExecuteException
}

class IllegalStateExceptionD : ExceptionDecorator<IllegalStateException> {
    override fun decorate(exception: IllegalStateException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Machine process error: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.THIRD_PARTY_BUILD_ENV_ERROR,
            cause = exception
        )
    }
}

class FileNotFoundExceptionD : ExceptionDecorator<FileNotFoundException> {
    override fun decorate(exception: FileNotFoundException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Machine file not found error: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            cause = exception
        )
    }
}

class RemoteServiceExceptionD : ExceptionDecorator<RemoteServiceException> {

    override fun decorate(exception: RemoteServiceException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "THIRD PARTY response error: ${exception.message}",
            errorType = ErrorType.THIRD_PARTY,
            errorCode = ErrorCode.THIRD_PARTY_INTERFACE_ERROR,
            cause = exception
        )
    }
}

class MismatchedInputExceptionD : ExceptionDecorator<MismatchedInputException> {
    override fun decorate(exception: MismatchedInputException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Plugin data is illegal: ${exception.message}",
            errorType = ErrorType.PLUGIN,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
            cause = exception
        )
    }
}

class InvalidFormatExceptionD : ExceptionDecorator<InvalidFormatException> {
    override fun decorate(exception: InvalidFormatException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Plugin data is illegal: ${exception.message}",
            errorType = ErrorType.PLUGIN,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
            cause = exception
        )
    }
}

class IOExceptionD : ExceptionDecorator<IOException> {
    override fun decorate(exception: IOException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "IO Exception: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            cause = exception
        )
    }
}

class FileSystemExceptionD : ExceptionDecorator<FileSystemException> {
    override fun decorate(exception: FileSystemException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "FileSystem Exception: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            cause = exception
        )
    }
}
