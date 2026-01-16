---
name: 15-parameter-validation
description: 参数校验指南，涵盖 JSR-303 注解校验、自定义校验器、分组校验、嵌套校验、错误消息国际化。当用户实现参数校验、编写自定义校验注解、处理校验错误或配置校验分组时使用。
core_files:
  - "src/backend/ci/core/common/common-web/src/main/kotlin/com/tencent/devops/common/web/annotation/BkField.kt"
related_skills:
  - 02-api-interface-design
token_estimate: 1200
---

# 参数校验

## Quick Reference

```
校验注解：@BkField(required, minLength, maxLength, patternStyle)
异常抛出：ErrorCodeException(errorCode, defaultMessage, params)
校验模式：DEFAULT | CODE_STYLE | ID_STYLE
```

### 最简示例

```kotlin
interface UserPipelineResource {
    @POST
    fun create(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true, minLength = 1, maxLength = 64)
        userId: String,
        
        @PathParam("projectId")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        projectId: String,
        
        @BkField(required = true, minLength = 1, maxLength = 128)
        pipelineName: String
    ): Result<PipelineId>
}

// 手动校验
fun validate(projectId: String) {
    if (projectId.isBlank()) {
        throw ErrorCodeException(
            errorCode = "2100013",
            defaultMessage = "projectId 不能为空"
        )
    }
}
```

## When to Use

- API 参数校验
- 数据格式验证
- 业务规则校验

---

## @BkField 注解

```kotlin
annotation class BkField(
    val patternStyle: BkStyleEnum = BkStyleEnum.DEFAULT,
    val required: Boolean = false,
    val minLength: Int = -1,
    val maxLength: Int = -1,
    val message: String = ""
)
```

## 校验模式

| 模式 | 正则 |
|------|------|
| `CODE_STYLE` | `^[a-zA-Z][a-zA-Z0-9_-]*$` |
| `ID_STYLE` | `^[a-zA-Z0-9_-]+$` |

## 常用错误码

| 错误码 | 说明 |
|--------|------|
| 2100001 | 系统内部错误 |
| 2100013 | 参数校验失败 |

---

## Checklist

- [ ] API 层使用注解校验
- [ ] 复杂逻辑在 Service 层校验
- [ ] 使用项目定义的错误码
- [ ] 提供清晰的错误信息
