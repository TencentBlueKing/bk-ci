---
name: 03-unit-testing
description: 单元测试编写指南，涵盖 JUnit5/MockK 使用、测试命名规范、Mock 技巧、测试覆盖率要求、TDD 实践。当用户编写单元测试、Mock 依赖、提高测试覆盖率或进行测试驱动开发时使用。
---

# Skill 03: 单元测试编写

## 概述
BK-CI 后端使用 JUnit 5 + MockK 作为测试框架，遵循 AAA（Arrange-Act-Assert）测试模式。

## 测试框架和工具链

| 组件 | 版本 | 用途 |
|------|------|------|
| JUnit 5 (Jupiter) | - | 主测试框架 |
| MockK | 1.12.2 | Kotlin 原生 Mock 框架 |
| JOOQ MockConnection | - | 数据库测试 |

## 测试文件组织规范

### 命名和路径约定

- 测试文件命名：`*Test.kt`
- 路径镜像源文件：

```
源代码：src/main/kotlin/com/tencent/devops/common/api/util/JsonUtil.kt
测试代码：src/test/kotlin/com/tencent/devops/common/api/util/JsonUtilTest.kt
```

- 测试资源：`src/test/resources/`

## 测试基类

### BkCiAbstractTest

位于 `common-test` 模块，提供：
- MockConnection
- ObjectMapper
- 工具方法

```kotlin
abstract class BkCiAbstractTest {
    protected val dslContext: DSLContext = DSL.using(
        MockConnection(Mock.of(0)),
        SQLDialect.MYSQL
    )
    
    protected val objectMapper: ObjectMapper = JsonUtil.getObjectMapper()
}
```

## 测试方法命名约定

### 模式1：简洁描述性名称（推荐）

```kotlin
@Test
fun mapToTest() { }

@Test
fun getAllVariable() { }
```

### 模式2：Kotlin Backtick 中文描述

```kotlin
@Test
fun `when container fail`() { }

@Test
fun `test verifyClientInformation with invalid grant type`() { }
```

### 模式3：配合 @DisplayName

```kotlin
@Test
@DisplayName("迁移api创建-管理员用户组")
fun test_1() { }
```

## AAA 测试模式

```kotlin
@Test
fun continueWhenFailure() {
    // Arrange（准备数据）
    val nullObject = null
    val additionalOptions = elementAdditionalOptions(
        runCondition = RunCondition.PRE_TASK_FAILED_ONLY
    )
    
    // Act（执行）
    val result = ControlUtils.continueWhenFailure(additionalOptions)
    
    // Assert（验证）
    Assertions.assertFalse(result)
}
```

## Mock 使用规范

### Mock 对象创建

```kotlin
// 基础 Mock
private val authOauth2ClientDetailsDao = mockk<AuthOauth2ClientDetailsDao>()

// Relaxed Mock（自动返回默认值）
private val pipelineAsCodeService: PipelineAsCodeService = mockk(relaxed = true)

// Spy（部分 Mock）
private val self: MigrateV3PolicyService = spyk(
    MigrateV3PolicyService(...),
    recordPrivateCalls = true  // 可访问私有方法
)
```

### Stub 行为定义

```kotlin
// 简单返回值
every { pipelineBuildVarDao.getVars(dslContext, projectId, buildId) } returns mockVars

// 条件应答
every { redisOperation.execute(any<RedisScript<*>>(), any(), any()) } answers {
    val scriptObject = args[0]!!
    if (scriptObject is DefaultRedisScript<*> && scriptObject.resultType == Long::class.java) {
        return@answers 1
    } else {
        throw RuntimeException("redisOperation.execute must mock by self")
    }
}

// Spring Bean Mock
mockkObject(SpringContextUtil)
every { SpringContextUtil.getBean(CommonConfig::class.java) } returns commonConfig
```

### 生命周期钩子

```kotlin
@BeforeEach
fun setup() {
    val commonConfig: CommonConfig = mockk()
    val redisOperation: RedisOperation = mockk()
    every { commonConfig.devopsDefaultLocaleLanguage } returns "zh_CN"
    every { redisOperation.get(any()) } returns "zh_CN"
    mockkObject(SpringContextUtil)
    every { SpringContextUtil.getBean(CommonConfig::class.java) } returns commonConfig
}
```

## 断言和验证模式

### 常用断言

```kotlin
// 基本断言
Assertions.assertEquals(expected, actual)
Assertions.assertTrue(condition)
Assertions.assertFalse(condition)
Assertions.assertNull(value)
Assertions.assertNotNull(value)

// 异常断言
val exception = assertThrows<ErrorCodeException> {
    oauth2ClientService.verifyClientInformation(...)
}
Assertions.assertEquals(AuthMessageCode.INVALID_AUTHORIZATION_TYPE, exception.errorCode)

// 集合断言
Assertions.assertEquals(map.size, 2)
Assertions.assertNotNull(map["key"])
```

### 验证调用

```kotlin
// 验证方法被调用
verify { mockService.doSomething(any()) }

// 验证调用次数
verify(exactly = 1) { mockService.doSomething(any()) }

// 验证未被调用
verify(exactly = 0) { mockService.doSomething(any()) }
```

## 测试数据构建

### Builder 模式（推荐）

```kotlin
// 在 TestBase 中定义 Builder 方法
fun elementAdditionalOptions(
    enable: Boolean = true,
    runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
    customVariables: MutableList<NameAndValue>? = null,
    retryCount: Int = 0
): ElementAdditionalOptions {
    return ElementAdditionalOptions(
        enable = enable,
        runCondition = runCondition,
        customVariables = customVariables,
        retryCount = retryCount
    )
}

// 使用
val options = elementAdditionalOptions(
    enable = false, 
    runCondition = RunCondition.PRE_TASK_FAILED_ONLY
)
```

### 从资源文件加载

```kotlin
val classPathResource = ClassPathResource("v3/group_api_policy_admin.json")
val taskDataResult = JsonUtil.to(
    json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
    type = MigrateTaskDataResult::class.java
)
```

### 共享 Fixture

```kotlin
companion object {
    const val projectId = "devops1"
    const val buildId = "b-12345678901234567890123456789012"
    const val pipelineId = "p-12345678901234567890123456789012"
}
```

## 数据库测试规范

### 使用 JOOQ MockConnection

```kotlin
// 在 BkCiAbstractTest 中配置
val dslContext: DSLContext = DSL.using(
    MockConnection(Mock.of(0)),
    SQLDialect.MYSQL
)

// Mock 查询结果
fun <R : Record> DSLContext.mockResult(t: Table<R>, vararg records: R): Result<R> {
    val result = newResult(t)
    records.forEach { result.add(it) }
    return result
}
```

## 测试组织模式

### 使用 @Nested 组织相关测试

```kotlin
class MigrateV3PolicyServiceTest {
    
    @Nested
    inner class BuildRbacAuthorizationScopeList {
        @Test
        @DisplayName("迁移api创建-管理员用户组")
        fun test_1() { }
        
        @Test
        @DisplayName("迁移api创建-用户自定义用户组")
        fun test_2() { }
    }
}
```

## 测试覆盖要求

- **单元测试**：关键业务逻辑必须有测试覆盖
  - Service 层核心方法
  - 工具类公共方法
  - 复杂算法和条件分支
- **边界测试**：空值、空列表、边界值、异常情况
- **覆盖率工具**：JaCoCo
