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

package com.tencent.devops.common.webhook.service.code.filter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PathPrefixFilterTest {
    private val response = WebhookFilterResponse()

    @Test
    @SuppressWarnings("LongMethod")
    fun includePaths() {
        var pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = emptyList()
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        // 路径匹配
        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = emptyList()
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = emptyList()
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))
    }

    @Test
    fun excludePaths() {
        var pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa", "bb")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa", "bb")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa", "bb")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        // 路径匹配
        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/", "bb/")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/", "bb/")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/", "bb/")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/", "bb/")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))
    }

    @Test
    @SuppressWarnings("LongMethod")
    fun includeAndExcludePaths() {
        var pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList()
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = listOf("cc")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = listOf("cc")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = listOf("cc")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc.txt"),
            includedPaths = listOf("aa", "bb"),
            excludedPaths = listOf("cc")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        // 路径匹配
        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = listOf("cc/")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = listOf("cc/")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = listOf("cc/")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt", "cc/include.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = listOf("aa/", "bb/", "cc/")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        // 路径嵌套
        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("src/main/ext/include.txt", "doc/b.md", "cc/other.txt"),
            includedPaths = listOf("src/main"),
            excludedPaths = listOf("src/main/ext/", "doc/b.md")
        )
        Assertions.assertFalse(pathPrefixFilter.doFilter(response))

        pathPrefixFilter = PathPrefixFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("src/main/ext/include.txt", "doc/b.md", "cc/other.txt"),
            includedPaths = listOf("src/main", "cc/"),
            excludedPaths = listOf("src/main/ext/", "doc/b.md")
        )
        Assertions.assertTrue(pathPrefixFilter.doFilter(response))
    }
}
