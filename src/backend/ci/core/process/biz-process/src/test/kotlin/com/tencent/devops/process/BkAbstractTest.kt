package com.tencent.devops.process

import com.tencent.devops.common.redis.RedisLock
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkClass
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.Mock
import org.jooq.tools.jdbc.MockConnection
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

open class BkAbstractTest {
    val dslContext: DSLContext = DSL.using(MockConnection(Mock.of(0)), SQLDialect.MYSQL)
    fun <R : Record> DSLContext.mockResult(t: Table<R>, vararg records: R): Result<R> {
        val result = newResult(t)
        records.forEach { result.add(it) }
        return result
    }

    fun MockKMatcherScope.anyDslContext(): DSLContext = any() as DSLContext

    @BeforeEach
    fun mockRedisLock() {
        val lock = mockkClass(RedisLock::class)
        justRun { lock.lock() }
        every { lock.tryLock() } returns true
        every { lock.unlock() } returns true
//            every { lock.lockAround(any() as (() -> Any)) } answers { callOriginal() }
    }

    companion object {
//        @JvmStatic
//        @BeforeAll
//        fun mockRedisLock() {
//            val lock = mockkClass(RedisLock::class)
//            justRun { lock.lock() }
//            every { lock.tryLock() } returns true
//            every { lock.unlock() } returns true
////            every { lock.lockAround(any() as (() -> Any)) } answers { callOriginal() }
//        }
    }
}
