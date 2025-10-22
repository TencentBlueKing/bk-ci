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

package com.tencent.devops.websocket.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

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
        Assertions.assertEquals("db39ec000cd044ff90b16f9164f3", sessionId1)
        Assertions.assertEquals("8a80fe4c0254e18921ff5a588720", sessionId2)
        Assertions.assertEquals(null, sessionId3)
        Assertions.assertEquals(null, sessionId4)
        Assertions.assertNotEquals("sessionId=8a80fe4c0254e18921ff5a588720&t=1597892754480", sessionId2)
    }
}
