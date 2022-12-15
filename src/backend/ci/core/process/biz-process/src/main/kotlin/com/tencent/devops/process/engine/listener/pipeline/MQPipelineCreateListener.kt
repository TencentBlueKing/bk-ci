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
 */

package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.service.AgentPipelineRefService
import com.tencent.devops.process.engine.service.PipelineAtomStatisticsService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线创建事件
 *
 * @version 1.0
 */
@Suppress("ALL")
@Component
class MQPipelineCreateListener @Autowired constructor(
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineAtomStatisticsService: PipelineAtomStatisticsService,
    private val callBackControl: CallBackControl,
    private val agentPipelineRefService: AgentPipelineRefService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineCreateEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineCreateEvent) {
        val watcher = Watcher(id = "${event.traceId}|CreatePipeline#${event.pipelineId}|${event.userId}")

        watcher.safeAround("callback") {
            callBackControl.pipelineCreateEvent(projectId = event.projectId, pipelineId = event.pipelineId)
        }

        watcher.safeAround("updateAtomPipelineNum") {
            pipelineAtomStatisticsService.updateAtomPipelineNum(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                version = event.version ?: 1
            )
        }

        watcher.safeAround("updateAgentPipelineRef") {
            with(event) {
                agentPipelineRefService.updateAgentPipelineRef(userId, "create_pipeline", projectId, pipelineId)
            }
        }

        watcher.safeAround("addWebhook") {
            pipelineWebhookService.addWebhook(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                version = event.version,
                userId = event.userId
            )
        }

        LogUtils.printCostTimeWE(watcher = watcher)
    }
}
