package com.tencent.devops.common.api.util

import org.junit.Assert
import org.junit.Test

internal class PageUtilTest {
    @Test
    fun test() {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(null, null)
        Assert.assertEquals(-1, sqlLimit.limit)
        Assert.assertEquals(0, sqlLimit.offset)
    }
}
