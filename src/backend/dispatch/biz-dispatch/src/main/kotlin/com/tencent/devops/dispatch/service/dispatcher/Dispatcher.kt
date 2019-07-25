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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service.dispatcher

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate

interface Dispatcher {

    fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent): Boolean

    fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent)

    fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent)

    fun retry(
        client: Client,
        rabbitTemplate: RabbitTemplate,
        pipelineEventDispatcher: PipelineEventDispatcher,
        event: PipelineAgentStartupEvent,
        errorMessage: String? = null
    ) {
        if (event.retryTime > 3) {
            // 置为失败
            onFailBuild(client, rabbitTemplate, event, errorMessage ?: "Fail to start up after 3 retries")
            return
        }
        event.retryTime += 1
        event.delayMills = 3000
        pipelineEventDispatcher.dispatch(event)
    }

    fun onFailBuild(
        client: Client,
        rabbitTemplate: RabbitTemplate,
        event: PipelineAgentStartupEvent,
        errorMessage: String
    ) {
        LogUtils.addRedLine(rabbitTemplate, event.buildId, errorMessage, "", event.executeCount ?: 1)
        client.get(ServiceBuildResource::class).setVMStatus(
            projectId = event.projectId, pipelineId = event.pipelineId, buildId = event.buildId,
            vmSeqId = event.vmSeqId, status = BuildStatus.FAILED
        )
    }
}
