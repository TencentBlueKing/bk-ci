---
name: 20-audit-logging
description: 审计日志指南，涵盖操作审计记录、用户行为追踪、敏感操作日志、审计日志存储、合规性要求。当用户实现审计功能、记录用户操作、追踪敏感行为或满足合规要求时使用。
---

# 审计日志

审计日志指南.

## 触发条件

当用户需要实现操作审计、合规记录、行为追踪时，使用此 Skill。

## ActionAuditContent

```kotlin
object ActionAuditContent {
    // 项目相关
    const val PROJECT_CREATE_TEMPLATE = "[%s]创建项目[%s]"
    const val PROJECT_UPDATE_TEMPLATE = "[%s]更新项目[%s]"
    const val PROJECT_DELETE_TEMPLATE = "[%s]删除项目[%s]"
    
    // 流水线相关
    const val PIPELINE_CREATE_TEMPLATE = "[%s]在项目[%s]创建流水线[%s]"
    const val PIPELINE_EDIT_TEMPLATE = "[%s]编辑流水线[%s]"
    const val PIPELINE_DELETE_TEMPLATE = "[%s]删除流水线[%s]"
    const val PIPELINE_EXECUTE_TEMPLATE = "[%s]执行流水线[%s]"
    
    // 权限相关
    const val PERMISSION_GRANT_TEMPLATE = "[%s]授予[%s]权限[%s]"
    const val PERMISSION_REVOKE_TEMPLATE = "[%s]撤销[%s]权限[%s]"
}
```

## 审计记录

```kotlin
@Service
class AuditService(
    private val auditDao: AuditDao
) {
    fun record(
        userId: String,
        action: String,
        resourceType: String,
        resourceId: String,
        content: String,
        projectId: String? = null
    ) {
        val audit = AuditRecord(
            id = UUIDUtil.generate(),
            userId = userId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            content = content,
            projectId = projectId,
            createTime = LocalDateTime.now(),
            clientIp = getClientIp()
        )
        auditDao.save(audit)
    }
}
```

## 使用示例

```kotlin
@Service
class PipelineService(
    private val auditService: AuditService
) {
    fun createPipeline(userId: String, projectId: String, pipeline: Pipeline): String {
        // 创建流水线
        val pipelineId = doCreate(pipeline)
        
        // 记录审计
        auditService.record(
            userId = userId,
            action = "CREATE",
            resourceType = "PIPELINE",
            resourceId = pipelineId,
            content = String.format(
                ActionAuditContent.PIPELINE_CREATE_TEMPLATE,
                userId, projectId, pipeline.name
            ),
            projectId = projectId
        )
        
        return pipelineId
    }
}
```

## 审计字段

| 字段 | 说明 |
|------|------|
| userId | 操作用户 |
| action | 操作类型（CREATE/UPDATE/DELETE/EXECUTE） |
| resourceType | 资源类型 |
| resourceId | 资源ID |
| content | 操作描述 |
| projectId | 项目ID |
| clientIp | 客户端IP |
| createTime | 操作时间 |

## 最佳实践

1. **关键操作**：记录所有敏感操作
2. **完整信息**：包含足够的上下文信息
3. **不可篡改**：审计日志只增不改
4. **定期归档**：定期归档历史审计数据

## 相关文件

- `common-audit/src/main/kotlin/com/tencent/devops/common/audit/`
