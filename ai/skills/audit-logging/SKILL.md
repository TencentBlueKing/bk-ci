---
name: audit-logging
description: 审计日志指南，涵盖操作审计记录、用户行为追踪、敏感操作日志、审计日志存储、合规性要求。当用户实现审计功能、记录用户操作、追踪敏感行为或满足合规要求时使用。
core_files:
  - "src/backend/ci/core/common/common-audit/"
related_skills:
  - 14-i18n-and-logging
token_estimate: 1200
---

# 审计日志

## Quick Reference

```
审计模板：ActionAuditContent 对象
记录字段：userId, action, resourceType, resourceId, content, projectId, clientIp
原则：只增不改，定期归档
```

### 最简示例

```kotlin
object ActionAuditContent {
    const val PIPELINE_CREATE_TEMPLATE = "[%s]在项目[%s]创建流水线[%s]"
    const val PIPELINE_DELETE_TEMPLATE = "[%s]删除流水线[%s]"
}

@Service
class AuditService(private val auditDao: AuditDao) {
    fun record(userId: String, action: String, resourceType: String, 
               resourceId: String, content: String, projectId: String? = null) {
        auditDao.save(AuditRecord(
            id = UUIDUtil.generate(),
            userId = userId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            content = content,
            projectId = projectId,
            createTime = LocalDateTime.now(),
            clientIp = getClientIp()
        ))
    }
}

// 使用
auditService.record(
    userId = userId,
    action = "CREATE",
    resourceType = "PIPELINE",
    resourceId = pipelineId,
    content = String.format(ActionAuditContent.PIPELINE_CREATE_TEMPLATE, userId, projectId, name),
    projectId = projectId
)
```

## When to Use

- 敏感操作记录
- 合规审计
- 行为追踪

---

## 审计字段

| 字段 | 说明 |
|------|------|
| userId | 操作用户 |
| action | 操作类型（CREATE/UPDATE/DELETE/EXECUTE） |
| resourceType | 资源类型 |
| resourceId | 资源 ID |
| content | 操作描述 |
| clientIp | 客户端 IP |

---

## Checklist

- [ ] 记录所有敏感操作
- [ ] 包含足够的上下文信息
- [ ] 审计日志只增不改
- [ ] 定期归档历史数据
