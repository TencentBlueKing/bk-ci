package com.tencent.devops.common.security.util

import org.junit.Assert
import org.junit.Test

class EnvironmentUtilTest {
    @Test
    fun getActiveProfile() {
        // 判断是否需要验证
        val activeProfile = EnvironmentUtil.getActiveProfile()
        Assert.assertEquals("", activeProfile)
    }

    @Test
    fun getApplicationName() {
        // 判断是否需要验证
        val applicationNam = EnvironmentUtil.getApplicationName()
        Assert.assertEquals("", applicationNam)
    }

    @Test
    fun getServerPort() {
        // 判断是否需要验证
        val serverPort = EnvironmentUtil.getServerPort()
        Assert.assertEquals(0, serverPort)
    }

    @Test
    fun isProdProfileActive() {
        // 判断是否需要验证
        val isProdProfileActive = EnvironmentUtil.isProdProfileActive()
        Assert.assertEquals(false, isProdProfileActive)
    }
}