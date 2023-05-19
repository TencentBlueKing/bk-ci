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

import com.tencent.devops.common.webhook.pojo.code.MATCH_PATHS
import org.slf4j.LoggerFactory

abstract class BasePathFilter(
    private val pipelineId: String,
    private val triggerOnPath: List<String>,
    private val includedPaths: List<String>,
    private val excludedPaths: List<String>,
    private val caseSensitive: Boolean = true
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info(
            "$pipelineId|triggerOnPath:$triggerOnPath|includedPaths:$includedPaths" +
                "|excludedPaths:$excludedPaths|caseSensitive:$caseSensitive|path prefix filter"
        )
        return hasNoPathSpecs() || hashPathSpecs(response)
    }

    private fun hasNoPathSpecs(): Boolean {
        return includedPaths.isEmpty() && excludedPaths.isEmpty()
    }

    @SuppressWarnings("NestedBlockDepth", "ReturnCount", "ComplexMethod")
    private fun hashPathSpecs(response: WebhookFilterResponse): Boolean {
        val matchIncludePaths = mutableSetOf<String>()
        if (includedPaths.isNotEmpty()) {
            val matchUserPaths = mutableSetOf<String>()
            triggerOnPath.forEach eventPath@{ eventPath ->
                includedPaths.forEach userPath@{ userPath ->
                    if (isPathMatch(eventPath, userPath)) {
                        matchIncludePaths.add(eventPath)
                        matchUserPaths.add(userPath)
                        return@eventPath
                    }
                }
            }
            if (matchUserPaths.isNotEmpty()) {
                response.addParam(MATCH_PATHS, matchUserPaths.joinToString(","))
            }
        } else {
            matchIncludePaths.addAll(triggerOnPath)
        }

        val matchExcludedPaths = mutableSetOf<String>()
        if (excludedPaths.isNotEmpty()) {
            matchIncludePaths.forEach eventPath@{ eventPath ->
                excludedPaths.forEach userPath@{ userPath ->
                    if (isPathMatch(eventPath, userPath)) {
                        matchExcludedPaths.add(eventPath)
                        return@eventPath
                    }
                }
            }
        }

        // 1. 包含不为空，过滤为空,判断matchIncludePaths是否为空
        // 2. 包含为空，过滤不为空,matchIncludePaths与triggerOnPath相同,判断matchIncludePaths与matchExcludedPaths大小
        return when {
            excludedPaths.isEmpty() && includedPaths.isNotEmpty() && matchIncludePaths.isEmpty() -> false
            matchIncludePaths.size == matchExcludedPaths.size -> false
            else -> true
        }
    }

    abstract fun isPathMatch(eventPath: String, userPath: String): Boolean
}
