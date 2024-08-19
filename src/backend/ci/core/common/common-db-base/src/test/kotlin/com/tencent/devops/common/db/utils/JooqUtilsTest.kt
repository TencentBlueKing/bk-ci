package com.tencent.devops.common.db.utils

import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JooqUtilsTest {

    @Test
    fun retryWhenDeadLock() {
        var actual = 0
        JooqUtils.retryWhenDeadLock {
            if (actual++ < 1) {
                throw DataAccessException("mock sql dead lock; ${JooqUtils.JooqDeadLockMessage}")
            }
        }
        val expect = 2 // retry
        Assertions.assertEquals(expect, actual)
    }

    @Test
    @DisplayName("重试多次")
    fun retryWhenDeadLock_2() {
        var actual = 0
        JooqUtils.retryWhenDeadLock(3) {
            if (actual++ < 3) {
                throw DataAccessException("mock sql dead lock; ${JooqUtils.JooqDeadLockMessage}")
            }
        }
        val expect = 4 // retry
        Assertions.assertEquals(expect, actual)
    }

    @Test
    fun assertThrowsRetryWhenDeadLock() {
        var actual = 0
        Assertions.assertThrows(DataAccessException::class.java) {
            JooqUtils.retryWhenDeadLock {
                if (actual++ < 1) {
                    throw DataAccessException("mock sql no dead lock; ")
                }
            }
        }
        val expect = 1 // not retry
        Assertions.assertEquals(expect, actual)
    }
}
