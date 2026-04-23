# 策略模式（Strategy Pattern）

## 流水线权限检查策略

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/strategy/`

```kotlin
interface IUserPipelinePermissionCheckStrategy {
    fun checkUserPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String? = null
    )
}

@Component
class UserNormalPipelinePermissionCheckStrategy : IUserPipelinePermissionCheckStrategy {
    override fun checkUserPipelinePermission(...) {
        // 正常流水线的权限检查逻辑
    }
}

@Component
class UserArchivedPipelinePermissionCheckStrategy : IUserPipelinePermissionCheckStrategy {
    override fun checkUserPipelinePermission(...) {
        // 归档流水线的权限检查逻辑
    }
}
```

**策略工厂**：
```kotlin
object UserPipelinePermissionCheckStrategyFactory {
    fun getStrategy(archived: Boolean): IUserPipelinePermissionCheckStrategy {
        return if (archived) {
            SpringContextUtil.getBean(UserArchivedPipelinePermissionCheckStrategy::class.java)
        } else {
            SpringContextUtil.getBean(UserNormalPipelinePermissionCheckStrategy::class.java)
        }
    }
}
```

**使用方式**：
```kotlin
val strategy = UserPipelinePermissionCheckStrategyFactory.getStrategy(pipeline.archived)
strategy.checkUserPipelinePermission(userId, projectId, pipelineId, AuthPermission.VIEW)
```

## 数据迁移策略

**位置**：`misc/biz-misc/src/main/kotlin/com/tencent/devops/misc/strategy/`

```kotlin
interface MigrationStrategy {
    fun migrate(projectId: String, pipelineId: String): Boolean
}
```

**实现类**（20+ 个）：
- `PipelineInfoMigrationStrategy` — 流水线基本信息迁移
- `PipelineSettingMigrationStrategy` — 流水线设置迁移
- `TemplatePipelineMigrationStrategy` — 模板流水线迁移
- `PipelineYamlInfoMigrationStrategy` — YAML 流水线迁移

每个策略负责特定数据的迁移，通过 Spring 容器管理，支持组合多个策略执行。
