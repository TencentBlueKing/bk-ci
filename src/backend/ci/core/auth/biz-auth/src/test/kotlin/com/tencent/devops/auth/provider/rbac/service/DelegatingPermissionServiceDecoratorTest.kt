/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 */

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.test.BkCiAbstractTest
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DelegatingPermissionServiceDecoratorTest : BkCiAbstractTest() {

    private val rbacPermissionService: RbacPermissionService = mockk()
    private val bkInternalPermissionService: BkInternalPermissionService = mockk()
    private val routingStrategy: PermissionRoutingStrategy = mockk()
    private val circuitBreakerRegistry: CircuitBreakerRegistry = mockk()
    private val rbacCommonService: RbacCommonService = mockk()

    private lateinit var decorator: DelegatingPermissionServiceDecorator

    // Helper to build a decorator with a CircuitBreakerRegistry configured similarly to production.
    // For test reliability we slightly shorten durations and counts so tests run quickly.
    private fun delegatingPermissionServiceDecorator(
        rbacPermissionService: RbacPermissionService,
        bkInternalPermissionService: BkInternalPermissionService,
        routingStrategy: PermissionRoutingStrategy,
        rbacCommonService: RbacCommonService
    ): DelegatingPermissionServiceDecorator {
        val builder = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
        builder.enableAutomaticTransitionFromOpenToHalfOpen()
        builder.writableStackTraceEnabled(false)
        // production: Duration.ofSeconds(5) — test uses shorter duration
        builder.waitDurationInOpenState(Duration.ofMillis(200))
        // production: 10 — test uses smaller number to speed up
        builder.permittedNumberOfCallsInHalfOpenState(2)
        // production: 60% failure rate threshold
        builder.failureRateThreshold(60.0F)
        // production: 80% slow call threshold
        builder.slowCallRateThreshold(80.0F)
        // production: 500ms slow call duration threshold — test lowers it
        builder.slowCallDurationThreshold(Duration.ofMillis(50))
        // production sliding window size 50; test reduce
        builder.slidingWindowSize(8)
        // production minimumNumberOfCalls 20; test reduce
        builder.minimumNumberOfCalls(4)
        return DelegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            circuitBreakerRegistry = CircuitBreakerRegistry.of(builder.build()),
            rbacCommonService = rbacCommonService
        )
    }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        decorator = delegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            rbacCommonService = rbacCommonService
        )
    }

    @Test
    fun `executeWithRouting NORMAL calls external`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.NORMAL
        every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } returns true

        val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)

        assertEquals(true, res)
        verify { rbacPermissionService.validateUserProjectPermission("u", "p", AuthPermission.MANAGE) }
        verify(exactly = 0) { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `executeWithRouting INTERNAL calls internal`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.INTERNAL
        every { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) } returns true

        val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)

        assertEquals(true, res)
        verify(atLeast = 1) { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `resolveInternalAction returns built action when no underscore`() {
        val action = "view"
        val result = decorator.resolveInternalAction(action, AuthResourceType.PROJECT.value)
        assert(result.contains("_") || result.isNotBlank())
    }

    @Test
    fun `VALIDATION mode calls external and triggers internal comparison async`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.VALIDATION
        every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } returns true

        val latch = CountDownLatch(1)
        every {
            bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
        } answers {
            latch.countDown()
            true
        }

        val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
        assertEquals(true, res)
        val called = latch.await(2, TimeUnit.SECONDS)
        assertEquals(true, called)
    }

    /**
     * 测试通过连续失败触发熔断器机制
     *
     * 该测试验证当外部权限服务连续失败时，熔断器能够正确打开并触发内部回退机制。
     * 测试使用模拟对象来控制外部服务的行为，并验证熔断器在达到失败阈值后
     * 是否正确切换到内部权限服务。
     */
    @Test
    fun `triggerCircuitBreakerByFailures`() {
        // 配置路由策略返回熔断器模式
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER

        // 使用辅助函数创建配置了注册表的装饰器
        val localDecorator = delegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            rbacCommonService = rbacCommonService
        )

        // 配置外部权限服务验证失败，模拟服务不可用
        every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } throws RuntimeException("boom")

        // 配置内部回退服务，当被调用时减少计数器并返回true
        val latch = CountDownLatch(1)
        every {
            bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
        } answers {
            latch.countDown()
            true
        }

        // 重复调用8次以累积失败次数并最终打开熔断器
        repeat(8) {
            try {
                localDecorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            } catch (_: Exception) {
                // 忽略外部失败引发的异常，直到熔断器打开
            }
        }

        // 等待内部回退服务被调用至少一次，超时时间为2秒
        val called = latch.await(2, TimeUnit.SECONDS)
        assertEquals(true, called)
    }

    @Test
    fun `triggerCircuitBreakerBySlowCall`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER
        val localDecorator = delegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            rbacCommonService = rbacCommonService
        )

        // external call sleeps to simulate slow call, then returns true
        every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } answers {
            Thread.sleep(100)
            true
        }
        val latch = CountDownLatch(1)
        every {
            bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
        } answers {
            latch.countDown()
            true
        }

        // call multiple times to accumulate slow call rate and open circuit
        repeat(8) {
            try {
                localDecorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            } catch (_: Exception) {
                // ignore until circuit opens
            }
        }

        val called = latch.await(2, TimeUnit.SECONDS)
        assertEquals(true, called)
    }
}
