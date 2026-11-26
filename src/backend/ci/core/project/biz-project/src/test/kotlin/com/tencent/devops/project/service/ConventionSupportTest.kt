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

package com.tencent.devops.project.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConventionSupportTest {

    @Test
    fun languageFallsBackToChinese() {
        assertEquals(ConventionSupport.LANG_ZH, ConventionSupport.normalizeLang(null))
        assertEquals(ConventionSupport.LANG_ZH, ConventionSupport.normalizeLang("zh-CN"))
        assertEquals(ConventionSupport.LANG_EN, ConventionSupport.normalizeLang("en-US"))
        assertEquals(ConventionSupport.LANG_EN, ConventionSupport.normalizeLang("EN"))
    }

    @Test
    fun agreeTimeIsShownToTheMinute() {
        val time = LocalDateTime.of(2026, 11, 16, 16, 35, 48)
        assertEquals("2026-11-16 16:35", ConventionSupport.formatMinute(time))
        assertNull(ConventionSupport.formatMinute(null))
    }

    @Test
    fun blankHtmlCannotBeSaved() {
        assertTrue(ConventionSupport.blankContent(null))
        assertTrue(ConventionSupport.blankContent("  "))
        assertTrue(ConventionSupport.blankContent("<p><br/></p>"))
        assertTrue(ConventionSupport.blankContent("<p>&nbsp;</p>"))
        assertFalse(ConventionSupport.blankContent("<p>使用公约</p>"))
    }
}
