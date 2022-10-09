package com.tencent.devops.process

import com.tencent.devops.common.redis.RedisOperation
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.Mock
import org.jooq.tools.jdbc.MockConnection
import org.junit.jupiter.api.BeforeAll
import org.springframework.data.redis.core.RedisCallback

open class BkAbstractTest {
    val dslContext: DSLContext = DSL.using(MockConnection(Mock.of(0)), SQLDialect.MYSQL)

    fun <R : Record> DSLContext.mockResult(t: Table<R>, vararg records: R): Result<R> {
        val result = newResult(t)
        records.forEach { result.add(it) }
        return result
    }

    fun MockKMatcherScope.anyDslContext(): DSLContext = any() as DSLContext

    companion object {
        val redisOperation: RedisOperation = mockk(relaxed = true)

        @JvmStatic
        @BeforeAll
        @SuppressWarnings("TooGenericExceptionThrown")
        fun mockRedisOperation() {
            every { redisOperation.execute(any<RedisCallback<*>>()) } answers {
                val argStr = args[0]!!::class.toString()
                if (argStr.contains("RedisLock\$set")) {
                    return@answers "OK"
                } else if (argStr.contains("RedisLock\$unlock")) {
                    return@answers true
                } else {
                    throw Exception("redisOperation.execute must mock by self")
                }
            }
        }
    }
}
