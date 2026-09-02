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

package com.tencent.devops.project.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.text.Collator
import java.util.Locale

class ProjectNamePinyinHelperTest {

    private val collator = Collator.getInstance(Locale.CHINA)

    @Test
    fun firstLetterUsesPinyinInitialForChinese() {
        assertEquals('a', ProjectNamePinyinHelper.firstLetter("啊项目"))
        assertEquals('b', ProjectNamePinyinHelper.firstLetter("吧项目"))
        assertEquals('c', ProjectNamePinyinHelper.firstLetter("测试11"))
        assertEquals('s', ProjectNamePinyinHelper.firstLetter(" 数据迁移测试dev18"))
        assertEquals('t', ProjectNamePinyinHelper.firstLetter("腾旭"))
        assertEquals('z', ProjectNamePinyinHelper.firstLetter("组织架构修正"))
    }

    @Test
    fun firstLetterUsesEnglishLetter() {
        assertEquals('a', ProjectNamePinyinHelper.firstLetter("aaa-rbac-dev"))
        assertEquals('b', ProjectNamePinyinHelper.firstLetter("bkdevops"))
        assertEquals('c', ProjectNamePinyinHelper.firstLetter("ceshi11"))
        assertEquals('z', ProjectNamePinyinHelper.firstLetter("zhenwtest"))
    }

    @Test
    fun chineseAndEnglishInterleaveByFirstLetter() {
        val names = listOf(
            "测试11",
            "zhenwtest",
            "啊项目",
            "bkdevops",
            "1",
            "ceshi11",
            "_greysonfang1",
            "aaa-rbac-dev",
            "吧项目",
            "组织架构修正"
        )
        val sorted = names.sortedWith { left, right ->
            ProjectNamePinyinHelper.compare(left, right, collator)
        }

        assertEquals(setOf("_greysonfang1", "1"), sorted.subList(0, 2).toSet())
        assertEquals(setOf("啊项目", "aaa-rbac-dev"), sorted.subList(2, 4).toSet())
        assertEquals(setOf("吧项目", "bkdevops"), sorted.subList(4, 6).toSet())
        assertEquals(setOf("测试11", "ceshi11"), sorted.subList(6, 8).toSet())
        assertEquals(setOf("zhenwtest", "组织架构修正"), sorted.subList(8, 10).toSet())
    }
}
