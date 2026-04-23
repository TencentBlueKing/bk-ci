# 装饰器模式（Decorator Pattern）

## 异常装饰器

**位置**：`worker-common/src/main/kotlin/com/tencent/devops/worker/common/exception/TaskExecuteExceptionDecorator.kt`

将各种异常统一装饰成 `TaskExecuteException`，便于统一处理和日志记录。

```kotlin
interface ExceptionDecorator<T : Throwable> {
    fun decorate(exception: T): TaskExecuteException
}

class DefaultExceptionBase : ExceptionDecorator<Throwable> {
    override fun decorate(exception: Throwable): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = exception.message ?: "Unknown error",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR
        )
    }
}

class FileNotFoundExceptionD : ExceptionDecorator<FileNotFoundException> {
    override fun decorate(exception: FileNotFoundException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "File not found: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
        )
    }
}

class RemoteServiceExceptionD : ExceptionDecorator<RemoteServiceException> {
    override fun decorate(exception: RemoteServiceException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Remote service error: ${exception.errorMessage}",
            errorType = ErrorType.THIRD_PARTY,
            errorCode = exception.errorCode
        )
    }
}
```

**装饰器注册与使用**：
```kotlin
object TaskExecuteExceptionDecorator {
    private val factory = mapOf(
        IllegalStateException::class to IllegalStateExceptionD(),
        FileNotFoundException::class to FileNotFoundExceptionD(),
        RemoteServiceException::class to RemoteServiceExceptionD(),
        IOException::class to IOExceptionD()
    )

    fun decorate(exception: Throwable): TaskExecuteException {
        val decorator = factory[exception::class] ?: DefaultExceptionBase()
        return decorator.decorate(exception)
    }
}
```

## 权限服务装饰器

**位置**：`auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/provider/rbac/service/DelegatingPermissionServiceDecorator.kt`

在原有权限服务基础上增加额外的权限检查逻辑，不修改原有服务代码。

```kotlin
class DelegatingPermissionServiceDecorator(
    private val delegate: PermissionService,
    private val extraCheckers: List<PermissionChecker>
) : PermissionService {

    override fun checkPermission(userId: String, resourceType: String, action: String): Boolean {
        if (!delegate.checkPermission(userId, resourceType, action)) {
            return false
        }
        return extraCheckers.all { it.check(userId, resourceType, action) }
    }
}
```
