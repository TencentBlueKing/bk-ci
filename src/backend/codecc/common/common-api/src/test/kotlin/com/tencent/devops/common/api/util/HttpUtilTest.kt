package com.tencent.devops.common.api.util

import com.tencent.devops.common.util.HttpUtil
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class HttpUtilTest {

    @Test
    fun getHttpClient() {
        Assert.assertNotNull(HttpUtil.getHttpClient())
    }
}
