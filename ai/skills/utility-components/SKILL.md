---
name: utility-components
description: BK-CI 工具组件指南：使用 JwtManager 生成验证 JWT Token、ExpressionParser 解析流水线条件表达式、ThreadPoolUtil/LoopUtil 批量并发与循环处理、责任链模式实现拦截器链。当用户需要操作这些具体工具类时使用。
core_files:
  - "src/backend/ci/core/common/common-security/src/main/kotlin/com/tencent/devops/common/security/jwt/"
  - "src/backend/ci/core/common/common-expression/"
  - "src/backend/ci/core/common/common-util/src/main/kotlin/com/tencent/devops/common/util/"
related_skills:
  - common-technical-practices
  - design-patterns
token_estimate: 4500
---

# 工具组件指南

本 Skill 涵盖 BK-CI 中 **4 类工具组件** 的具体使用，详细实现见各 reference 文件。

> **与 `common-technical-practices` 的区别**: 本 Skill 关注具体工具类（JwtManager、ExpressionParser、ThreadPoolUtil、LoopUtil），`common-technical-practices` 关注框架级横切关注点（AOP 切面、分布式锁、重试机制、性能监控）。

| 主题 | 核心类 | 文档 |
|------|--------|------|
| **JWT 安全认证** | `JwtManager` | [1-jwt-security.md](./reference/1-jwt-security.md) |
| **表达式解析器** | `ExpressionParser` | [2-expression-parser.md](./reference/2-expression-parser.md) |
| **线程池循环工具** | `ThreadPoolUtil`, `LoopUtil` | [3-thread-pool-loop-util.md](./reference/3-thread-pool-loop-util.md) |
| **责任链模式** | 拦截器链 | [4-chain-responsibility.md](./reference/4-chain-responsibility.md) |

---

## 一、JWT 安全认证

详见 [reference/1-jwt-security.md](./reference/1-jwt-security.md)

使用 RSA 公私钥对实现微服务间 JWT Token 认证：

```kotlin
import com.tencent.devops.common.security.jwt.JwtManager

// JwtManager 通过 Spring 注入，使用 RSA 密钥对
// 构造: JwtManager(privateKeyString, publicKeyString, enable)

// 获取当前服务的 JWT Token（自动刷新）
val token: String? = jwtManager.getToken()

// 验证其他服务传入的 Token
val isValid: Boolean = jwtManager.verifyJwt(token)
```

---

## 二、表达式解析器

详见 [reference/2-expression-parser.md](./reference/2-expression-parser.md)

解析流水线条件表达式，支持变量替换、布尔求值和自定义函数：

```kotlin
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.StringContextData

// 构建上下文
val nameValue = listOf(NamedValueInfo("variables", ContextValueNode()))
val context = DictionaryContextData().apply {
    add("variables", DictionaryContextData().apply {
        add("status", StringContextData("success"))
    })
}

// 解析表达式求值
val tree = ExpressionParser.createTree(
    expression = "eq(variables.status, 'success')",
    trace = null,
    namedValues = nameValue,
    functions = null
)
val result = tree!!.evaluate(null, context, null)
// result.value = true
```

---

## 三、线程池与循环工具

详见 [reference/3-thread-pool-loop-util.md](./reference/3-thread-pool-loop-util.md)

### ThreadPoolUtil — 创建受控线程池

```kotlin
import com.tencent.devops.common.util.ThreadPoolUtil
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

// 创建线程池（建议定义为成员变量，避免每次创建）
val executor = ThreadPoolUtil.getThreadPoolExecutor(
    corePoolSize = 4,
    maximumPoolSize = 8,
    keepAliveTime = 60,
    unit = TimeUnit.SECONDS,
    threadNamePrefix = "batch-process-",
    queue = LinkedBlockingQueue(100)
)
try {
    executor.submit { processTask() }
} finally {
    executor.shutdown() // 局部变量必须在 finally 中关闭
}
```

### LoopUtil — 防死循环数据库批量加载

```kotlin
import com.tencent.devops.common.util.LoopUtil
import com.tencent.devops.common.util.LoopUtil.LoopVo

// 按自增 ID 分批加载数据库记录，内置循环次数和耗时保护
val vo = LoopVo(id = 0L, data = mutableListOf<Record>())
val metrics = LoopUtil.doLoop(vo) { loopVo ->
    val records = dao.listByIdGreaterThan(loopVo.id, limit = 500)
    if (records.isEmpty()) {
        loopVo.finish = true  // 标记完成，退出循环
    } else {
        loopVo.data.addAll(records)
        loopVo.id = records.last().id  // 更新游标
    }
}
// metrics.loopCount = 实际循环次数, metrics.totalTime = 总耗时ms
```

---

## 四、责任链模式

详见 [reference/4-chain-responsibility.md](./reference/4-chain-responsibility.md)

实现拦截器链处理请求，支持流水线插件链和请求处理链：

```kotlin
// 定义处理器
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        // 认证逻辑
        return chain.proceed(request)  // 传递给下一个拦截器
    }
}

// 组装拦截器链
val chain = InterceptorChain(listOf(
    AuthInterceptor(),
    ValidationInterceptor(),
    LoggingInterceptor()
))
val response = chain.proceed(request)
```

---

## 相关 Skill

- [common-technical-practices](../common-technical-practices/SKILL.md) - 框架级横切关注点（AOP、分布式锁、重试、监控）
- [design-patterns](../design-patterns/SKILL.md) - 设计模式指南
- [backend-microservice-development](../backend-microservice-development/SKILL.md) - 后端微服务开发
