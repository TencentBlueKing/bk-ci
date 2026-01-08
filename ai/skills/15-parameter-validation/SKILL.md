---
name: 15-parameter-validation
description: 参数校验指南，涵盖 JSR-303 注解校验、自定义校验器、分组校验、嵌套校验、错误消息国际化。当用户实现参数校验、编写自定义校验注解、处理校验错误或配置校验分组时使用。
---

# 参数校验

参数校验指南.

## 触发条件

当用户需要实现 API 参数校验、数据验证时，使用此 Skill。

## @BkField 注解

```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class BkField(
    val patternStyle: BkStyleEnum = BkStyleEnum.DEFAULT,
    val required: Boolean = false,
    val minLength: Int = -1,
    val maxLength: Int = -1,
    val message: String = ""
)
```

### 使用示例

```kotlin
interface UserPipelineResource {
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true, minLength = 1, maxLength = 64)
        userId: String,
        
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        projectId: String,
        
        @Parameter(description = "流水线名称", required = true)
        @BkField(required = true, minLength = 1, maxLength = 128)
        pipelineName: String
    ): Result<PipelineId>
}
```

## 校验模式

| 模式 | 说明 | 正则 |
|------|------|------|
| `DEFAULT` | 默认 | 无 |
| `CODE_STYLE` | 代码风格 | `^[a-zA-Z][a-zA-Z0-9_-]*$` |
| `ID_STYLE` | ID 风格 | `^[a-zA-Z0-9_-]+$` |

## ErrorCodeException

```kotlin
throw ErrorCodeException(
    statusCode = 400,
    errorCode = "2100013",
    defaultMessage = "无效参数",
    params = arrayOf(paramName)
)
```

### 常用错误码

| 错误码 | 说明 |
|--------|------|
| 2100001 | 系统内部错误 |
| 2100013 | 参数校验失败 |
| 2119042 | 资源不存在 |

## 手动校验

```kotlin
fun validateParams(projectId: String, pipelineId: String) {
    if (projectId.isBlank()) {
        throw ErrorCodeException(
            errorCode = "2100013",
            defaultMessage = "projectId 不能为空"
        )
    }
    if (pipelineId.length > 64) {
        throw ErrorCodeException(
            errorCode = "2100013",
            defaultMessage = "pipelineId 长度不能超过64"
        )
    }
}
```

## 最佳实践

1. **API 层校验**：在 Resource 接口层使用注解校验
2. **业务层校验**：复杂业务逻辑在 Service 层校验
3. **统一错误码**：使用项目定义的错误码
4. **友好提示**：提供清晰的错误信息

## 相关文件

- `common-web/src/main/kotlin/com/tencent/devops/common/web/annotation/BkField.kt`
- `common-api/src/main/kotlin/com/tencent/devops/common/api/exception/ErrorCodeException.kt`
