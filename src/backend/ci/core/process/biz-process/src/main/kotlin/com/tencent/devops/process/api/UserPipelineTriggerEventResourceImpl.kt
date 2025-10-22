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
 *
 */

package com.tencent.devops.process.api

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineTriggerEventResource
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonStatistics
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventVo
import com.tencent.devops.process.trigger.PipelineTriggerEventService

@RestResource
class UserPipelineTriggerEventResourceImpl(
    private val pipelineTriggerEventService: PipelineTriggerEventService
) : UserPipelineTriggerEventResource {

    override fun listTriggerType(
        userId: String,
        scmType: ScmType?
    ): Result<List<IdValue>> {
        return Result(
            PipelineTriggerType.toMap(
                userId = userId,
                scmType = scmType
            )
        )
    }

    override fun listEventType(
        userId: String,
        scmType: ScmType?
    ): Result<List<IdValue>> {
        val eventTypes = CodeEventType.getEventsByScmType(scmType).map {
            IdValue(
                id = it.name,
                value = I18nUtil.getCodeLanMessage(
                    messageCode = "${CodeEventType.MESSAGE_CODE_PREFIX}_${it.name}",
                    defaultMessage = it.name,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        return Result(eventTypes)
    }

    override fun listPipelineTriggerEvent(
        userId: String,
        projectId: String,
        pipelineId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        eventId: Long?,
        reason: PipelineTriggerReason?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEventVo>> {
        return Result(
            pipelineTriggerEventService.listPipelineTriggerEvent(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                eventType = eventType,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventId = eventId,
                reason = reason?.name,
                startTime = startTime,
                endTime = endTime,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listRepoTriggerEvent(
        userId: String,
        projectId: String,
        repoHashId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        pipelineId: String?,
        eventId: Long?,
        pipelineName: String?,
        reason: PipelineTriggerReason?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<RepoTriggerEventVo>> {
        return Result(
            pipelineTriggerEventService.listRepoTriggerEvent(
                projectId = projectId,
                repoHashId = repoHashId,
                triggerType = triggerType,
                eventType = eventType,
                triggerUser = triggerUser,
                pipelineId = pipelineId,
                eventId = eventId,
                reason = reason?.name,
                pipelineName = pipelineName,
                startTime = startTime,
                endTime = endTime,
                page = page,
                pageSize = pageSize,
                userId = userId
            )
        )
    }

    override fun listEventDetail(
        userId: String,
        projectId: String,
        eventId: Long,
        pipelineId: String?,
        pipelineName: String?,
        reason: PipelineTriggerReason?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEventVo>> {
        return Result(
            pipelineTriggerEventService.listRepoTriggerEventDetail(
                projectId = projectId,
                eventId = eventId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                reason = reason?.name,
                page = page,
                pageSize = pageSize,
                userId = userId
            )
        )
    }

    override fun triggerReasonStatistics(
        userId: String,
        projectId: String,
        eventId: Long,
        pipelineId: String?,
        pipelineName: String?
    ): Result<PipelineTriggerReasonStatistics> {
        return Result(
            pipelineTriggerEventService.triggerReasonStatistics(
                projectId = projectId,
                eventId = eventId,
                pipelineId = pipelineId,
                pipelineName = pipelineName
            )
        )
    }

    override fun replay(
        userId: String,
        projectId: String,
        detailId: Long
    ): Result<Boolean> {
        return Result(
            pipelineTriggerEventService.replay(
                userId = userId,
                projectId = projectId,
                detailId = detailId
            )
        )
    }

    override fun replayAll(
        userId: String,
        projectId: String,
        eventId: Long
    ): Result<Boolean> {
        return Result(
            pipelineTriggerEventService.replayAll(
                userId = userId,
                projectId = projectId,
                eventId = eventId
            )
        )
    }
}
