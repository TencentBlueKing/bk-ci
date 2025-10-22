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

import com.tencent.devops.common.webhook.pojo.code.MATCH_BRANCH
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

class BranchFilter(
    private val pipelineId: String,
    private val triggerOnBranchName: String,
    private val includedBranches: List<String>,
    private val excludedBranches: List<String>,
    // 包含过滤失败原因
    private val includedFailedReason: String = "",
    // 排除过滤失败原因
    private val excludedFailedReason: String = ""
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(BranchFilter::class.java)
    }
    private val matcher = AntPathMatcher()

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info(
            "$pipelineId|triggerOnBranchName:$triggerOnBranchName|includedBranches:$includedBranches" +
                "|excludedBranches:$excludedBranches|branch filter"
        )
        return hasNoBranchSpecs() || (isBranchNotExcluded(response) && isBranchIncluded(response))
    }

    private fun hasNoBranchSpecs(): Boolean {
        return includedBranches.isEmpty() && excludedBranches.isEmpty()
    }

    private fun isBranchNotExcluded(response: WebhookFilterResponse): Boolean {
        excludedBranches.forEach { excludePattern ->
            if (matcher.match(excludePattern, triggerOnBranchName)) {
                logger.warn(
                    "$pipelineId|the excluded branch match the git event branch $excludePattern"
                )
                response.failedReason = excludedFailedReason
                return false
            }
        }
        return true
    }

    private fun isBranchIncluded(response: WebhookFilterResponse): Boolean {
        includedBranches.forEach { includePattern ->
            if (matcher.match(includePattern, triggerOnBranchName)) {
                response.addParam(MATCH_BRANCH, includePattern)
                logger.warn(
                    "$pipelineId|the included branch match the git event branch $includePattern"
                )
                return true
            }
        }
        val branchIncluded = includedBranches.isEmpty()
        if (!branchIncluded) {
            response.failedReason = includedFailedReason
        }
        return branchIncluded
    }
}
