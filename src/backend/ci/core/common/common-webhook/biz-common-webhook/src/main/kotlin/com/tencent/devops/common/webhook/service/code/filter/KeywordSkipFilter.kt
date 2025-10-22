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

class KeywordSkipFilter(
    private val pipelineId: String,
    private val keyWord: List<String>,
    private val enable: Boolean? = true,
    private val triggerOnMessage: String?,
    private val failedReason: String = ""
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(KeywordSkipFilter::class.java)
        val KEYWORD_SKIP_CI = listOf("[skip ci]")
        val KEYWORD_SKIP_WIP = listOf("[WIP]", "WIP")
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info("$pipelineId|triggerOnMessage:$triggerOnMessage|skipWord:$keyWord|enable:$enable")
        return when {
            enable == false -> true
            keyWord.any { triggerOnMessage?.contains(it) == true } -> {
                response.failedReason = failedReason
                false
            }

            else -> true
        }
    }
}
