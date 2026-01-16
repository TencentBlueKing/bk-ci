---
name: 11-service-communication
description: 服务间调用规范，涵盖 Feign 客户端、服务发现、负载均衡、熔断降级、超时配置、链路追踪。当用户进行微服务间调用、配置 Feign 客户端、实现服务降级或处理跨服务通信时使用。
core_files:
  - "src/backend/ci/core/common/common-client/src/main/kotlin/com/tencent/devops/common/client/Client.kt"
related_skills:
  - 01-backend-microservice-development
  - 13-retry-mechanism
token_estimate: 4500
---

# 服务间调用

## Quick Reference

```
⚠️ 核心规则：跨模块必须使用 Client 调用 Service*Resource，禁止直接依赖其他模块 Service/DAO

调用方式：client.get(ServiceXxxResource::class).method()
接口位置：api-{模块}/ServiceXxxResource.kt
返回格式：Result<T>
```

### 最简示例

```kotlin
@Service
class MyService(private val client: Client) {
    
    // ✅ 正确：ticket 调用 process
    fun getBuildInfo(projectId: String, buildId: String): BuildInfo? {
        return client.get(ServiceBuildResource::class)
            .serviceBasic(projectId, buildId)
            .data
    }
    
    // ✅ 正确：store 调用 project
    fun getProjectName(projectId: String): String? {
        return client.get(ServiceProjectResource::class)
            .get(projectId)
            .data?.projectName
    }
}

// ❌ 错误：直接依赖其他模块
class BadService(
    private val buildService: BuildService,  // ❌ process 模块的 Service
    private val projectDao: ProjectDao       // ❌ project 模块的 DAO
)
```

## When to Use

- 不同微服务模块间交互
- 调用其他模块的 API
- 需要服务发现和负载均衡

## When NOT to Use

- 同模块内部调用 → 直接依赖注入
- 异步通知 → 使用 `08-event-driven-architecture`

---

## 判断是否需要跨模块调用

| 场景 | 是否跨模块 | 调用方式 |
|------|-----------|----------|
| ticket → process | ✅ 是 | 必须用 Client |
| store → project | ✅ 是 | 必须用 Client |
| process → auth | ✅ 是 | 必须用 Client |
| ticket 内 Service → DAO | ❌ 否 | 直接注入 |

**简单规则**：调用的类在 `core/{另一个模块}/` → 必须用 Client

## 常见跨模块调用场景

| 调用方 | 被调用模块 | 典型场景 | Resource |
|--------|-----------|----------|----------|
| ticket | process | 获取构建信息 | `ServiceBuildResource` |
| ticket | auth | 权限校验 | `ServicePermissionAuthResource` |
| store | project | 获取项目信息 | `ServiceProjectResource` |
| process | repository | 检查代码库 | `ServiceRepositoryResource` |

## 异常处理

```kotlin
fun getProjectInfo(projectId: String): ProjectVO? {
    return try {
        client.get(ServiceProjectResource::class).get(projectId).data
    } catch (e: RemoteServiceException) {
        logger.warn("获取项目失败: ${e.errorMessage}")
        null
    } catch (e: ClientException) {
        logger.error("服务调用异常", e)
        throw e
    }
}
```

## 带重试调用

```kotlin
val result = RetryUtils.clientRetry(retryTime = 3, retryPeriodMills = 500) {
    client.get(ServiceProjectResource::class).get(projectId)
}
```

## 批量调用优化

```kotlin
// ❌ N+1 问题
projectIds.map { client.get(ServiceProjectResource::class).get(it).data?.projectName }

// ✅ 批量调用
client.get(ServiceProjectResource::class)
    .getNameByCode(projectIds.joinToString(","))
    .data ?: emptyMap()
```

---

## Checklist

跨模块调用前确认：
- [ ] 确认是否真的需要跨模块调用
- [ ] 目标模块是否有 Service*Resource 接口
- [ ] 使用 Client 组件，不直接依赖其他模块
- [ ] 添加异常处理
- [ ] 考虑重试和降级
- [ ] 避免 N+1 调用，使用批量接口
