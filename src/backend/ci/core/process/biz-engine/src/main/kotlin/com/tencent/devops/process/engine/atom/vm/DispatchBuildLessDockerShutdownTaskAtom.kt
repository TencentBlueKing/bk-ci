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

package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.control.BuildingHeartBeatUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchBuildLessDockerShutdownTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val buildingHeartBeatUtils: BuildingHeartBeatUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : IAtomTask<NormalContainer> {
    override fun getParamElement(task: PipelineBuildTask): NormalContainer {
        return JsonUtil.mapTo(task.taskParams, NormalContainer::class.java)
    }

    private val logger = LoggerFactory.getLogger(DispatchBuildLessDockerShutdownTaskAtom::class.java)

    override fun execute(
        task: PipelineBuildTask,
        param: NormalContainer,
        runVariables: Map<String, String>
    ): AtomResponse {

        val vmSeqId = task.containerId

        val buildId = task.buildId

        val projectId = task.projectId

        val pipelineId = task.pipelineId

        pipelineEventDispatcher.dispatch(
            PipelineBuildLessShutdownDispatchEvent(
                source = "shutdownVMTaskAtom",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = task.starter,
                buildId = buildId,
                vmSeqId = vmSeqId,
                buildResult = true,
                executeCount = task.executeCount
            )
        )

        // 同步Job执行状态
        buildLogPrinter.stopLog(
            buildId = buildId,
            tag = task.containerHashId ?: "",
            jobId = task.containerHashId ?: "",
            executeCount = task.executeCount
        )

        // 关闭心跳
        buildingHeartBeatUtils.dropHeartbeat(buildId, vmSeqId, task.executeCount)

        logger.info("[$buildId]|SHUTDOWN_VM|stageId=${task.stageId}|container=${task.containerId}|vmSeqId=$vmSeqId")
        return AtomResponse(BuildStatus.SUCCEED)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: NormalContainer,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        return if (force) {
            if (task.status.isFinish()) {
                AtomResponse(
                    buildStatus = task.status,
                    errorType = task.errorType,
                    errorCode = task.errorCode,
                    errorMsg = task.errorMsg
                )
            } else { // 强制终止的设置为失败
                logger.warn("[${task.buildId}]|[FORCE_STOP_BUILD_LESS_IN_SHUTDOWN_TASK]")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildLessShutdownDispatchEvent(
                        source = "force_stop_shutdownBuildLess",
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        userId = task.starter,
                        buildId = task.buildId,
                        vmSeqId = task.containerId,
                        buildResult = true,
                        executeCount = task.executeCount
                    )
                )
                defaultFailAtomResponse
            }
        } else {
            AtomResponse(
                buildStatus = task.status,
                errorType = task.errorType,
                errorCode = task.errorCode,
                errorMsg = task.errorMsg
            )
        }
    }
}
