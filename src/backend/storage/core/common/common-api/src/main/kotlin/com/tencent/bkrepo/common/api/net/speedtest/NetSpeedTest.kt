/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 * 
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.api.net.speedtest

import com.tencent.bkrepo.common.api.util.HumanReadable
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * 网络带宽测速
 * */
class NetSpeedTest(
    val settings: SpeedTestSettings
) {
    private val logger = LoggerFactory.getLogger(NetSpeedTest::class.java)

    /**
     * upload test
     * @return bytes/s
     * */
    fun uploadTest(): Long {
        with(settings) {
            val total = AtomicLong()
            val maxSend = settings.maxBlobMegabytes * Counter.MB.toLong()
            val start = System.currentTimeMillis()
            val doUpload: () -> Unit = {
                while (System.currentTimeMillis() - start < timeoutInSecond * 1000) {
                    val counter = Counter(maxSend, total)
                    counter.start()
                    post(uploadUrl, counter)
                    logger.debug("Speed ${HumanReadable.size(counter.avgBytes())}/s ,send total ${counter.total}")
                }
            }
            val countDownLatch = CountDownLatch(concurrent)
            for (i in 0 until concurrent) {
                thread {
                    doUpload()
                    countDownLatch.countDown()
                }
                TimeUnit.MILLISECONDS.sleep(i * 200L)
            }
            countDownLatch.await(timeoutInSecond.toLong(), TimeUnit.SECONDS)
            val totalBytes = total.get()
            return totalBytes / (System.currentTimeMillis() - start) * 1000
        }
    }

    fun post(url: String, body: InputStream) {
        val requestUrl = URL(url)
        with(requestUrl.openConnection() as HttpURLConnection) {
            try {
                doOutput = true
                requestMethod = POST
                body.use {
                    body.copyTo(outputStream)
                }
                responseCode
            } finally {
                disconnect()
            }
        }
    }

    companion object {
        const val POST = "POST"
    }
}
