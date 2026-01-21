---
name: conditional-configuration
description: 条件配置指南，涵盖 @Conditional 注解、Profile 配置、特性开关、配置优先级、动态配置加载。当用户实现条件化 Bean 加载、配置多环境、使用特性开关或处理配置优先级时使用。
core_files:
  - "src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/permission/config/"
related_skills:
  - 01-backend-microservice-development
token_estimate: 1200
---

# 条件配置

## Quick Reference

```
配置条件：@ConditionalOnProperty(prefix, name, havingValue, matchIfMissing)
典型场景：多实现切换（RBAC/Sample 权限）、功能开关
```

### 最简示例

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

// 简单实现（默认）
@Service
@ConditionalOnProperty(name = ["auth.idProvider"], havingValue = "sample", matchIfMissing = true)
class SamplePermissionService : PermissionService {
    override fun check(userId: String, resource: String) = true
}
```

```yaml
# application.yml
auth:
  idProvider: rbac  # 或 sample
```

## When to Use

- 多实现切换
- 环境差异化配置
- 功能开关

---

## 其他条件注解

| 注解 | 说明 |
|------|------|
| `@ConditionalOnProperty` | 基于配置属性 |
| `@ConditionalOnBean` | 基于 Bean 存在 |
| `@ConditionalOnMissingBean` | 基于 Bean 不存在 |
| `@ConditionalOnClass` | 基于类存在 |

---

## Checklist

- [ ] 使用 matchIfMissing 指定默认实现
- [ ] 配置项命名清晰
- [ ] 在配置模板中说明各选项
- [ ] 确保各条件分支有测试覆盖
