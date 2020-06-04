package com.tencent.devops.common.api.exception

import org.junit.Assert
import org.junit.Test
import javax.ws.rs.core.Response

/**
 * @version 1.0
 */
class ErrorCodeExceptionTest {

    @Test
    fun test() {
        val params = emptyArray<String>()
        val e = ErrorCodeException("1", params, null)
        Assert.assertNull(e.defaultMessage)
        Assert.assertArrayEquals(params, e.params)
        Assert.assertEquals(e.status, Response.Status.INTERNAL_SERVER_ERROR)
        Assert.assertEquals(e.errorCode, "1")
    }
}
