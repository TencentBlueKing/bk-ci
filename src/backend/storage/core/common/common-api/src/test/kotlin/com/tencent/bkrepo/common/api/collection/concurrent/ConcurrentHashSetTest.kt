package com.tencent.bkrepo.common.api.collection.concurrent

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

internal class ConcurrentHashSetTest {

    @Test
    fun concurrentTest() {
        val set = ConcurrentHashSet<Int>()
        val count = 1000
        val latch = CountDownLatch(count)
        var sum = 0
        // 并发添加
        repeat(count) {
            thread {
                set.add(it)
                latch.countDown()
            }
            sum += it
        }
        latch.await()
        assertEquals(count, set.size)

        // 内部数据校验
        var sum2 = 0
        assertDoesNotThrow {
            set.iterator().forEach {
                sum2 += it
            }
        }
        assertEquals(sum, sum2)

        // 并发移除
        val latch1 = CountDownLatch(count)
        repeat(count) {
            thread {
                set.remove(it)
                latch1.countDown()
            }
        }
        latch1.await()
        assertEquals(0, set.size)

        // 并发添加和移除
        val threads = arrayListOf<Thread>()
        repeat(count) {
            thread {
                assertDoesNotThrow {
                    set.add(it)
                    set.remove(it)
                }
            }.apply {
                threads.add(this)
            }
        }
        threads.forEach { it.join() }
    }
}
