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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineTriggerEventResource
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.webhook.RepoWebhookEvent
import com.tencent.devops.process.service.trigger.PipelineTriggerEventService

@RestResource
class UserPipelineTriggerEventResourceImpl(
    private val pipelineTriggerEventService: PipelineTriggerEventService
) : UserPipelineTriggerEventResource {
    override fun listTriggerEvent(
        userId: String,
        projectId: String,
        pipelineId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEvent>> {
        return Result(
            pipelineTriggerEventService.listTriggerEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                eventType = eventType,
                triggerType = triggerType,
                triggerUser = triggerUser,
                startTime = startTime,
                endTime = endTime,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listRepoWebhookEvent(
        userId: String,
        projectId: String,
        repoHashId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        pipelineId: String?,
        eventId: Long?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<RepoWebhookEvent>> {
        return Result(
            pipelineTriggerEventService.listRepoWebhookEvent(
                projectId = projectId,
                repoHashId = repoHashId,
                triggerType = triggerType,
                eventType = eventType,
                triggerUser = triggerUser,
                pipelineId = pipelineId,
                eventId = eventId,
                startTime = startTime,
                endTime = endTime,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listRepoWebhookEventDetail(
        userId: String,
        projectId: String,
        repoHashId: String,
        eventId: Long,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEvent>> {
        return Result(
            pipelineTriggerEventService.listRepoWebhookEventDetail(
                projectId = projectId,
                repoHashId = repoHashId,
                eventId = eventId,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun replay(userId: String, projectId: String, id: Long): Result<Boolean> {
        return Result(
            pipelineTriggerEventService.replay(
                userId = userId,
                projectId = projectId,
                id = id
            )
        )
    }

    override fun replayAll(
        userId: String,
        projectId: String,
        repoHashId: String,
        eventId: Long
    ): Result<Boolean> {
        return Result(
            pipelineTriggerEventService.replayAll(
                userId = userId,
                projectId = projectId,
                repoHashId = repoHashId,
                eventId = eventId
            )
        )
    }
}
