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

package com.tencent.devops.common.webhook.service.code.filter

import org.springframework.util.AntPathMatcher

class PathRegexFilter(
    private val pipelineId: String,
    private val triggerOnPath: List<String>,
    private val includedPaths: List<String>,
    private val excludedPaths: List<String>,
    // 包含过滤失败原因
    private val includedFailedReason: String = "",
    // 排除过滤失败原因
    private val excludedFailedReason: String = "",
    private val caseSensitive: Boolean
) : BasePathFilter(
    pipelineId = pipelineId,
    triggerOnPath = triggerOnPath,
    includedPaths = includedPaths,
    excludedPaths = excludedPaths,
    includedFailedReason = includedFailedReason,
    excludedFailedReason = excludedFailedReason,
    caseSensitive = caseSensitive
) {
    private val matcher = AntPathMatcher()

    override fun isPathMatch(eventPath: String, userPath: String): Boolean {
        matcher.setCaseSensitive(caseSensitive)
        return matcher.match(userPath, eventPath)
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    override fun extractMatchUserPath(
        eventPath: String,
        userPath: String
    ): String {
        val patternParts = userPath.split("/")
        // 无视规则，直接返回空
        if (isInvalidPattern(userPath) || isInvalidPattern(patternParts)) {
            return ""
        }
        val pathParts = eventPath.split("/")
        val pathList = mutableListOf<String>()
        var pathIndex = 0
        var segment = 0
        while (segment < patternParts.size && pathIndex < pathParts.size) {
            val patternPart = patternParts[segment]
            // 无效字符
            if (isInvalidPattern(patternPart)) {
                // 以*或**结尾的规则，直接结束
                if (segment == patternParts.size - 1) {
                    break
                }
                val nextPatternPart = patternParts[segment + 1]
                // 后续不存在有效字符继续找
                if (isInvalidPattern(nextPatternPart)) {
                    segment++
                    continue
                }
                while (pathIndex < pathParts.size && !matcher.match(nextPatternPart, pathParts[pathIndex])) {
                    val pathItem = pathParts[pathIndex++]
                    pathList.add(pathItem)
                }
                // 追加上匹配上的真实目录[nextPatternPart可能为dir_**]
                if (matcher.match(nextPatternPart, pathParts[pathIndex])) {
                    pathList.add(pathParts[pathIndex])
                }
                segment++
            } else {
                val pathItem = pathParts[pathIndex]
                pathList.add(pathItem)
            }
            pathIndex++
            segment++
        }
        return pathList.joinToString("/")
    }

    private fun isInvalidPattern(pattern: String) = (pattern == "*" || pattern == "**")

    private fun isInvalidPattern(patterns: List<String>): Boolean {
        patterns.toSet().forEach {
            if (!isInvalidPattern(it)) {
                return false
            }
        }
        return true
    }
}
