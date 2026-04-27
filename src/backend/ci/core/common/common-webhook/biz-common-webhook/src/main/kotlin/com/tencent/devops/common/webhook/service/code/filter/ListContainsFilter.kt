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

import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

/**
 * 包含过滤器
 */
class ListContainsFilter(
    private val pipelineId: String,
    // 过滤器名字
    private val filterName: String,
    private val triggerOn: Set<String>,
    private val included: List<String>,
    private val excluded: List<String>,
    private val includeFailedReason: (reasonCode: String) -> String,
    private val excludedFailedReason: (reasonCode: String) -> String,
    // 记录本次匹配到的元素在匹配结果中的key值
    private val includeItemKey: String? = null
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(ListContainsFilter::class.java)
    }

    private val matcher = AntPathMatcher()

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info(
            "$pipelineId|triggerOn:$triggerOn|included:$included|excluded:$excluded|$filterName filter"
        )
        return hasNoBranchSpecs() || (isNotExcluded(response) && isIncluded(response))
    }

    private fun hasNoBranchSpecs(): Boolean {
        return included.isEmpty() && excluded.isEmpty()
    }

    /**
     * 不在排除范围内
     */
    private fun isNotExcluded(response: WebhookFilterResponse): Boolean {
        excluded.forEach { excludePattern ->
            val excludeItem = triggerOn.find { item -> matcher.match(excludePattern, item) }
            if (excludeItem != null) {
                logger.warn(
                    "$pipelineId|the excluded pattern match the trigger items $excludeItem"
                )
                response.failedReason = excludedFailedReason(excludeItem)
                return false
            }
        }
        return true
    }

    /**
     * 在包含范围内
     */
    private fun isIncluded(response: WebhookFilterResponse): Boolean {
        included.forEach { includePattern ->
            val includeItem = triggerOn.find { item -> matcher.match(includePattern, item) }
            if (includeItem != null) {
                logger.warn(
                    "$pipelineId|the included pattern match the trigger items $includeItem"
                )
                // 记录本次匹配到的元素，后续如有需要可填充到流水线变量中
                includeItemKey?.takeIf { it.isNotEmpty() }?.let {
                    response.addParam(it, includeItem)
                }
                return true
            }
        }
        val branchIncluded = included.isEmpty()
        if (!branchIncluded) {
            response.failedReason = includeFailedReason("")
        }
        return branchIncluded
    }
}
