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

class PathRegexFilterTest {

    private val response = WebhookFilterResponse()

    @Test
    @SuppressWarnings("LongMethod")
    fun includePaths() {
        var pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa*", "bb"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        // 路径匹配
        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa/", "bb/"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))
    }

    @Test
    fun excludePaths() {
        var pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa*", "bb*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa*", "bb*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa*", "bb*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        // 路径匹配
        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/*", "bb/*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/*", "bb/*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/*", "bb/*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc/include.txt"),
            includedPaths = emptyList(),
            excludedPaths = listOf("aa/*", "bb/*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))
    }

    @Test
    @SuppressWarnings("LongMethod")
    fun includeAndExcludePaths() {
        var pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = emptyList(),
            excludedPaths = emptyList(),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = listOf("cc"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = listOf("cc"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt", "bb.txt", "cc.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = listOf("cc*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("cc.txt"),
            includedPaths = listOf("aa*", "bb*"),
            excludedPaths = listOf("cc*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        // 路径匹配
        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = listOf("cc/*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = listOf("cc/*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = listOf("cc/*"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("aa/include.txt", "bb/include.txt", "cc/include.txt"),
            includedPaths = listOf("aa/*", "bb/*"),
            excludedPaths = listOf("aa/*", "bb/*", "cc/*"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        // 路径嵌套
        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("src/main/ext/include.txt", "doc/b.md", "cc/other.txt"),
            includedPaths = listOf("src/main/**"),
            excludedPaths = listOf("src/main/ext/**", "doc/b.md"),
            caseSensitive = true
        )
        Assertions.assertFalse(pathRegexFilter.doFilter(response))

        pathRegexFilter = PathRegexFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnPath = listOf("src/main/ext/include.txt", "doc/b.md", "cc/other.txt"),
            includedPaths = listOf("src/main/**", "cc/**"),
            excludedPaths = listOf("src/main/ext/**", "doc/b.md"),
            caseSensitive = true
        )
        Assertions.assertTrue(pathRegexFilter.doFilter(response))
    }
}
