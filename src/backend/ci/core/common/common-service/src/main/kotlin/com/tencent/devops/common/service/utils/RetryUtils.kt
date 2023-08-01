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

package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.RemoteServiceException
import org.slf4j.LoggerFactory

/**
 *
 * @version 1.0
 */
object RetryUtils {
    private val logger = LoggerFactory.getLogger(RetryUtils::class.java)

    fun <T> execute(action: Action<T>, retryTime: Int = 1, retryPeriodMills: Long = 500): T {
        try {
            return action.execute()
        } catch (ignored: Throwable) {
            if (retryTime - 1 <= 0) {
                return action.fail(ignored)
            }
            Thread.sleep(retryPeriodMills)
            return execute(action, retryTime - 1)
        }
    }

    @Throws(ClientException::class)
    fun <T> clientRetry(retryTime: Int = 5, retryPeriodMills: Long = 500, action: () -> T): T {
        return try {
            action()
        } catch (re: ClientException) {
            if (retryTime - 1 < 0) {
                throw re
            }
            if (retryPeriodMills > 0) {
                Thread.sleep(retryPeriodMills)
            }
            clientRetry(action = action, retryTime = retryTime - 1, retryPeriodMills = retryPeriodMills)
        } catch (e: RemoteServiceException) {
            // 对限流重试
            if (e.httpStatus != 429) throw e
            if (retryTime - 1 < 0) {
                throw e
            }
            logger.info("Remote service return 429 and message:${e.message}")
            // 固定延迟1s
            Thread.sleep(1000)
            clientRetry(action = action, retryTime = retryTime - 1)
        }
    }

    fun <T> retryAnyException(retryTime: Int = 3, retryPeriodMills: Long = 50, action: (retryTime: Int) -> T): T {
        return try {
            action(retryTime)
        } catch (re: Throwable) {
            if (retryTime - 1 < 0) {
                throw re
            }
            if (retryPeriodMills > 0) {
                Thread.sleep(retryPeriodMills)
            }
            retryAnyException(action = action, retryTime = retryTime - 1, retryPeriodMills = retryPeriodMills)
        }
    }

    interface Action<out T> {

        fun execute(): T

        /**
         * 默认直接抛出异常
         */
        fun fail(e: Throwable): T = throw e
    }
}
