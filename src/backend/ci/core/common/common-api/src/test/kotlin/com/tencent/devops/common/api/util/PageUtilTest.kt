package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PageUtilTest {
    @Test
    fun test() {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(null, null)
        Assertions.assertEquals(-1, sqlLimit.limit)
        Assertions.assertEquals(0, sqlLimit.offset)
    }
}
