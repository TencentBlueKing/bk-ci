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

import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.utils.AntPathMatcher
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

@Suppress("NestedBlockDepth")
class HashFiles(
    private val workspace: String,
    private val out: ExpressionOutput?
) {

    private val matcher = AntPathMatcher(File.separator)

    fun calculate(ps: List<String>): String {
        val patterns = ps.map { Pattern(it, workspace) }
        // 将searchPath相同的pattern放到一起方便匹配
        val searchPathMap = mutableMapOf<String, MutableList<Pattern>>()
        patterns.forEach { pattern ->
            if (searchPathMap.contains(pattern.searchPath)) {
                searchPathMap[pattern.searchPath]?.add(pattern)
            } else {
                searchPathMap[pattern.searchPath] = mutableListOf(pattern)
            }
        }

        var count = 0
        val result = MessageDigest.getInstance("SHA-256")
        match(searchPathMap).forEach { file ->
            try {
                result.update(calculateHash(File(file)))
                count++
            } catch (e: Exception) {
                throw RuntimeException("calculate $file hash error ${e.message}")
            }
        }

        return if (count == 0) {
            ""
        } else {
            DatatypeConverter.printHexBinary(result.digest()).toLowerCase()
        }
    }

    private fun match(searchPathMap: Map<String, List<Pattern>>): Set<String> {
        val files = mutableSetOf<String>()
        searchPathMap.forEach search@{ (searchPath, patterns) ->
            val path = Paths.get(searchPath)
            if (!Files.isDirectory(path)) {
                patterns.forEach { pattern ->
                    if (matcher.match(pattern.pattern, path.toString())) {
                        val abs = path.toAbsolutePath().toString()
                        files.add(abs)
                        out?.writeDebugLog("pattern: ${pattern.pattern} match file $abs to calculateHash")
                    }
                }
                return@search
            }
            Files.walk(path)
                .filter { Files.isRegularFile(it) }
                .forEach files@{ file ->
                    patterns.forEach { pattern ->
                        if (matcher.match(pattern.pattern, file.toString())) {
                            val abs = file.toAbsolutePath().toString()
                            files.add(abs)
                            out?.writeDebugLog("pattern: ${pattern.pattern} match file $abs to calculateHash")
                        }
                    }
                }
        }
        return files
    }

    private fun calculateHash(file: File): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(file.readBytes())
    }
}
