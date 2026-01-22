---
name: unit-testing
description: 单元测试编写指南，涵盖 JUnit5/MockK 使用、测试命名规范、Mock 技巧、测试覆盖率要求、TDD 实践。当用户编写单元测试、Mock 依赖、提高测试覆盖率或进行测试驱动开发时使用。
core_files:
  - "src/backend/ci/core/common/common-test/"
  - "src/test/kotlin/"
related_skills:
  - 01-backend-microservice-development
token_estimate: 2800
---

# 单元测试编写

## Quick Reference

```
框架：JUnit 5 (Jupiter) + MockK 1.12.2
测试基类：BkCiAbstractTest（提供 dslContext、objectMapper）
文件命名：*Test.kt
测试模式：AAA（Arrange-Act-Assert）
```

### 最简示例

```kotlin
class PipelineServiceTest : BkCiAbstractTest() {
    private val pipelineDao = mockk<PipelineDao>()
    private val service = PipelineService(pipelineDao)

    @Test
    fun `should return pipeline when exists`() {
        // Arrange
        every { pipelineDao.get(any(), any()) } returns mockPipeline
        
        // Act
        val result = service.getPipeline(PROJECT_ID, PIPELINE_ID)
        
        // Assert
        Assertions.assertNotNull(result)
        verify { pipelineDao.get(PROJECT_ID, PIPELINE_ID) }
    }
    
    companion object {
        const val PROJECT_ID = "test-project"
        const val PIPELINE_ID = "p-12345678901234567890123456789012"
    }
}
```

## When to Use

- 编写 Service/DAO 层单元测试
- Mock 外部依赖
- 验证业务逻辑正确性
- 进行 TDD 开发

## When NOT to Use

- 集成测试 → 需要启动完整服务
- E2E 测试 → 需要部署完整环境

---

## 测试基类

```kotlin
abstract class BkCiAbstractTest {
    protected val dslContext: DSLContext = DSL.using(
        MockConnection(Mock.of(0)),
        SQLDialect.MYSQL
    )
    protected val objectMapper: ObjectMapper = JsonUtil.getObjectMapper()
}
```

## Mock 创建方式

```kotlin
// 基础 Mock
private val dao = mockk<PipelineDao>()

// Relaxed Mock（自动返回默认值）
private val service = mockk<PipelineService>(relaxed = true)

// Spy（部分 Mock）
private val self = spyk(MyService(), recordPrivateCalls = true)

// Spring Bean Mock
mockkObject(SpringContextUtil)
every { SpringContextUtil.getBean(CommonConfig::class.java) } returns config
```

## Stub 行为定义

```kotlin
// 简单返回
every { dao.get(any(), any()) } returns mockData

// 条件应答
every { redis.execute(any<RedisScript<*>>(), any(), any()) } answers {
    val script = args[0] as DefaultRedisScript<*>
    if (script.resultType == Long::class.java) 1L else throw RuntimeException()
}

// 抛出异常
every { service.doSomething() } throws ErrorCodeException(...)
```

## 断言与验证

```kotlin
// 基本断言
Assertions.assertEquals(expected, actual)
Assertions.assertTrue(condition)
Assertions.assertNull(value)

// 异常断言
val ex = assertThrows<ErrorCodeException> { service.doSomething() }
Assertions.assertEquals("2100013", ex.errorCode)

// 验证调用
verify { dao.get(any(), any()) }
verify(exactly = 1) { service.save(any()) }
verify(exactly = 0) { service.delete(any()) }
```

## 测试组织

```kotlin
class MyServiceTest {
    @Nested
    inner class GetPipelineTests {
        @Test
        @DisplayName("流水线存在时返回数据")
        fun `returns pipeline when exists`() { }
        
        @Test
        @DisplayName("流水线不存在时抛出异常")
        fun `throws exception when not found`() { }
    }
}
```

## 测试数据构建

```kotlin
// Builder 模式
fun buildOptions(
    enable: Boolean = true,
    runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS
) = ElementAdditionalOptions(enable = enable, runCondition = runCondition)

// 从资源文件加载
val resource = ClassPathResource("test-data/pipeline.json")
val data = JsonUtil.to(resource.inputStream, PipelineInfo::class.java)
```

---

## Checklist

编写测试前确认：
- [ ] 继承 `BkCiAbstractTest` 基类
- [ ] 使用 AAA 模式组织测试代码
- [ ] Mock 所有外部依赖
- [ ] 覆盖正常和异常场景
- [ ] 测试方法名清晰描述测试意图
