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

package com.tencent.devops.repository.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.repository.tables.TRepositoryWebhookRequest
import com.tencent.devops.model.repository.tables.records.TRepositoryWebhookRequestRecord
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class RepositoryWebhookRequestDao {
    fun saveWebhookRequest(dslContext: DSLContext, webhookRequest: RepositoryWebhookRequest) {
        return with(TRepositoryWebhookRequest.T_REPOSITORY_WEBHOOK_REQUEST) {
            dslContext.insertInto(
                this,
                REQUEST_ID,
                EXTERNAL_ID,
                REPOSITORY_TYPE,
                EVENT_TYPE,
                TRIGGER_USER,
                EVENT_MESSAGE,
                REQUEST_HEADER,
                REQUEST_PARAM,
                REQUEST_BODY,
                CREATE_TIME
            ).values(
                webhookRequest.requestId,
                webhookRequest.externalId,
                webhookRequest.repositoryType,
                webhookRequest.eventType,
                webhookRequest.triggerUser,
                webhookRequest.eventMessage,
                webhookRequest.requestHeader?.let { JsonUtil.toJson(it) },
                webhookRequest.requestParam?.let { JsonUtil.toJson(it) },
                webhookRequest.requestBody,
                webhookRequest.createTime
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        requestId: String
    ): RepositoryWebhookRequest? {
        val record = with(TRepositoryWebhookRequest.T_REPOSITORY_WEBHOOK_REQUEST) {
            dslContext.selectFrom(this)
                .where(REQUEST_ID.eq(requestId))
                .fetchOne()
        }
        return record?.let { convert(it) }
    }

    fun convert(record: TRepositoryWebhookRequestRecord): RepositoryWebhookRequest {
        return with(record) {
            RepositoryWebhookRequest(
                requestId = requestId,
                externalId = externalId,
                eventType = eventType,
                repositoryType = repositoryType,
                triggerUser = triggerUser,
                eventMessage = eventMessage,
                requestHeader = requestHeader?.let {
                    JsonUtil.to(
                        it,
                        object : TypeReference<Map<String, String>>() {})
                },
                requestParam = requestParam?.let {
                    JsonUtil.to(
                        it,
                        object : TypeReference<Map<String, String>>() {})
                },
                requestBody = requestBody,
                createTime = createTime
            )
        }
    }
}
