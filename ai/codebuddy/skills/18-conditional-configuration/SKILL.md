---
name: 18-conditional-configuration
description: 条件配置指南
---

# 条件配置

条件配置指南.

## 触发条件

当用户需要实现多实现切换、环境差异化配置、功能开关时，使用此 Skill。

## @ConditionalOnProperty

```kotlin
@Configuration
@ConditionalOnProperty(
    prefix = "auth",
    name = ["idProvider"],
    havingValue = "rbac"
)
class RbacConfiguration {
    @Bean
    fun permissionService(): PermissionService {
        return RbacPermissionService()
    }
}

@Configuration
@ConditionalOnProperty(
    prefix = "auth",
    name = ["idProvider"],
    havingValue = "sample",
    matchIfMissing = true  // 默认使用
)
class SampleConfiguration {
    @Bean
    fun permissionService(): PermissionService {
        return SamplePermissionService()
    }
}
```

## 配置示例

```yaml
# application.yml
auth:
  idProvider: rbac  # 或 sample

pipeline:
  permission:
    enabled: true
    strategy: stream  # 或 default
```

## 多实现切换

```kotlin
// 接口定义
interface PermissionService {
    fun check(userId: String, resource: String): Boolean
}

// RBAC 实现
@Service
@ConditionalOnProperty(name = ["auth.idProvider"], havingValue = "rbac")
class RbacPermissionService : PermissionService {
    override fun check(userId: String, resource: String): Boolean {
        // RBAC 权限检查
    }
}

// 简单实现
@Service
@ConditionalOnProperty(name = ["auth.idProvider"], havingValue = "sample", matchIfMissing = true)
class SamplePermissionService : PermissionService {
    override fun check(userId: String, resource: String): Boolean {
        return true  // 简单实现，全部放行
    }
}
```

## 其他条件注解

| 注解 | 说明 |
|------|------|
| `@ConditionalOnProperty` | 基于配置属性 |
| `@ConditionalOnBean` | 基于 Bean 存在 |
| `@ConditionalOnMissingBean` | 基于 Bean 不存在 |
| `@ConditionalOnClass` | 基于类存在 |
| `@ConditionalOnExpression` | 基于 SpEL 表达式 |

## 最佳实践

1. **默认实现**：使用 `matchIfMissing = true` 指定默认
2. **清晰命名**：配置项命名要清晰表达含义
3. **文档说明**：在配置模板中说明各选项含义
4. **测试覆盖**：确保各条件分支都有测试

## 相关文件

- `process/biz-process/src/main/kotlin/com/tencent/devops/process/permission/config/`
- `auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/provider/`
