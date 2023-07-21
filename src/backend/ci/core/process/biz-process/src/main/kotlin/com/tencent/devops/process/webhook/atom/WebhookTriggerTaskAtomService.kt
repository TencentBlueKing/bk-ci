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
 *
 */

package com.tencent.devops.process.webhook.atom

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookTriggerTaskAtomService @Autowired constructor(
    private val client: Client
) {

    fun saveRepoWebhookRequest(
        matcher: ScmWebhookMatcher,
        request: WebhookRequest,
        eventTime: LocalDateTime,
        scmType: ScmType
    ): Long {
        val repositoryWebhookRequest = RepositoryWebhookRequest(
            externalId = matcher.getExternalId(),
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventMessage = matcher.getMessage() ?: "",
            repositoryType = scmType.name,
            requestHeader = request.headers,
            requestParam = request.queryParams,
            requestBody = request.body,
            createTime = eventTime
        )
        return client.get(ServiceRepositoryWebhookResource::class).saveWebhookRequest(
            repositoryWebhookRequest = repositoryWebhookRequest
        ).data!!
    }

    fun getRepoWebhookRequest(
        requestId: Long
    ): RepositoryWebhookRequest? {
        return client.get(ServiceRepositoryWebhookResource::class).getWebhookRequest(requestId = requestId).data
    }
}
