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
            matchPathsMap[userPath]!!.map {
                TriggerPathDepth(
                    path = it
                )
            }.toMutableSet()
        }
        var targetEventPath = getShortPath(userPath, eventPath)
        // 匹配最后一层目录时，可忽略具体文件
        if (userPath.endsWith("/*") &&
            !userPath.endsWith("/**/*") &&
            targetEventPath.path.contains("/")
        ) {
            targetEventPath = TriggerPathDepth(
                path = targetEventPath.path.substring(0, targetEventPath.path.lastIndexOf("/"))
            )
        }
        targetSet.add(targetEventPath)
        // 按深度进行分组
        val group = targetSet.groupBy { it.depth }
        // 最浅深度
        val shortDepthPathKey = group.keys.toList().sorted()[0]
        matchPathsMap[userPath] = group[shortDepthPathKey]?.map { it.path }?.toMutableSet() ?: mutableSetOf()
    }

    private fun patternIsMatchDir(userPath: String) =
        userPath.endsWith("/*") ||
                userPath.endsWith("/**") ||
                userPath == "**"

    private fun getShortPath(userPath: String, eventPath: String): TriggerPathDepth {
        if (!patternIsMatchDir(userPath)) {
            return TriggerPathDepth(
                path = eventPath
            )
        }
        if (!eventPath.contains("/")) {
            return TriggerPathDepth(
                path = eventPath
            )
        }
        var targetShortPath = eventPath
        var shortPath = eventPath.substring(0, eventPath.lastIndexOf("/"))
        while (matcher.match(userPath, shortPath)) {
            targetShortPath = shortPath
            if (!shortPath.contains("/")) {
                break
            }
            shortPath = shortPath.substring(0, shortPath.lastIndexOf("/"))
        }
        return TriggerPathDepth(
            path = targetShortPath
        )
    }
}

data class TriggerPathDepth(
    val path: String,// 路径信息
    val depth: Int = path.split("/").size// 深度信息
)