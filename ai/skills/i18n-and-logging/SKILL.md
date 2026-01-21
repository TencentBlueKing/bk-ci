---
name: i18n-and-logging
description: 国际化与日志规范指南，涵盖 i18n 消息配置、多语言支持、日志级别规范、日志格式、敏感信息脱敏。当用户实现国际化、配置多语言、规范日志输出或处理日志脱敏时使用。
core_files:
  - "support-files/i18n/"
related_skills:
  - 12-performance-monitoring
token_estimate: 1200
---

# 国际化与日志规范

## Quick Reference

```
i18n 位置：support-files/i18n/message_{locale}.properties
错误码格式：21(平台) + 01(服务) + 001(业务码)
日志格式：logger.info("MODULE|action|key=value")
脱敏注解：@SkipLogField
```

### 最简示例

```properties
# support-files/i18n/message_zh_CN.properties
2100001=系统内部繁忙，请稍后再试
2100002=参数错误: {0}
```

```kotlin
// 日志规范
companion object {
    private val logger = LoggerFactory.getLogger(MyService::class.java)
}

// 结构化日志
logger.info("QUALITY|pipelineCancelListener|buildId=$buildId")
logger.error("BKSystemErrorMonitor|getBuildVariableValue|$pipelineId|${error.message}")

// 敏感字段脱敏
data class NameAndValue(
    val key: String,
    @SkipLogField
    val value: String  // 敏感字段，日志中不显示
)
val logJson = JsonUtil.skipLogFields(bean)
```

## When to Use

- 多语言支持
- 规范日志输出
- 敏感信息脱敏

---

## 错误码规范

```
21 (平台) + 01 (服务) + 001 (业务码)
```

| 服务 | 代码 |
|------|------|
| common | 00 |
| project | 01 |
| auth | 02 |
| process | 19 |

## 日志级别

| 级别 | 场景 |
|------|------|
| DEBUG | 开发调试，生产不开启 |
| INFO | 关键业务信息 |
| WARN | 业务逻辑错误 |
| ERROR | 系统异常、外部调用异常 |

---

## Checklist

- [ ] 不记录敏感信息（密码、Token）
- [ ] 使用管道符分隔结构化日志
- [ ] 错误日志带 `BKSystemErrorMonitor|` 前缀
- [ ] 敏感字段使用 @SkipLogField
