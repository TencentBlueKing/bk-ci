/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.job.batch.base

import com.tencent.bkrepo.job.executor.BlockThreadPoolTaskExecutorDecorator
import com.tencent.bkrepo.job.executor.IdentityTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.jupiter.api.assertThrows
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class BlockThreadPoolTaskExecutorDecoratorTest {

    @Test
    fun maxAvailableTest() {
        val executor = BlockThreadPoolTaskExecutorDecorator(newSingleThreadExecutor(), 20)
        repeat(20) {
            executor.execute {
                println("start")
                TimeUnit.MILLISECONDS.sleep(100)
                println("end")
            }
        }
        val add = AtomicInteger()
        thread {
            executor.execute {}
            println("add")
            add.incrementAndGet()
        }
        Assertions.assertEquals(0, add.get())
        TimeUnit.MILLISECONDS.sleep(150)
        Assertions.assertEquals(1, add.get())
    }

    @Test
    fun idTaskTest() {
        val executor = BlockThreadPoolTaskExecutorDecorator(newSingleThreadExecutor(), 20)
        val id = "id"
        repeat(2) {
            val task = IdentityTask(
                id,
                Runnable {
                    println("$it start ${LocalDateTime.now()}")
                    TimeUnit.MILLISECONDS.sleep(100)
                    println("$it end ${LocalDateTime.now()}")
                }
            )
            executor.executeWithId(task)
        }
        executor.complete(id)
        val num = AtomicInteger()
        thread {
            executor.get(id, 1000)
            num.incrementAndGet()
        }
        Assertions.assertEquals(0, num.get())
        TimeUnit.MILLISECONDS.sleep(300)
        Assertions.assertEquals(1, num.get())
    }

    @Test
    fun deadLock() {
        // 线程池执行一个任务，这个任务会产生新的任务。在任务达到限制数量时，会进行等待。
        // 依次类推，线程池的工作线程被这些生产任务占满，同时这些生产任务又在等待线程池里工作线程结束任务，产生死锁。
        val executor = BlockThreadPoolTaskExecutorDecorator(newSingleThreadExecutor(), 1)
        val num = AtomicInteger()
        executor.execute {
            // 已经达到限制，此时生产一个任务，即会进行等待自身。导致死锁。
            executor.execute { }
            num.incrementAndGet()
        }
        TimeUnit.MILLISECONDS.sleep(100)
        Assertions.assertEquals(0, num.get())
    }

    @Test
    fun noDeadLock() {
        val executor = BlockThreadPoolTaskExecutorDecorator(newSingleThreadExecutor(), 1)
        val num = AtomicInteger()
        // 使用executeProduce避免死锁
        executor.executeProduce {
            executor.execute { }
            num.incrementAndGet()
        }
        TimeUnit.MILLISECONDS.sleep(100)
        Assertions.assertEquals(1, num.get())
    }

    @Test
    fun waitTimeoutTest() {
        val executor = BlockThreadPoolTaskExecutorDecorator(newSingleThreadExecutor(), 1)
        val timeout = 1000L
        val identityTask = IdentityTask(id = "id", runnable = Runnable { TimeUnit.MILLISECONDS.sleep(timeout * 2) })
        executor.executeWithId(identityTask)
        assertThrows<TimeoutException> { executor.completeAndGet(identityTask.id, timeout) }
    }

    @Test
    fun rateLimitTest() {
        val executor = BlockThreadPoolTaskExecutorDecorator(newFixThreadExecutor(100), 100)
        val id = "id"
        val begin = System.currentTimeMillis()
        repeat(100) {
            val identityTask = IdentityTask(id = id, runnable = Runnable { TimeUnit.MILLISECONDS.sleep(100) })
            executor.executeWithId(identityTask, permitsPerSecond = 30.0)
        }
        executor.completeAndGet(id, 5000)
        val end = System.currentTimeMillis()
        val spend = end - begin
        println(spend)
        Assertions.assertTrue(spend > 3000)
    }

    private fun newSingleThreadExecutor(): ThreadPoolTaskExecutor {
        return newFixThreadExecutor(1)
    }

    private fun newFixThreadExecutor(n: Int): ThreadPoolTaskExecutor {
        val pool = ThreadPoolTaskExecutor()
        pool.corePoolSize = n
        pool.initialize()
        return pool
    }
}
