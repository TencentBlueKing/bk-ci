/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.util.TaskUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CancelTaskSignalServiceTest {

    private val buildId = "b1"
    private val containerId = "1"
    private val queueKey = TaskUtils.getCancelTaskIdRedisKey(buildId, containerId)
    private val member = TaskUtils.getCancelSignalMember(buildId, containerId)

    /**
     * 常态：索引里没有这个 Job，除了首次兜底探测外不应再去读队列。
     */
    @Test
    fun skipQueueWhenNoSignal() {
        val redisOperation = redis(emptySet())
        val service = CancelTaskSignalService(redisOperation)

        // 首次心跳会做一次兜底探测
        Assertions.assertNull(service.drainCancelTaskIds(buildId, containerId))
        // 兜底间隔内的后续心跳不再访问队列
        repeat(10) { Assertions.assertNull(service.drainCancelTaskIds(buildId, containerId)) }

        verify(exactly = 1) { redisOperation.rightPop(queueKey) }
    }

    /**
     * 索引命中时必须把队列读干，取消语义与原实现一致。
     */
    @Test
    fun drainQueueWhenSignalHit() {
        val redisOperation = redis(setOf(member))
        every { redisOperation.rightPop(queueKey) } returnsMany listOf("t1", "t2", null)
        val service = CancelTaskSignalService(redisOperation)

        Assertions.assertEquals(setOf("t1", "t2"), service.drainCancelTaskIds(buildId, containerId))
    }

    /**
     * 同实例写入取消信号后，本次心跳就要能看到，不用等快照过期。
     */
    @Test
    fun markSignalVisibleImmediately() {
        val redisOperation = redis(emptySet())
        every { redisOperation.zadd(TaskUtils.CANCEL_SIGNAL_INDEX_KEY, member, any()) } returns true
        every { redisOperation.rightPop(queueKey) } returnsMany listOf(null, "t1", null)
        val service = CancelTaskSignalService(redisOperation)

        // 先跑掉首次兜底探测，确保后面的命中来自索引
        service.drainCancelTaskIds(buildId, containerId)
        service.markCancelSignal(buildId, containerId)

        Assertions.assertEquals(setOf("t1"), service.drainCancelTaskIds(buildId, containerId))
    }

    /**
     * 刷新只取分数最高的有限条，清理异常导致堆积时读取量不会被放大。
     */
    @Test
    fun refreshReadsBoundedMembers() {
        val redisOperation = redis(emptySet())
        val service = CancelTaskSignalService(redisOperation)

        service.drainCancelTaskIds(buildId, containerId)

        verify { redisOperation.zrevrange(TaskUtils.CANCEL_SIGNAL_INDEX_KEY, 0, 1999) }
        verify(exactly = 0) { redisOperation.zrange(any(), any(), any()) }
    }

    /**
     * 索引不可用时退回原来的"每次心跳都读队列"逻辑，不能漏掉取消。
     */
    @Test
    fun fallbackToQueueWhenIndexUnavailable() {
        val redisOperation = mockk<RedisOperation>()
        every {
            redisOperation.zrevrange(TaskUtils.CANCEL_SIGNAL_INDEX_KEY, any(), any())
        } throws RuntimeException("redis down")
        every { redisOperation.rightPop(queueKey) } returns null
        val service = CancelTaskSignalService(redisOperation)

        repeat(3) { Assertions.assertNull(service.drainCancelTaskIds(buildId, containerId)) }

        verify(atLeast = 3) { redisOperation.rightPop(queueKey) }
    }

    private fun redis(members: Set<String>): RedisOperation {
        val redisOperation = mockk<RedisOperation>()
        every { redisOperation.zrevrange(TaskUtils.CANCEL_SIGNAL_INDEX_KEY, any(), any()) } returns members
        every {
            redisOperation.zremoveRangeByScore(TaskUtils.CANCEL_SIGNAL_INDEX_KEY, any(), any())
        } returns 0L
        every { redisOperation.rightPop(any()) } returns null
        return redisOperation
    }
}
