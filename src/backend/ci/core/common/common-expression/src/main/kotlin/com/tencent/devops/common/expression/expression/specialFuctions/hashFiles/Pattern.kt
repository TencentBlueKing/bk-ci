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

package com.tencent.devops.common.expression.expression.specialFuctions.hashFiles

import java.io.File
import java.nio.file.Paths

@Suppress("ComplexCondition")
class Pattern(
    var pattern: String,
    workspace: String
) {
    // 最小搜索路径
    var searchPath: String

    init {
        assertH(pattern.isNotBlank()) { "hash文件路径不能为空" }
        val literalSegments = Paths.get(pattern).map { it.toString() }
        var canSearchAdd = true
        val searchBuilder = StringBuilder()

        // 为工作空间下相对路径
        if (hasRoot(pattern)) {
            throw RuntimeException("'$pattern'为绝对路径，hash函数仅支持workspace下相对路径")
        }
        literalSegments.forEachIndexed { _, x ->
            // 路径中不能包含 . 或 ..
            if (x == "." || x == "..") {
                throw RuntimeException("'$pattern' 路径段中不能包含 '.' 或 '..' ")
            }
            // 寻找最大可以进行寻找的根目录
            if (canSearchAdd && !x.contains("*") && !x.contains("**") && !x.contains("?")) {
                searchBuilder.append(File.separator).append(x)
            } else {
                canSearchAdd = false
            }
        }

        pattern = workspace + File.separator + pattern

        searchPath = workspace + searchBuilder.toString()
    }

    companion object {
        private val isWindows = System.getProperty("os.name").startsWith("Windows", true)

        // 判断当前路径是否包含根路径
        fun hasRoot(pattern: String): Boolean {
            return if (isWindows) {
                pattern.startsWith("\\") || "^[A-Za-z]:".toRegex().containsMatchIn(pattern)
            } else {
                pattern.startsWith("/")
            }
        }

        fun assertH(check: Boolean, lazyMessage: () -> Any) {
            if (!check) {
                throw RuntimeException(lazyMessage().toString())
            }
        }
    }
}
