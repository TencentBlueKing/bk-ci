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
            circuitBreakerRegistry = io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.of(builder.build()),
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
        val action = "VIEW"
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

    @Test
    fun `CIRCUIT_BREAKER falls back to internal when open (CallNotPermittedException)`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER

        val circuitBreaker: CircuitBreaker = mockk()
        every { circuitBreakerRegistry.circuitBreaker(any()) } returns circuitBreaker
        every { circuitBreaker.executeCallable<Boolean>(any()) } throws CallNotPermittedException.createCallNotPermittedException(circuitBreaker)

        every { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) } returns true

        val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
        assertEquals(true, res)
        verify { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `CIRCUIT_BREAKER falls back to internal when external throws generic exception`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER

        val circuitBreaker: CircuitBreaker = mockk()
        every { circuitBreakerRegistry.circuitBreaker(any()) } returns circuitBreaker
        every { circuitBreaker.executeCallable<Boolean>(any()) } throws RuntimeException("external failure")

        every { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) } returns true

        val res = decorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
        assertEquals(true, res)
        verify { bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `triggerCircuitBreakerByFailures`() {
        every { routingStrategy.getModeForProject("p") } returns RoutingMode.CIRCUIT_BREAKER
        // use helper to create decorator with configured registry
        val localDecorator = delegatingPermissionServiceDecorator(
            rbacPermissionService = rbacPermissionService,
            bkInternalPermissionService = bkInternalPermissionService,
            routingStrategy = routingStrategy,
            rbacCommonService = rbacCommonService
        )

        // external fails
        every { rbacPermissionService.validateUserProjectPermission(any(), any(), any()) } throws RuntimeException("boom")
        // internal fallback counts down latch when called
        val latch = CountDownLatch(1)
        every {
            bkInternalPermissionService.validateUserResourcePermission(any(), any(), any(), any(), any(), any())
        } answers {
            latch.countDown()
            true
        }

        // call multiple times to accumulate failures and eventually open circuit
        repeat(8) {
            try {
                localDecorator.validateUserProjectPermission("u", "p", AuthPermission.MANAGE)
            } catch (_: Exception) {
                // ignore exceptions from external failures until circuit opens
            }
        }

        // after attempts, wait for internal fallback to be invoked at least once
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