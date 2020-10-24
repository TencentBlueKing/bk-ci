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

package com.tencent.bkrepo.common.artifact.stream

import com.tencent.bkrepo.common.api.constant.StringPool.randomString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

internal class BoundedInputStreamTest {

    @Test
    fun testContentEquals() {
        val size = 10
        val content = randomString(size)
        val source = content.byteInputStream()
        val wrapper = BoundedInputStream(source, 100)
        val readContent = wrapper.readBytes().toString(Charset.defaultCharset())
        Assertions.assertEquals(content, readContent)
    }

    @Test
    fun testLimit() {
        val size = 10
        val content = randomString(size)
        val source = content.byteInputStream()
        val wrapper = BoundedInputStream(source, 5)
        Assertions.assertEquals(wrapper.available(), 5)
        Assertions.assertEquals(content[0], wrapper.read().toChar())
        Assertions.assertEquals(wrapper.available(), 4)

        Assertions.assertEquals(4, wrapper.read(ByteArray(5)))
        Assertions.assertEquals(wrapper.available(), 0)
        Assertions.assertEquals(-1, wrapper.read())
        Assertions.assertEquals(-1, wrapper.read(ByteArray(1)))
    }

    @Test
    fun testSkipAndLimit() {
        val size = 15
        val content = randomString(size)
        val source = content.byteInputStream()
        source.skip(5)
        val wrapper = BoundedInputStream(source, 3)
        Assertions.assertEquals(wrapper.available(), 3)
        Assertions.assertEquals(content[5], wrapper.read().toChar())
        Assertions.assertEquals(wrapper.available(), 2)

        Assertions.assertEquals(2, wrapper.read(ByteArray(5)))
        Assertions.assertEquals(wrapper.available(), 0)
        Assertions.assertEquals(-1, wrapper.read())
        Assertions.assertEquals(-1, wrapper.read(ByteArray(1)))
    }

    @Test
    fun testOverflowInt() {
        val size = 10
        val content = randomString(size)
        val source = content.byteInputStream()
        val wrapper = BoundedInputStream(source, Int.MAX_VALUE.toLong() + 1)

        val byteArray = ByteArray(4096)
        wrapper.read(byteArray)
    }
}
