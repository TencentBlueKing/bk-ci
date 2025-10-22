/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.sdk.common.util

import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SdkRetryHelper(
    // 最大重试次数
    private val maxAttempts: Int = 3,
    // 重试等待时间(ms),默认睡眠500ms
    private val retryWaitTime: Long = 500
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SdkRetryHelper::class.java)
    }

    fun <T> execute(action: () -> T): T {
        var attempt = 1
        while (attempt <= maxAttempts) {
            try {
                return action()
            } catch (e: UnknownHostException) {
                logger.error(e.message)
            } catch (e: ConnectException) {
                logger.error(e.message)
            } catch (e: SocketTimeoutException) {
                if (e.message == "connect timed out" || e.message == "timeout") {
                    logger.error(e.message)
                } else {
                    throw e
                }
            }
            logger.info("Attempting $attempt|Waiting ${retryWaitTime}ms before trying again")
            Thread.sleep(retryWaitTime)
            attempt++
        }
        return action()
    }
}
