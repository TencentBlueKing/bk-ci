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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.artifact.hash

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class HashAlgorithmTest {

    private val content = "Hello, world!"
    private val md5 = "6cd3556deb0da54bca060b4c39479839"
    private val sha1 = "943a702d06f34599aee1f8da8ef9f7296031d699"
    private val sha256 = "315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3"

    @Test
    fun testInputStream() {
        Assertions.assertEquals(md5, content.byteInputStream().md5())
        Assertions.assertEquals(sha1, content.byteInputStream().sha1())
        Assertions.assertEquals(sha256, content.byteInputStream().sha256())
    }

    @Test
    fun testString() {
        Assertions.assertEquals(md5, content.md5())
        Assertions.assertEquals(sha1, content.sha1())
        Assertions.assertEquals(sha256, content.sha256())
    }
}
