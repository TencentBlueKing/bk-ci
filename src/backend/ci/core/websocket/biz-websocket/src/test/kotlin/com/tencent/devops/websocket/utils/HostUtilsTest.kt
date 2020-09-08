package com.tencent.devops.websocket.utils

import org.junit.Assert
import org.junit.Test

class HostUtilsTest {

    @Test
    fun getRealSession() {
        val query = "sessionId=db39ec000cd044ff90b16f9164f3"
        val query1 = "sessionId=8a80fe4c0254e18921ff5a588720&t=1597892754480"
        val query2 = ""
        val query3 = null
        val sessionId1 = HostUtils.getRealSession(query)
        val sessionId2 = HostUtils.getRealSession(query1)
        val sessionId3 = HostUtils.getRealSession(query2)
        val sessionId4 = HostUtils.getRealSession(query3)
        Assert.assertEquals("db39ec000cd044ff90b16f9164f3", sessionId1)
        Assert.assertEquals("8a80fe4c0254e18921ff5a588720", sessionId2)
        Assert.assertEquals(null, sessionId3)
        Assert.assertEquals(null, sessionId4)
        Assert.assertNotEquals("sessionId=8a80fe4c0254e18921ff5a588720&t=1597892754480", sessionId2)
    }
}