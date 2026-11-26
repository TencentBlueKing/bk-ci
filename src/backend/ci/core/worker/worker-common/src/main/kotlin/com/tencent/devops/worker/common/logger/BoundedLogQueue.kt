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

package com.tencent.devops.worker.common.logger

import com.tencent.devops.common.log.pojo.message.LogMessage
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 条数 + 预估字节双重有界的日志上报队列。
 *
 * 正常突发时通过 [offer] 提供温和背压而不丢行；当队列为空时允许单条超限消息入队避免死锁。
 * 发生熔断或停止时调用 [clear] / [signalAll] 可立即唤醒所有阻塞中的生产者与消费者。
 */
internal class BoundedLogQueue(
    private val maxCount: Int = LoggerUploadBatch.QUEUE_MAX_COUNT,
    private val maxBytes: Long = LoggerUploadBatch.QUEUE_MAX_BYTES
) {

    private data class Entry(val message: LogMessage, val bytes: Int)

    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()
    private val notFull = lock.newCondition()
    private val items = ArrayDeque<Entry>()
    private var bytesInQueue: Long = 0L

    @Throws(InterruptedException::class)
    fun offer(message: LogMessage, timeoutMs: Long): Boolean {
        val bytes = LoggerUploadBatch.estimateBytes(message.message)
        var remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs.coerceAtLeast(0L))
        lock.lockInterruptibly()
        try {
            while (!canAcceptLocked(bytes)) {
                if (remainingNanos <= 0L) {
                    return false
                }
                remainingNanos = notFull.awaitNanos(remainingNanos)
            }
            items.addLast(Entry(message, bytes))
            bytesInQueue += bytes
            notEmpty.signal()
            return true
        } finally {
            lock.unlock()
        }
    }

    @Throws(InterruptedException::class)
    fun awaitNotEmpty(timeoutMs: Long): Boolean {
        var remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs.coerceAtLeast(0L))
        lock.lockInterruptibly()
        try {
            while (items.isEmpty()) {
                if (remainingNanos <= 0L) {
                    return false
                }
                remainingNanos = notEmpty.awaitNanos(remainingNanos)
            }
            return true
        } finally {
            lock.unlock()
        }
    }

    /**
     * 按条数和字节上限批量取出到 [target]。若 [target] 原本为空且队列非空，至少取出 1 条防止卡死。
     */
    fun drainTo(target: MutableList<LogMessage>, maxCount: Int, maxBytes: Long): Int {
        if (maxCount <= 0 || maxBytes <= 0L) {
            return 0
        }
        lock.withLock {
            var moved = 0
            var movedBytes = 0L
            while (items.isNotEmpty() && moved < maxCount) {
                val head = items.peekFirst()
                if (moved > 0 && movedBytes + head.bytes > maxBytes) {
                    break
                }
                items.removeFirst()
                bytesInQueue -= head.bytes
                target.add(head.message)
                moved++
                movedBytes += head.bytes
            }
            if (moved > 0) {
                notFull.signalAll()
            }
            return moved
        }
    }

    fun clear() {
        lock.withLock {
            items.clear()
            bytesInQueue = 0L
            notFull.signalAll()
            notEmpty.signalAll()
        }
    }

    fun signalAll() {
        lock.withLock {
            notFull.signalAll()
            notEmpty.signalAll()
        }
    }

    fun isEmpty(): Boolean = lock.withLock { items.isEmpty() }

    fun size(): Int = lock.withLock { items.size }

    fun pendingBytes(): Long = lock.withLock { bytesInQueue }

    private fun canAcceptLocked(bytes: Int): Boolean {
        return items.isEmpty() || (items.size < maxCount && bytesInQueue + bytes <= maxBytes)
    }
}
