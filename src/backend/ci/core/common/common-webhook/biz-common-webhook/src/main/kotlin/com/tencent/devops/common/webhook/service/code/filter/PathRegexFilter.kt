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

import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

class PathRegexFilter(
    private val pipelineId: String,
    private val triggerOnPath: List<String>,
    private val includedPaths: List<String>,
    private val excludedPaths: List<String>,
    private val caseSensitive: Boolean
) : BasePathFilter(
    pipelineId = pipelineId,
    triggerOnPath = triggerOnPath,
    includedPaths = includedPaths,
    excludedPaths = excludedPaths,
    caseSensitive = caseSensitive
) {
    private val matcher = AntPathMatcher()

    override fun isPathMatch(eventPath: String, userPath: String): Boolean {
        matcher.setCaseSensitive(caseSensitive)
        return matcher.match(userPath, eventPath)
    }

    override fun buildFinalIncludePath(
        eventPath: String,
        userPath: String,
        matchPathsMap: MutableMap<String, MutableSet<String>>
    ) {
        val targetSet = if (matchPathsMap[userPath] == null) {
            mutableSetOf()
        } else {
            matchPathsMap[userPath]!!
        }
        targetSet.add(getShortPath(userPath, eventPath))
        matchPathsMap[userPath] = targetSet
    }

    private fun patternIsMatchDir(userPath: String) =
        userPath.endsWith("/*") ||
                userPath.endsWith("/**") ||
                userPath == "**"

    private fun getShortPath(userPath: String, eventPath: String): String {
        // 配置路径中最后可用字符串，可能为通配符字符串，如:dir_**
        val lastValidStr = getLastValidStr(userPath)
        if (!patternIsMatchDir(userPath) || !eventPath.contains("/") || lastValidStr.isNullOrBlank()) {
            return eventPath
        }
        var targetShortPath = eventPath
        // 拆分目录层级
        val eventPathDirArr = eventPath.split("/")
        for (i in eventPathDirArr.indices) {
            // 有效字符匹配
            if (matcher.match(lastValidStr, eventPathDirArr[i])) {
                targetShortPath = eventPathDirArr.subList(0, i + 1).joinToString(separator = "/")
                break
            }
        }
        return targetShortPath
    }

    private fun getLastValidStr(userPath: String): String? {
        val userPathDirArr = userPath.split("/")
        for (i in userPathDirArr.size - 1 downTo 0) {
            if (userPathDirArr[i] != "*" && userPathDirArr[i] != "**") {
                return userPathDirArr[i]
            }
        }
        logger.info("The userPath do not contain available string.")
        return null
    }

    override fun getFinalPath(
        matchPathsMap: MutableMap<String, MutableSet<String>>
    ): Set<String> = when {
        matchPathsMap.isEmpty() -> {
            setOf()
        }
        // 存在**时匹配所有文件，无视其他路径规则，**时输出全路径
        matchPathsMap.containsKey("**") -> {
            matchPathsMap["**"] ?: setOf()
        }
        // 合并所有匹配路径
        else -> {
            val paths = matchPathsMap.map { it.value }
            val targetPath = mutableSetOf<String>()
            paths.forEach { targetPath.addAll(it) }
            targetPath.toSet()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
