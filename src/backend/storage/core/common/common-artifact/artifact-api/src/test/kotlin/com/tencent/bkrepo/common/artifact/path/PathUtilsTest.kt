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

package com.tencent.bkrepo.common.artifact.path

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("路径工具类测试")
class PathUtilsTest {

    @Test
    fun testNormalizeFullPath() {
        assertEquals(ROOT, PathUtils.normalizeFullPath("/"))
        assertEquals(ROOT, PathUtils.normalizeFullPath("/////\\\\//\\/"))
        assertEquals(ROOT, PathUtils.normalizeFullPath("\\"))
        assertEquals(ROOT, PathUtils.normalizeFullPath("  /   "))
        assertEquals(ROOT, PathUtils.normalizeFullPath("  "))
        assertEquals(ROOT, PathUtils.normalizeFullPath("/./"))
        assertEquals("/a", PathUtils.normalizeFullPath("  /   a"))
        assertEquals("/a/b", PathUtils.normalizeFullPath("  /   a  /b"))
        assertEquals("/a/b", PathUtils.normalizeFullPath("./a  \\b /"))
        assertEquals("/.1", PathUtils.normalizeFullPath("/.1/"))
        assertEquals("/....", PathUtils.normalizeFullPath("/..../"))
        assertEquals("/a/b/c", PathUtils.normalizeFullPath("./a/b/c"))
        assertEquals("/.a/b/c", PathUtils.normalizeFullPath("./.a/./b/c/."))

        // test ..
        assertEquals(ROOT, PathUtils.normalizeFullPath(".."))
        assertEquals(ROOT, PathUtils.normalizeFullPath("../"))
        assertEquals(ROOT, PathUtils.normalizeFullPath("../../../"))
        assertEquals("/a", PathUtils.normalizeFullPath("../a"))
        assertEquals("/a/..b", PathUtils.normalizeFullPath("../a/..b/"))
        assertEquals("/a/.b..", PathUtils.normalizeFullPath("../a/.b.."))
        assertEquals("/a", PathUtils.normalizeFullPath("../a/ . /"))
        assertEquals("/a/. .", PathUtils.normalizeFullPath("../a/ . . "))
        assertEquals(ROOT, PathUtils.normalizeFullPath("../a/  .. /"))
        assertEquals("/1/3/6", PathUtils.normalizeFullPath("..//1/2/..//3/4/5/../../6"))
    }

    @Test
    fun testNormalizePath() {
        assertEquals(ROOT, PathUtils.normalizePath(""))
        assertEquals(ROOT, PathUtils.normalizePath("/"))
        assertEquals("/.*|^/a/", PathUtils.normalizePath("/.*|^/a"))
        assertEquals("/.*|^/a", PathUtils.normalizeFullPath("/.*|^/a"))
        assertEquals("/a/b/", PathUtils.normalizePath("/a/b"))
        assertEquals("/a/b/", PathUtils.normalizePath("/a/b/"))
    }

    @Test
    fun testValidateFileName() {
        assertEquals("abc", PathUtils.validateFileName("abc"))
        assertEquals("中文测试", PathUtils.validateFileName("中文测试"))
        assertEquals("！@……&%#&¥*@#¥*（！——#！!@(#(!$", PathUtils.validateFileName("！@……&%#&¥*@#¥*（！——#！!@(#(!$"))
        assertThrows<ErrorCodeException> { PathUtils.validateFileName("") }
        assertThrows<ErrorCodeException> { PathUtils.validateFileName("   ") }
        assertThrows<ErrorCodeException> { PathUtils.validateFileName("..") }
        assertThrows<ErrorCodeException> { PathUtils.validateFileName(".") }
        assertThrows<ErrorCodeException> { PathUtils.validateFileName("dsjfkjafk/dsajdklsak") }
        assertThrows<ErrorCodeException> { PathUtils.validateFileName(StringPool.randomString(1025)) }

        val illegalString = StringBuilder().append("/a/").append(0.toByte()).toString()
        assertThrows<ErrorCodeException> { PathUtils.validateFileName(illegalString) }
    }

    @Test
    fun testCombineFullPath() {
        assertEquals("/a", PathUtils.combineFullPath("", "a"))
        assertEquals("/a/b", PathUtils.combineFullPath("/a", "b"))
        assertEquals("/a/b", PathUtils.combineFullPath("/a", "/b"))
        assertEquals("/a/b", PathUtils.combineFullPath("/a/", "b"))
        assertEquals("/a/b", PathUtils.combineFullPath("/a/", "/b"))
    }

    @Test
    fun testResolvePath() {
        assertEquals("/", PathUtils.resolveParent(""))
        assertEquals("/", PathUtils.resolveParent("/"))
        assertEquals("/a/", PathUtils.resolveParent("/a/b"))
        assertEquals("/a/", PathUtils.resolveParent("/a/b.txt"))
        assertEquals("/a/b/", PathUtils.resolveParent("/a/b/c/"))
        assertEquals("/", PathUtils.resolveParent("/a"))
    }

    @Test
    fun testResolveName() {
        assertEquals("", PathUtils.resolveName(""))
        assertEquals("", PathUtils.resolveName("/"))
        assertEquals("b", PathUtils.resolveName("/a/b"))
        assertEquals("b.txt", PathUtils.resolveName("/a/b.txt"))
        assertEquals("c", PathUtils.resolveName("/a/b/c/"))
    }

    @Test
    fun testEscapeRegex() {
        assertEquals("""\.\*""", PathUtils.escapeRegex(".*"))
        assertEquals("""/\.\*\|\^/a/""", PathUtils.escapeRegex("/.*|^/a/"))
    }
}
