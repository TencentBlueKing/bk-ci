---
name: 17-aop-aspect-programming
description: AOP 切面编程指南，涵盖切面定义、切点表达式、通知类型（Before/After/Around）、注解驱动 AOP、性能监控切面。当用户实现切面逻辑、编写拦截器、添加日志切面或实现权限切面时使用。
---

# AOP 切面编程

AOP 切面编程指南.

## 触发条件

当用户需要实现横切关注点、日志记录、权限检查、性能监控时，使用此 Skill。

## 切面定义

```kotlin
@Aspect
@Component
class BkApiAspect(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BkApiAspect::class.java)
    }
    
    // 切点定义
    @Pointcut("@annotation(com.tencent.devops.common.web.annotation.BkApiPermission)")
    fun permissionPointcut() {}
    
    // 环绕通知
    @Around("permissionPointcut()")
    fun checkPermission(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        
        try {
            // 前置处理
            val userId = extractUserId(joinPoint)
            validatePermission(userId)
            
            // 执行目标方法
            val result = joinPoint.proceed()
            
            // 后置处理
            logSuccess(joinPoint, System.currentTimeMillis() - startTime)
            
            return result
        } catch (e: Exception) {
            logError(joinPoint, e)
            throw e
        }
    }
}
```

## 通知类型

| 类型 | 注解 | 说明 |
|------|------|------|
| 前置 | `@Before` | 方法执行前 |
| 后置 | `@After` | 方法执行后（无论成功失败） |
| 返回 | `@AfterReturning` | 方法成功返回后 |
| 异常 | `@AfterThrowing` | 方法抛出异常后 |
| 环绕 | `@Around` | 完全控制方法执行 |

## 切点表达式

```kotlin
// 注解匹配
@Pointcut("@annotation(com.xxx.MyAnnotation)")

// 方法匹配
@Pointcut("execution(* com.tencent.devops.*.service.*.*(..))")

// 类匹配
@Pointcut("within(com.tencent.devops.*.service.*)")

// 组合
@Pointcut("permissionPointcut() && execution(* create*(..))")
```

## 使用示例

### 触发器事件切面

```kotlin
@Aspect
@Component
class PipelineTriggerEventAspect {
    
    @Around("@annotation(triggerEvent)")
    fun recordTriggerEvent(
        joinPoint: ProceedingJoinPoint,
        triggerEvent: TriggerEvent
    ): Any? {
        val startTime = System.currentTimeMillis()
        val result = joinPoint.proceed()
        
        // 记录触发事件
        saveTriggerEvent(
            eventType = triggerEvent.type,
            duration = System.currentTimeMillis() - startTime
        )
        
        return result
    }
}
```

## 最佳实践

1. **单一职责**：每个切面只处理一个关注点
2. **性能考虑**：避免在切面中执行耗时操作
3. **异常传播**：正确处理和传播异常
4. **顺序控制**：使用 `@Order` 控制切面执行顺序

## 相关文件

- `common-web/src/main/kotlin/com/tencent/devops/common/web/aop/`
- `process/biz-process/src/main/kotlin/com/tencent/devops/process/trigger/`
