/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.log.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BuildLogPrinterTest {

    @Test
    fun sameBuildIdExecutesInStrictFifoOrder() {
        val executors = BuildLogPrinter.createStripedExecutors(stripeCount = 4, queueCapacity = 64)
        try {
            val buildId = "b-order-check"
            val stripe = BuildLogPrinter.stripeFor(buildId, executors)
            val observed = CopyOnWriteArrayList<String>()
            val done = CountDownLatch(1)

            BuildLogPrinter.submitToStripe(stripe, buildId, critical = true, task = Runnable {
                observed.add("start")
            })
            repeat(20) { idx ->
                BuildLogPrinter.submitToStripe(stripe, buildId, critical = false, task = Runnable {
                    observed.add("line-$idx")
                })
            }
            BuildLogPrinter.submitToStripe(stripe, buildId, critical = true, task = Runnable {
                observed.add("stop")
                done.countDown()
            })

            assertTrue(done.await(5, TimeUnit.SECONDS))
            val expected = buildList {
                add("start")
                repeat(20) { add("line-$it") }
                add("stop")
            }
            assertEquals(expected, observed)
        } finally {
            executors.forEach { it.shutdownNow() }
        }
    }

    @Test
    fun criticalCallFallsBackInlineWhenQueueIsFull() {
        val executors = BuildLogPrinter.createStripedExecutors(stripeCount = 1, queueCapacity = 1)
        val releaseBlocker = CountDownLatch(1)
        val blockerStarted = CountDownLatch(1)
        try {
            val stripe = executors[0]
            BuildLogPrinter.submitToStripe(stripe, "b-full", critical = false, task = Runnable {
                blockerStarted.countDown()
                releaseBlocker.await(5, TimeUnit.SECONDS)
            })
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS))

            // 占满容量为 1 的队列
            BuildLogPrinter.submitToStripe(stripe, "b-full", critical = false, task = Runnable {})

            val nonCriticalExecuted = CopyOnWriteArrayList<String>()
            BuildLogPrinter.submitToStripe(stripe, "b-full", critical = false, task = Runnable {
                nonCriticalExecuted.add("dropped")
            })

            val criticalExecuted = CopyOnWriteArrayList<String>()
            BuildLogPrinter.submitToStripe(
                stripe = stripe,
                buildId = "b-full",
                critical = true,
                task = Runnable { criticalExecuted.add("stopLog") },
                criticalOfferTimeoutMs = 20L
            )

            assertTrue(nonCriticalExecuted.isEmpty())
            assertEquals(listOf("stopLog"), criticalExecuted)
        } finally {
            releaseBlocker.countDown()
            executors.forEach { it.shutdownNow() }
        }
    }
}
