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

import org.junit.Assert.assertEquals
import org.junit.Test

class HashUtilTest {

    @Test
    fun longId() {
        val id = 3L
        val hashId = HashUtil.encodeLongId(id)
        assertEquals(id, HashUtil.decodeIdToLong(hashId))
        println("longId=$hashId")
        println(HashUtil.decodeIdToLong("rqmaddpk"))
        println(HashUtil.decodeIdToLong("qjmbwapv"))
    }

    @Test
    fun otherLongId() {
        val id = 3L
        val hashId = HashUtil.encodeOtherLongId(id)
        println("otherLongId=$hashId")
        assertEquals(id, HashUtil.decodeOtherIdToLong(hashId))
    }

    @Test
    fun intId() {
        val id = 3
        val hashId = HashUtil.encodeIntId(id)
        println("intId=$hashId")
        assertEquals(id, HashUtil.decodeIdToInt(hashId))
    }

    @Test
    fun otherIntId() {
        val id = 3
        val hashId = HashUtil.encodeOtherIntId(id)
        println("otherIntId=$hashId")
        assertEquals(id, HashUtil.decodeOtherIdToInt(hashId))
    }
}