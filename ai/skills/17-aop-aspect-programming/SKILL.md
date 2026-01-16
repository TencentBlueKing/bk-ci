---
name: 17-aop-aspect-programming
description: AOP 切面编程指南，涵盖切面定义、切点表达式、通知类型（Before/After/Around）、注解驱动 AOP、性能监控切面。当用户实现切面逻辑、编写拦截器、添加日志切面或实现权限切面时使用。
core_files:
  - "src/backend/ci/core/common/common-web/src/main/kotlin/com/tencent/devops/common/web/aop/"
related_skills:
  - 27-design-patterns
token_estimate: 1500
---

# AOP 切面编程

## Quick Reference

```
切面定义：@Aspect @Component
切点：@Pointcut("@annotation(xxx)") 或 @Pointcut("execution(...)")
通知：@Before | @After | @AfterReturning | @AfterThrowing | @Around
```

### 最简示例

```kotlin
@Aspect
@Component
class BkApiAspect {
    
    @Pointcut("@annotation(com.tencent.devops.common.web.annotation.BkApiPermission)")
    fun permissionPointcut() {}
    
    @Around("permissionPointcut()")
    fun checkPermission(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        try {
            // 前置：权限检查
            val userId = extractUserId(joinPoint)
            validatePermission(userId)
            
            // 执行目标方法
            val result = joinPoint.proceed()
            
            // 后置：记录耗时
            logger.info("耗时: ${System.currentTimeMillis() - startTime}ms")
            return result
        } catch (e: Exception) {
            logger.error("执行失败", e)
            throw e
        }
    }
}
```

## When to Use

- 横切关注点（日志、权限、监控）
- 方法拦截
- 性能统计

---

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
```

---

## Checklist

- [ ] 每个切面单一职责
- [ ] 避免在切面中执行耗时操作
- [ ] 正确处理和传播异常
- [ ] 使用 @Order 控制执行顺序
