---
name: 14-i18n-and-logging
description: 国际化与日志规范指南
---

# 国际化与日志规范

国际化与日志规范指南.

## 触发条件

当用户需要实现多语言支持、日志记录、敏感信息脱敏时，使用此 Skill。

## 国际化（i18n）

### 配置文件位置

```
support-files/i18n/
├── message_zh_CN.properties    # 中文
├── message_en_US.properties    # 英文
├── message_ja_JP.properties    # 日文
└── process/
    ├── message_zh_CN.properties
    └── message_en_US.properties
```

### 错误码格式

```properties
# 格式：错误码=错误信息
2100001=系统内部繁忙，请稍后再试
2100002=参数错误: {0}
2119042=流水线不存在
```

### 错误码规范

```
21 (平台) + 01 (服务) + 001 (业务码)
```

| 服务 | 代码 |
|------|------|
| common | 00 |
| project | 01 |
| process | 19 |
| auth | 02 |

## 日志规范

### 日志级别

| 级别 | 场景 |
|------|------|
| DEBUG | 开发调试，生产不开启 |
| INFO | 关键业务信息 |
| WARN | 业务逻辑错误 |
| ERROR | 系统异常、外部调用异常 |

### 日志格式

```kotlin
// 使用管道符分隔
logger.info("QUALITY|pipelineCancelListener|buildId=${event.buildId}")
logger.error("BKSystemErrorMonitor|getBuildVariableValue|$pipelineId-$buildId|${error.message}")

// Logger 获取
companion object {
    private val logger = LoggerFactory.getLogger(ClassName::class.java)
}
```

### 敏感字段脱敏

```kotlin
// 使用 @SkipLogField 注解
data class NameAndValue(
    val key: String,
    @SkipLogField
    val value: String,  // 敏感字段
    @SkipLogField("valueType")
    @get:JsonProperty("valueType")
    val type: TestType
)

// 日志输出前脱敏
val logJsonString = JsonUtil.skipLogFields(bean)
```

## 最佳实践

1. **不记录敏感信息**：密码、Token、密钥等
2. **结构化日志**：使用管道符分隔关键信息
3. **错误日志带监控前缀**：`BKSystemErrorMonitor|`
4. **使用 Kotlin 字符串模板**：`$variable`

## 相关文件

- `support-files/i18n/` - 国际化配置
- `docs/specification/log_specification.md` - 日志规范
