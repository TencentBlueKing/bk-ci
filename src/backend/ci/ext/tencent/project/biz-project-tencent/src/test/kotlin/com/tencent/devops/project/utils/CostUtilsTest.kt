package com.tencent.devops.project.utils

import org.junit.Test

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class CostUtilsTest {

    @Test
    fun costTime1() {
        val startTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1)
        val url = "http://test"
        CostUtils.costTime(startTime, url, logger)
    }

    @Test
    fun costTime2() {
        val startTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)
        val url = "http://test"
        CostUtils.costTime(startTime, url, logger)
    }

    @Test
    fun costTime3() {
        val startTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5)
        val url = "http://test"
        CostUtils.costTime(startTime, url, logger)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}