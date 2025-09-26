package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.test.BkCiAbstractTest
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DelegatingPermissionServiceDecoratorTest : BkCiAbstractTest() {

    private val rbacPermissionService: RbacPermissionService = mockk()
    private val bkInternalPermissionService: BkInternalPermissionService = mockk()
    private val routingStrategy: PermissionRoutingStrategy = mockk()
    private val rbacCommonService: RbacCommonService = mockk()
    private val meterRegistry: MeterRegistry = mockk()
    private lateinit var decorator: DelegatingPermissionServiceDecorator
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    // Helper to build a decorator with a CircuitBreakerRegistry configured similarly to production.
    // For test reliability we slightly shorten durations and counts so tests run quickly.
    private fun createTestCircuitBreakerRegistry(): CircuitBreakerRegistry {
        val builder = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
        builder.enableAutomaticTransitionFromOpenToHalfOpen()
        builder.writableStackTraceEnabled(false)
        builder.waitDurationInOpenState(Duration.ofMillis(200)) // Shorter duration for tests
        builder.permittedNumberOfCallsInHalfOpenState(3) // Smaller number to speed up
        builder.failureRateThreshold(60.0F) // 60% failure rate threshold
        builder.slowCallRateThreshold(80.0F) // 80% slow call threshold
        builder.slowCallDurationThreshold(Duration.ofMillis(50)) // 50ms slow call duration for tests
        builder.slidingWindowSize(8) // Smaller sliding window size
        builder.minimumNumberOfCalls(4) // Smaller minimum number of calls
        return CircuitBreakerRegistry.of(builder.build())
    }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        circuitBreakerRegistry = createTestCircuitBreakerRegistry()

        // Mock the companion object's threadPoolExecutor to use our testThreadPool
        mockkObject(DelegatingPermissionServiceDecorator.Companion)
        decorator = DelegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            circuitBreakerRegistry = circuitBreakerRegistry,
            rbacCommonService = rbacCommonService,
            meterRegistry = meterRegistry
        )
    }

    @Nested
    @DisplayName("Routing Mode Tests")
    inner class RoutingModeTests {
        @Test
        fun `NORMAL mode calls external`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.NORMAL
            every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } returns true

            val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)

            assertTrue(res)
            verify(exactly = 1) {
                rbacPermissionService.validateUserProjectPermission(
                    "u", "p",
                    AuthPermission.MANAGE
                )
            }
            verify(exactly = 0) {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            }
        }

        @Test
        fun `INTERNAL mode calls internal`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.INTERNAL
            every {
                bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
            } returns true

            val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)

            assertTrue(res)
            verify(exactly = 1) {
                bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
            }
            verify(exactly = 0) { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Tests")
    inner class CircuitBreakerTests {

        @Test
        fun `circuit breaker remains closed under normal load`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER
            every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } returns true
            every {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            } returns false // Should not be called

            repeat(4) { // Calls within minimumNumberOfCalls and well below failure thresholds
                val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
                assertTrue(res)
            }
            verify(exactly = 4) { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) }
            verify(exactly = 0) {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            }
            assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )
        }

        @Test
        fun `triggerCircuitBreakerByFailures opens circuit and calls fallback`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER
            every { decorator.circuitBreakerCounter().increment() } returns Unit
            // Simulate failures for external service
            every {
                rbacPermissionService.validateUserProjectPermission(any(), any(), any())
            } throws RuntimeException("External service failure")

            val fallbackLatch = CountDownLatch(1)
            every {
                bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
            } answers {
                fallbackLatch.countDown()
                true // Fallback returns true
            }

            // Perform enough failures to open the circuit (minimumNumberOfCalls = 4, failureRateThreshold = 60%)
            // 4 calls: 3 failures will be 75%, exceeding 60%
            repeat(3) { // First 3 calls fail, circuit state is still CLOSED but metrics are collected
                try {
                    decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
                } catch (_: Exception) {
                    // Ignore expected exceptions
                }
            }

            // The 4th call (minimumNumberOfCalls met) should trigger the state transition to OPEN
            // and immediately fall back to internal
            val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(res, "Should use fallback and return true") // Fallback returns true

            assertTrue(fallbackLatch.await(1, TimeUnit.SECONDS), "Fallback should be called")
            verify(atLeast = 1) {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            }
            assertEquals(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )

            // Further calls should also directly go to fallback without calling external
            val furtherRes = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(furtherRes)
            verify(exactly = 4) { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) }
        }

        @Test
        fun `triggerCircuitBreakerBySlowCall opens circuit and calls fallback`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER
            every { decorator.circuitBreakerCounter().increment() } returns Unit
            // Simulate slow calls for external service (threshold 50ms, sleep 100ms)
            every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } answers {
                Thread.sleep(100)
                true // External call eventually succeeds
            }

            val fallbackLatch = CountDownLatch(1)
            every {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            } answers {
                fallbackLatch.countDown()
                true // Fallback returns true
            }

            // Perform enough slow calls to open the circuit (minimumNumberOfCalls = 4, slowCallRateThreshold = 80%)
            // 4 calls: 4 slow calls will be 100%, exceeding 80%
            repeat(4) { // First 4 calls are slow, metrics are collected. Circuit will open after these.
                decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            }

            // Circuit should now be OPEN. The next call should immediately fall back.
            val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(res, "Should use fallback and return true")

            assertTrue(fallbackLatch.await(1, TimeUnit.SECONDS), "Fallback should be called")
            verify(atLeast = 1) {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            }
            assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )

            // Further calls should also directly go to fallback without calling external
            val furtherRes = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(furtherRes)
            verify(exactly = 4) { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) }
        }

        @Test
        fun `circuit breaker transitions from OPEN to HALF_OPEN and then to CLOSED`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER

            // 1. OPEN the circuit
            // 最小调用次数 minimumNumberOfCalls(4)
            // 故障率阈值 failureRateThreshold(60.0F)
            // 在 4 次调用中，只要失败次数 >= 4 * 0.6 = 2.4 (即3次失败)，熔断器就会打开。
            // 这里模拟 4 次全部失败，肯定会打开。
            every {
                rbacPermissionService.validateUserProjectPermission(any(), any(), any())
            } throws RuntimeException("Initial failure to open circuit")
            repeat(4) { // 触发 4 次失败调用，满足 minimumNumberOfCalls(4) 和 failureRateThreshold(60.0F)
                try {
                    decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
                } catch (_: Exception) {
                }
            }
            assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )

            // Configure fallback (用于 HALF_OPEN 状态下如果第一次尝试失败，或 CLOSED 状态下的错误处理)
            // 注意：在这个测试中，我们期望 HALF_OPEN 后的调用是成功的，
            // 所以这个 fallback 可能不是直接用来处理 HALF_OPEN 的第一次尝试失败，
            // 而是模拟当 Circuit Breaker 打开时，内部服务作为备用方案。
            every {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            } returns true // 确保 fallback 成功

            // 2. Wait for waitDurationInOpenState (200ms)
            Thread.sleep(250) // Wait a bit longer than 200ms，确保等待时间已过

            // 3. First call after waitDurationInOpenState should transition to HALF_OPEN and try external
            // 模拟外部服务现在恢复正常，第一次调用成功
            every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } returns true
            val resHalfOpen = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(resHalfOpen)
            // 第一次成功调用后，状态应为 HALF_OPEN
            assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )

            // 4. Perform permittedNumberOfCallsInHalfOpenState (3 calls) successful calls
            // 根据配置 permittedNumberOfCallsInHalfOpenState(3)，需要再进行 3 次成功调用才能关闭。
            // 因为上面已经进行了一次成功调用并进入 HALF_OPEN，所以这里需要再额外进行 2 次成功调用。
            // 总共在 HALF_OPEN 状态下会有 1 (进入 HALF_OPEN) + 2 (这里) = 3 次成功调用。
            repeat(2) { // 额外进行 2 次成功调用
                decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            }
            // 现在应该是第三次成功调用，它应该会使 Circuit Breaker 切换到 CLOSED
            val resClosed = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertTrue(resClosed)

            // After 3 successful calls in HALF_OPEN (1 initial + 2 repeat), circuit should transition to CLOSED
            assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )
        }

        @Test
        fun `circuit breaker transitions from OPEN to HALF_OPEN and then back to OPEN on failure`() {
            every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER

            // 1. OPEN the circuit
            every {
                rbacPermissionService.validateUserProjectPermission(any(), any(), any())
            } throws RuntimeException("Initial failure to open circuit")
            repeat(4) {
                try {
                    decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
                } catch (_: Exception) {
                }
            }
            assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )

            // Configure fallback
            every {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            } returns true

            // 2. Wait for waitDurationInOpenState (200ms)
            Thread.sleep(250)

            assertEquals(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )
            //
            repeat(2) {
                try {
                    decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
                } catch (_: Exception) {
                }
            }
            every {
                rbacPermissionService.validateUserProjectPermission(any(), any(), any())
            } returns true

            decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            assertEquals(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("AUTH_CIRCUIT_BREAKER").state
            )
        }
    }

    @Nested
    @DisplayName("PermissionService Method Tests")
    inner class PermissionServiceMethodTests {
        private val USER_ID = "testUser"
        private val PROJECT_CODE = "testProject"
        private val RESOURCE_CODE = "testResource"
        private val RESOURCE_TYPE = AuthResourceType.PIPELINE_DEFAULT.value
        private val ACTION_NAME = "pipeline_view" // Example for resolveInternalAction
        private val AUTH_RESOURCE_INSTANCE = AuthResourceInstance(RESOURCE_TYPE, RESOURCE_CODE)

        @BeforeEach
        fun setupCommonMocks() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every { routingStrategy.getDefaultMode() } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.buildAuthResourceInstance(any(), any(), any(), any())
            } returns AUTH_RESOURCE_INSTANCE
            every { rbacCommonService.getActionInfo(any()) } returns mockk {
                every { relatedResourceType } returns AuthResourceType.PROJECT.value
            }
        }

        @Test
        fun `validateUserActionPermission calls rbacPermissionService`() {
            every { rbacPermissionService.validateUserActionPermission(USER_ID, "action") } returns true
            val result = decorator.validateUserActionPermission(USER_ID, "action")
            assertTrue(result)
            verify(exactly = 1) { rbacPermissionService.validateUserActionPermission(USER_ID, "action") }
        }

        @Test
        fun `validateUserProjectPermission routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.VIEW)
            } returns true

            val result = decorator.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.VIEW)
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.VIEW)
            }

            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.INTERNAL
            every {
                bkInternalPermissionService.validateUserResourcePermission(
                    any(), any(), any(),
                    any(), any(), any()
                )
            } returns true

            val internalResult = decorator.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.VIEW)
            assertTrue(internalResult)
            verify(exactly = 1) {
                bkInternalPermissionService.validateUserResourcePermission(
                    USER_ID, PROJECT_CODE, ResourceTypeId.PROJECT, PROJECT_CODE,
                    RbacAuthUtils.buildAction(
                        AuthPermission.VIEW,
                        AuthResourceType.PROJECT
                    ), false
                )
            }
        }

        @Test
        fun `checkProjectManager calls validateUserProjectPermission with MANAGE`() {
            every {
                rbacPermissionService.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.MANAGE)
            } returns true
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL

            val result = decorator.checkProjectManager(USER_ID, PROJECT_CODE)
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserProjectPermission(USER_ID, PROJECT_CODE, AuthPermission.MANAGE)
            }
        }

        @Test
        fun `validateUserResourcePermission with project resource type routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every { rbacCommonService.getActionInfo(ACTION_NAME) } returns mockk {
                every {
                    relatedResourceType
                } returns AuthResourceType.PROJECT.value
            }
            every {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, any()
                )
            } returns true

            val result = decorator.validateUserResourcePermission(USER_ID, ACTION_NAME, PROJECT_CODE, null)
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME, PROJECT_CODE, AUTH_RESOURCE_INSTANCE
                )
            }
        }

        @Test
        fun `validateUserResourcePermission with non-project resource type routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every { rbacCommonService.getActionInfo(ACTION_NAME) } returns mockk {
                every {
                    relatedResourceType
                } returns AuthResourceType.PIPELINE_DEFAULT.value
            }
            every {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, any()
                )
            } returns true

            val result = decorator.validateUserResourcePermission(
                USER_ID, ACTION_NAME, PROJECT_CODE,
                AuthResourceType.PIPELINE_DEFAULT.value
            )
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, any()
                )
            }
        }

        @Test
        fun `validateUserResourcePermissionByRelation routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, any()
                )
            } returns true

            val result = decorator.validateUserResourcePermissionByRelation(
                USER_ID, ACTION_NAME, PROJECT_CODE,
                RESOURCE_CODE, RESOURCE_TYPE, null
            )
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, AUTH_RESOURCE_INSTANCE
                )
            }
        }

        @Test
        fun `validateUserResourcePermissionByInstance routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, AUTH_RESOURCE_INSTANCE
                )
            } returns true

            val result = decorator.validateUserResourcePermissionByInstance(
                USER_ID, ACTION_NAME,
                PROJECT_CODE, AUTH_RESOURCE_INSTANCE
            )
            assertTrue(result)
            verify(exactly = 1) {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    USER_ID, ACTION_NAME,
                    PROJECT_CODE, AUTH_RESOURCE_INSTANCE
                )
            }
        }

        @Test
        fun `batchValidateUserResourcePermission routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.batchValidateUserResourcePermissionByInstance(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resource = AUTH_RESOURCE_INSTANCE
                )
            } returns mapOf("action1" to true)

            val result = decorator.batchValidateUserResourcePermission(
                USER_ID, listOf("action1"), PROJECT_CODE,
                RESOURCE_CODE, RESOURCE_TYPE
            )
            assertEquals(mapOf("action1" to true), result)
            verify(exactly = 1) {
                rbacPermissionService.batchValidateUserResourcePermissionByInstance(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resource = AUTH_RESOURCE_INSTANCE
                )
            }
        }

        @Test
        fun `batchValidateUserResourcePermissionByInstance routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.batchValidateUserResourcePermissionByInstance(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resource = AUTH_RESOURCE_INSTANCE
                )
            } returns mapOf("action1" to true)

            val result = decorator.batchValidateUserResourcePermissionByInstance(
                USER_ID, listOf("action1"),
                PROJECT_CODE, AUTH_RESOURCE_INSTANCE
            )
            assertEquals(mapOf("action1" to true), result)
            verify(exactly = 1) {
                rbacPermissionService.batchValidateUserResourcePermissionByInstance(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resource = AUTH_RESOURCE_INSTANCE
                )
            }
        }

        @Test
        fun `getUserResourceByAction routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.getUserResourceByAction(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    action = ACTION_NAME,
                    resourceType = RESOURCE_TYPE
                )
            } returns listOf(RESOURCE_CODE)

            val result = decorator.getUserResourceByAction(
                userId = USER_ID,
                action = ACTION_NAME,
                projectCode = PROJECT_CODE,
                resourceType = RESOURCE_TYPE
            )
            assertEquals(listOf(RESOURCE_CODE), result)
            verify(exactly = 1) {
                rbacPermissionService.getUserResourceByAction(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    action = ACTION_NAME,
                    resourceType = RESOURCE_TYPE
                )
            }
        }

        @Test
        fun `getUserResourcesByActions routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.getUserResourcesByActions(
                    USER_ID, listOf(ACTION_NAME),
                    PROJECT_CODE, RESOURCE_TYPE
                )
            } returns mapOf(AuthPermission.VIEW to listOf(RESOURCE_CODE))

            val result = decorator.getUserResourcesByActions(
                USER_ID, listOf(ACTION_NAME),
                PROJECT_CODE, RESOURCE_TYPE
            )
            assertEquals(mapOf(AuthPermission.VIEW to listOf(RESOURCE_CODE)), result)
            verify(exactly = 1) {
                rbacPermissionService.getUserResourcesByActions(
                    USER_ID, listOf(ACTION_NAME),
                    PROJECT_CODE, RESOURCE_TYPE
                )
            }
        }

        @Test
        fun `filterUserResourcesByActions routes correctly`() {
            every { routingStrategy.getModeForProject(PROJECT_CODE) } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.filterUserResourcesByActions(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resourceType = RESOURCE_TYPE,
                    resources = any()
                )
            } returns mapOf(AuthPermission.VIEW to listOf(RESOURCE_CODE))

            val result = decorator.filterUserResourcesByActions(
                USER_ID, listOf(AuthPermission.VIEW.value),
                PROJECT_CODE, RESOURCE_TYPE, listOf(AUTH_RESOURCE_INSTANCE)
            )
            assertEquals(mapOf(AuthPermission.VIEW to listOf(RESOURCE_CODE)), result)
            verify(exactly = 1) {
                rbacPermissionService.filterUserResourcesByActions(
                    userId = USER_ID,
                    projectCode = PROJECT_CODE,
                    actions = any(),
                    resourceType = RESOURCE_TYPE,
                    resources = any()
                )
            }
        }

        @Test
        fun `getUserResourceAndParentByPermission does not use routing`() {
            every {
                rbacPermissionService.getUserResourceAndParentByPermission(
                    userId = USER_ID, projectCode = PROJECT_CODE,
                    action = ACTION_NAME, resourceType = RESOURCE_TYPE
                )
            } returns mapOf("parent" to listOf("p1"))

            val result = decorator.getUserResourceAndParentByPermission(
                userId = USER_ID, action = ACTION_NAME,
                projectCode = PROJECT_CODE, resourceType = RESOURCE_TYPE
            )
            assertEquals(mapOf("parent" to listOf("p1")), result)
            verify(exactly = 1) {
                rbacPermissionService.getUserResourceAndParentByPermission(
                    userId = USER_ID, projectCode = PROJECT_CODE,
                    action = ACTION_NAME, resourceType = RESOURCE_TYPE
                )
            }
            verify(exactly = 0) { routingStrategy.getModeForProject(any()) }
        }

        @Test
        fun `getUserProjectsByPermission routes correctly`() {
            val projectList = listOf("projectA", "projectB")
            every { routingStrategy.getDefaultMode() } returns RoutingMode.NORMAL
            every {
                rbacPermissionService.getUserProjectsByPermission(USER_ID, ACTION_NAME, null)
            } returns projectList

            val result = decorator.getUserProjectsByPermission(USER_ID, ACTION_NAME, null)
            assertEquals(projectList, result)
            verify(exactly = 1) {
                rbacPermissionService.getUserProjectsByPermission(USER_ID, ACTION_NAME, null)
            }

            every { routingStrategy.getDefaultMode() } returns RoutingMode.INTERNAL
            every { bkInternalPermissionService.getUserProjectsByAction(USER_ID, any()) } returns projectList

            val internalResult = decorator.getUserProjectsByPermission(USER_ID, ACTION_NAME, null)
            assertEquals(projectList, internalResult)
            verify(exactly = 1) {
                bkInternalPermissionService.getUserProjectsByAction(
                    USER_ID, ACTION_NAME
                )
            }
        }
    }

    @Nested
    @DisplayName("Internal Action Resolution Tests")
    inner class InternalActionResolutionTests {
        @Test
        fun `resolveInternalAction correctly builds action from permission and resource type`() {
            val action = "view"
            val resourceType = AuthResourceType.PROJECT.value
            val expected = RbacAuthUtils.buildAction(AuthPermission.VIEW, AuthResourceType.PROJECT)
            val result = decorator.resolveInternalAction(action, resourceType)
            assertEquals(expected, result)
        }

        @Test
        fun `resolveInternalAction returns action as is if it contains underscore`() {
            val action = "pipeline_view"
            val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
            val result = decorator.resolveInternalAction(action, resourceType)
            assertEquals(action, result)
        }
    }
}
