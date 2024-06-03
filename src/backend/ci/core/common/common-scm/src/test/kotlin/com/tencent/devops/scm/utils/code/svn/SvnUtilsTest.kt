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

package com.tencent.devops.scm.utils.code.svn

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SvnUtilsTest {

    @Test
    fun getSvnFilePath() {
        var url = "http://svn.example.com/demo/trunk/aaa"
        var filePath = "/trunk/aaa/test.java"
        var expected = SvnUtils.getSvnFilePath(
            url = url,
            filePath = filePath
        )
        Assertions.assertEquals(expected, "/test.java")

        filePath = "/trunk/bbb/test.java"
        expected = SvnUtils.getSvnFilePath(
            url = url,
            filePath = filePath
        )
        Assertions.assertEquals(expected, "trunk/bbb/test.java")

        url = "http://svn.example.com/demo/"
        filePath = "/trunk/aaa/test.java"
        expected = SvnUtils.getSvnFilePath(
            url = url,
            filePath = filePath
        )
        Assertions.assertEquals(expected, "trunk/aaa/test.java")

        url = "http://svn.example.com/demo/trunk/aaa/bbb"
        filePath = "/trunk/aaa/test.java"
        expected = SvnUtils.getSvnFilePath(
            url = url,
            filePath = filePath
        )
        Assertions.assertEquals(expected, "trunk/aaa/test.java")
    }

    @Test
    fun getSvnProjectName() {
        var url = "svn+ssh://abcd@abcd-svn.abcd.com/code_python/test_project_proj/branches/dir_1/dir_2"
        var targetProjectName = "code_python/test_project_proj"
        var result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)

        url = "http://abcd-svn.abcd.com/code_java/java_project_proj/trunk"
        targetProjectName = "code_java/java_project_proj"
        result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)

        url = "svn+ssh://abcdefg@abcdefg-svn.abcdefg.com/code_vue/vue_front/branches/dir_1/dir_2"
        targetProjectName = "code_vue/vue_front"
        result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)

        url = "svn+ssh://xyz-xyz.svn.cn/code_js/jquery/jquery_proj/branches/2.design/1.word"
        targetProjectName = "code_js/jquery/jquery_proj"
        result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)

        url = "svn+ssh://svn-xyz.com/code_c/c_lib/base_lib_proj/branches/test"
        targetProjectName = "code_c/c_lib/base_lib_proj"
        result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)

        url = "http://svn-xyz.com/code_kotlin/k_code/branches/test"
        targetProjectName = "code_kotlin/k_code"
        result = SvnUtils.getSvnProjectName(url)
        Assertions.assertEquals(result, targetProjectName)
    }
}
