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

package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.process.engine.pojo.event.PipelineRestoreEvent
import com.tencent.devops.process.engine.service.AgentPipelineRefService
import com.tencent.devops.process.engine.service.PipelineAtomStatisticsService
import com.tencent.devops.process.engine.service.RepoPipelineRefService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线恢复事件
 *
 * @version 1.0
 */
@Component
class MQPipelineRestoreListener @Autowired constructor(
    private val agentPipelineRefService: AgentPipelineRefService,
    private val pipelineAtomStatisticsService: PipelineAtomStatisticsService,
    private val callBackControl: CallBackControl,
    private val repoPipelineRefService: RepoPipelineRefService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineRestoreEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineRestoreEvent) {
        val watcher = Watcher(id = "${event.traceId}|RestorePipeline#${event.pipelineId}|${event.userId}")
        try {
            watcher.start("updateAgentPipelineRef")
            with(event) {
                agentPipelineRefService.updateAgentPipelineRef(userId, "restore_pipeline", projectId, pipelineId)
            }
            watcher.stop()
            watcher.start("updateAtomPipelineNum")
            pipelineAtomStatisticsService.updateAtomPipelineNum(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                version = event.version,
                deleteFlag = false,
                restoreFlag = true
            )
            watcher.stop()
            watcher.start("callback")
            callBackControl.pipelineRestoreEvent(projectId = event.projectId, pipelineId = event.pipelineId)
            with(event) {
                repoPipelineRefService.updateRepoPipelineRef(userId, "restore_pipeline", projectId, pipelineId)
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }
}
