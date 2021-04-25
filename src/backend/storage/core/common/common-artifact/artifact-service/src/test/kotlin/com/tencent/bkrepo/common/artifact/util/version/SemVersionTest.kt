/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.util.version

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SemVersionTest {

    @Test
    fun testSemVer() {
        println(SemVersion.parse("0.0.1"))
        println(SemVersion.parse("0.1"))
        println(SemVersion.parse("0.1.1"))
        println(SemVersion.parse("0"))
        println(SemVersion.parse("1.0"))
        println(SemVersion.parse("1.1.0"))
        println(SemVersion.parse("1"))
        println(SemVersion.parse("1.1-alpha-2"))
    }

    @Test
    fun testSemVerOrdinal() {
        Assertions.assertEquals(19999, SemVersion.parse("0.0.1").ordinal(4))
        Assertions.assertEquals(1000000019999, SemVersion.parse("1.0.1").ordinal(4))
        Assertions.assertEquals(1000200019999, SemVersion.parse("1.2.1").ordinal(4))
        Assertions.assertEquals(1000200010000, SemVersion.parse("1.2.1-SNAPSHOT").ordinal(4))
        Assertions.assertEquals(3000300129999, SemVersion.parse("3.3.12").ordinal(4))
        Assertions.assertEquals(1000100019999, SemVersion.parse("1.1.1").ordinal(4))
    }
}
