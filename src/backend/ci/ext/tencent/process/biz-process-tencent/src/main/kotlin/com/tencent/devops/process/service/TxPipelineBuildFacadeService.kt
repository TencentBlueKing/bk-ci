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
package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.trigger.GenericEventStartRequest
import com.tencent.devops.process.pojo.trigger.WeMateStartRequest
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.trigger.market.MarketEventTriggerBuildService
import com.tencent.devops.store.pojo.common.BK_STORE_CREATIVE_STREAM_WEMATE_MESSAGE_REMINDER_TRIGGER
import org.springframework.stereotype.Service

@Service
class TxPipelineBuildFacadeService(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val marketEventTriggerBuildService: MarketEventTriggerBuildService,
    private val pipelineVisibilityService: PipelineVisibilityService
) {

    fun weMateBuildStart(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: WeMateStartRequest
    ): BuildId {
        checkVisibility(userId, projectId, pipelineId)
        return marketEventTriggerBuildService.genericEventTrigger(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            eventCode = BK_STORE_CREATIVE_STREAM_WEMATE_MESSAGE_REMINDER_TRIGGER,
            request = GenericEventStartRequest(
                eventBody = mapOf(
                    WeMateStartRequest::triggerUser.name to request.triggerUser,
                    WeMateStartRequest::message.name to request.message
                ),
                startParams = request.startParams
            )
        )
    }

    fun visibilityManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): BuildManualStartupInfo {
        checkVisibility(userId, projectId, pipelineId)
        return pipelineBuildFacadeService.buildManualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.getRequestChannelCode(),
            checkPermission = false
        )
    }

    private fun checkVisibility(
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        if (!pipelineVisibilityService.hasVisibility(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_USER_NOT_VISIBLE,
                params = arrayOf(userId, pipelineId)
            )
        }
    }
}
