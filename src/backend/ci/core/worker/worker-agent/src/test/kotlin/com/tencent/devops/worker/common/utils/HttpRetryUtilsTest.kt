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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.util.HttpRetryUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.HttpRetryException
import java.net.SocketTimeoutException

class HttpRetryUtilsTest {

    @Test
    fun retry() {
        var i = 0
        var retryTime = 1
        var expected = 2
        val retryPeriodMills = 10L
        try {
            i = HttpRetryUtils.retry(retryTime = retryTime, retryPeriodMills = retryPeriodMills) {
                if (i++ < expected) {
                    throw SocketTimeoutException("$i")
                } else {
                    i
                }
            }
        } catch (re: SocketTimeoutException) {
            Assertions.assertEquals(expected, i)
        }

        i = 0
        retryTime = 2
        expected = 2
        i = HttpRetryUtils.retry(retryTime = retryTime, retryPeriodMills = retryPeriodMills) {
            if (i++ < expected) {
                throw SocketTimeoutException("$i")
            } else {
                i
            }
        }
        Assertions.assertEquals(expected, i - 1)

        retryTime = 2
        expected = 3
        i = 0
        try {
            HttpRetryUtils.retry(retryTime = retryTime, retryPeriodMills = retryPeriodMills) {
                if (i++ < expected) {
                    throw HttpRetryException("$i", 999)
                } else {
                    i
                }
            }
        } catch (re: HttpRetryException) {
            Assertions.assertEquals(expected, i)
        }

        i = 0
        retryTime = 5
        expected = 3
        i = HttpRetryUtils.retry(retryTime = retryTime, retryPeriodMills = retryPeriodMills) {
            if (i++ < expected) {
                throw HttpRetryException("$i", 999)
            } else {
                i
            }
        }

        Assertions.assertEquals(expected, i - 1)
    }

    @Test
    fun throwSocketTimeoutException() {
        Assertions.assertThrows(SocketTimeoutException::class.java) {
            var i = 0
            val retryTime = 5
            val expected = 3
            i = HttpRetryUtils.retryWhenHttpRetryException(retryTime = retryTime) {
                if (i++ < expected) {
                    throw SocketTimeoutException("$i")
                } else {
                    i
                }
            }

            Assertions.assertEquals(expected, i - 1)
        }
    }

    @Test
    fun throwHttpRetryException() {
        Assertions.assertThrows(HttpRetryException::class.java) {
            var i = 0
            val retryTime = 5
            val expected = 3
            i = HttpRetryUtils.retryWhenSocketTimeException(retryTime = retryTime) {
                if (i++ < expected) {
                    throw HttpRetryException("$i", 999)
                } else {
                    i
                }
            }

            Assertions.assertEquals(expected, i - 1)
        }
    }
}
