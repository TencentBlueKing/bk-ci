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

package com.tencent.devops.common.webhook.service.code.matcher

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.p4.P4Event
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory

class P4WebHookMatcher(
    override val event: P4Event
) : AbstractScmWebhookMatcher<P4Event>(event) {

    companion object {
        private val logger = LoggerFactory.getLogger(P4WebHookMatcher::class.java)
    }

    override fun getEventType(): CodeEventType {
        return eventHandler.getEventType(event) ?: super.getEventType()
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): WebhookMatchResult {
        if (repository !is CodeP4Repository) {
            logger.warn("$pipelineId|The repo($repository) is not code p4 repo for p4 web hook")
            return WebhookMatchResult(isMatch = false)
        }
        return eventHandler.isMatch(
            event = event,
            projectId = projectId,
            pipelineId = pipelineId,
            repository = repository,
            webHookParams = webHookParams
        )
    }

    override fun getCodeType() = CodeType.P4
}
