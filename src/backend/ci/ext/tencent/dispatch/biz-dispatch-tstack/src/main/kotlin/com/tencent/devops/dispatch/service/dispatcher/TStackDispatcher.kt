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
import com.tencent.devops.common.pipeline.type.tstack.TStackDispatchType
import com.tencent.devops.dispatch.service.TstackBuildService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TStackDispatcher @Autowired constructor(
    private val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val tstackBuildService: TstackBuildService
) : Dispatcher {
    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is TStackDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        val startSuccess = tstackBuildService.startTstackBuild(pipelineAgentStartupEvent)
        if (!startSuccess) {
            logger.warn("Start tstack build failed 0 $pipelineAgentStartupEvent, retry")
            retry(client, buildLogPrinter, pipelineEventDispatcher, pipelineAgentStartupEvent)
        }
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        tstackBuildService.finishTstackBuild(
            pipelineAgentShutdownEvent.buildId,
            pipelineAgentShutdownEvent.vmSeqId,
            pipelineAgentShutdownEvent.buildResult
        )
    }

//    override fun canDispatch(buildMessage: PipelineBuildMessage) =
//        buildMessage.dispatchType.buildType == BuildType.TSTACK
//
//    override fun build(buildMessage: PipelineBuildMessage) {
//        val startSuccess = tstackBuildService.startTstackBuild(buildMessage)
//        if (!startSuccess) {
//            logger.warn("Start tstack build failed 0 $buildMessage, retry")
//            retry(rabbitTemplate, buildMessage)
//        }
//    }
//
//    override fun finish(buildFinishMessage: PipelineFinishMessage) {
//        tstackBuildService.finishTstackBuild(
//            buildFinishMessage.buildId,
//            buildFinishMessage.vmSeqId,
//            buildFinishMessage.buildResult
//        )
//    }

    companion object {
        private val logger = LoggerFactory.getLogger(TStackDispatcher::class.java)
    }
}
