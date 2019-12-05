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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom

/**
 * @version 1.0
 */
class ShaUtilsTest {

    private var byteArray: ByteArray = byteArrayOf(1, 2, 3, 99, -12, -23, 69, 21, 112, -99)
    private val expectSha1 = "ec6a2bdb3c5dd567da1899216eab87358059520e"

    @Test
    fun sha1() {
        val sha1 = ShaUtils.sha1(byteArray)
        println("sha1=$sha1")
        Assert.assertEquals(expectSha1, sha1)
    }

    @Test
    fun hmacSha1() {
        val key = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(key)
        val hmacSha1 = ShaUtils.hmacSha1(key, byteArray)
        println("hmacSha1=$hmacSha1")
    }

    @Test
    fun isEqual() {
        val key = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(key)
        Assert.assertFalse(ShaUtils.isEqual(key, byteArray))
        val sha1 = ShaUtils.sha1(key)
        Assert.assertFalse(ShaUtils.isEqual(expectSha1, sha1))
    }
}
