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

package com.tencent.devops.process.yaml.utils

import com.tencent.devops.common.api.exception.InvalidParamException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class YamlMaxParallelParserTest {

    @Test
    fun parseNotConfigured() {
        // 未配置时数字与变量都为null，运行时取默认值
        assertEquals(Pair(null, null), YamlMaxParallelParser.parse(null))
        assertEquals(Pair(null, null), YamlMaxParallelParser.parse(""))
        assertEquals(Pair(null, null), YamlMaxParallelParser.parse("  "))
    }

    @Test
    fun parseNumber() {
        assertEquals(Pair(5, null), YamlMaxParallelParser.parse("5"))
        assertEquals(Pair(50, null), YamlMaxParallelParser.parse(" 50 "))
    }

    @Test
    fun parseVariable() {
        assertEquals(Pair(null, "\${MATRIX_MAX}"), YamlMaxParallelParser.parse("\${MATRIX_MAX}"))
        assertEquals(
            Pair(null, "\${{ variables.MATRIX_MAX }}"),
            YamlMaxParallelParser.parse("\${{ variables.MATRIX_MAX }}")
        )
        // 带函数调用的表达式同样视为变量，实际值由运行时解析
        assertEquals(
            Pair(null, "\${{ fromJSON(variables.MATRIX_MAX) }}"),
            YamlMaxParallelParser.parse("\${{ fromJSON(variables.MATRIX_MAX) }}")
        )
    }

    @Test
    fun parseInvalid() {
        // 既不是数字也不是变量表达式：直接失败，避免脏值被当成变量存到运行时才暴露
        assertThrows(InvalidParamException::class.java) { YamlMaxParallelParser.parse("abc") }
        assertThrows(InvalidParamException::class.java) { YamlMaxParallelParser.parse("1.5") }
    }

    @Test
    fun format() {
        // 变量优先
        assertEquals("\${MATRIX_MAX}", YamlMaxParallelParser.format("\${MATRIX_MAX}", 5))
        assertEquals("5", YamlMaxParallelParser.format(null, 5))
        assertEquals("5", YamlMaxParallelParser.format("  ", 5))
        assertNull(YamlMaxParallelParser.format(null, null))
    }

    @Test
    fun roundTrip() {
        val (maxConcurrency, maxConcurrencyVar) = YamlMaxParallelParser.parse("\${{ variables.MATRIX_MAX }}")
        assertEquals(
            "\${{ variables.MATRIX_MAX }}",
            YamlMaxParallelParser.format(maxConcurrencyVar, maxConcurrency)
        )
        val (number, numberVar) = YamlMaxParallelParser.parse("20")
        assertEquals("20", YamlMaxParallelParser.format(numberVar, number))
    }
}
