/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.scm.utils

import java.net.SocketTimeoutException
import org.slf4j.LoggerFactory

object RetryUtils {

    private val logger = LoggerFactory.getLogger(RetryUtils::class.java)

    fun <T> retryFun(
        funName: String,
        action: () -> T
    ): T {
        try {
            return timeoutRetry(
                5,
                500
            ) {
                action()
            }
        } catch (e: SocketTimeoutException) {
            logger.warn("scm $funName request timeout retry 5 times error: ${e.message} ")
            throw e
        }
    }

    private fun <T> timeoutRetry(retryTime: Int = 5, retryPeriodMills: Long = 500, action: () -> T): T {
        return try {
            action()
        } catch (re: SocketTimeoutException) {
            if (retryTime - 1 < 0) {
                throw re
            }
            if (retryPeriodMills > 0) {
                Thread.sleep(retryPeriodMills)
            }
            timeoutRetry(action = action, retryTime = retryTime - 1, retryPeriodMills = retryPeriodMills)
        }
    }
}
