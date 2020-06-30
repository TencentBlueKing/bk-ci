package com.tencent.devops.common.security.util

import org.junit.Assert
import org.junit.Test

class EnvironmentUtilTest {
    @Test
    fun getActiveProfile() {
        // 获取profile active信息
        val activeProfile = EnvironmentUtil.getActiveProfile()
        Assert.assertEquals("", activeProfile)
    }

    @Test
    fun getApplicationName() {
        // 获取application name信息
        val applicationNam = EnvironmentUtil.getApplicationName()
        Assert.assertEquals("", applicationNam)
    }

    @Test
    fun getServerPort() {
        // 获取server port信息
        val serverPort = EnvironmentUtil.getServerPort()
        Assert.assertEquals(0, serverPort)
    }

    @Test
    fun isProdProfileActive() {
        // 判断是否为生产环境
        val isProdProfileActive = EnvironmentUtil.isProdProfileActive()
        Assert.assertEquals(false, isProdProfileActive)
    }
}