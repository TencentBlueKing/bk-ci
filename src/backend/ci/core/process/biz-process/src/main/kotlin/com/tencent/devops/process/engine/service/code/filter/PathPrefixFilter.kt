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

package com.tencent.devops.process.engine.service.code.filter

import org.slf4j.LoggerFactory

class PathPrefixFilter(
    private val pipelineId: String,
    private val triggerOnPath: List<String>,
    private val includedPaths: List<String>,
    private val excludedPaths: List<String>
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(PathPrefixFilter::class.java)
        private const val MATCH_PATHS = "matchPaths"
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        return hasNoBranchSpecs() || (isPathNotExcluded() && isPathIncluded(response))
    }

    private fun hasNoBranchSpecs(): Boolean {
        return includedPaths.isEmpty() && excludedPaths.isEmpty()
    }

    private fun isPathNotExcluded(): Boolean {
        triggerOnPath.forEach eventPath@{ eventPath ->
            excludedPaths.forEach userPath@{ userPath ->
                if (isPathMatch(eventPath, userPath)) {
                    return@eventPath
                }
            }
            logger.warn("$pipelineId|$eventPath|Event path not match the user path")
            return true
        }
        return false
    }

    private fun isPathIncluded(response: WebhookFilterResponse): Boolean {
        val matchPaths = mutableSetOf<String>()
        triggerOnPath.forEach eventPath@{ eventPath ->
            includedPaths.forEach userPath@{ userPath ->
                if (isPathMatch(eventPath, userPath)) {
                    matchPaths.add(userPath)
                }
            }
        }
        logger.warn("$pipelineId|$matchPaths|Event path match the user path")
        return if (matchPaths.isNotEmpty()) {
            response.addParam(MATCH_PATHS, matchPaths.joinToString(","))
            true
        } else {
            false
        }
    }

    private fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        return fullPath.removePrefix("/").startsWith(prefixPath.removePrefix("/"))
    }
}
