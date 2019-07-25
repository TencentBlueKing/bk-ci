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

package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchVMShutdownTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : IAtomTask<VMBuildContainer> {
    override fun getParamElement(task: PipelineBuildTask): VMBuildContainer {
        return JsonUtil.mapTo(task.taskParams, VMBuildContainer::class.java)
    }

    private val logger = LoggerFactory.getLogger(DispatchVMShutdownTaskAtom::class.java)

    override fun execute(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        runVariables: Map<String, String>
    ): AtomResponse {
        val vmSeqId = task.containerId

        val buildId = task.buildId

        val projectId = task.projectId

        val pipelineId = task.pipelineId

        pipelineEventDispatcher.dispatch(
            PipelineAgentShutdownEvent(
                source = "shutdownVMTaskAtom",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = task.starter,
                buildId = buildId,
                vmSeqId = vmSeqId,
                buildResult = true,
                routeKeySuffix = param.dispatchType?.routeKeySuffix?.routeKeySuffix
            )
        )
        logger.info("[$buildId]|SHUTDOWN_VM|stageId=${task.stageId}|container=${task.containerId}|vmSeqId=$vmSeqId")
        return AtomResponse(BuildStatus.SUCCEED)
    }

    companion object {
        /**
         * 生成构建任务
         */
        fun makePipelineBuildTasks(
            projectId: String,
            pipelineId: String,
            buildId: String,
            stageId: String,
            container: Container,
            containerSeq: Int,
            taskSeq: Int,
            userId: String
        ): List<PipelineBuildTask> {

            val list: MutableList<PipelineBuildTask> = mutableListOf()

            val containerId = container.id!!
            val containerType = container.getClassType()
            val endTaskSeq = VMUtils.genVMSeq(containerSeq, taskSeq - 1)

            // end-1xxx 无后续任务的结束节点
            list.add(
                PipelineBuildTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerType = containerType,
                    taskSeq = endTaskSeq,
                    taskId = VMUtils.genEndPointTaskId(endTaskSeq),
                    taskName = "Wait_Finish_Job#$containerId",
                    taskType = EnvControlTaskType.VM.name,
                    taskAtom = "",
                    status = BuildStatus.QUEUE,
                    taskParams = container.genTaskParams(),
                    executeCount = 1,
                    starter = userId,
                    approver = null,
                    subBuildId = null,
                    additionalOptions = null
                )
            )

            // stopVM-1xxx 停止虚拟机节点
            val stopVMTaskSeq = VMUtils.genVMSeq(containerSeq, taskSeq)
            list.add(
                PipelineBuildTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerType = containerType,
                    taskSeq = stopVMTaskSeq,
                    taskId = VMUtils.genStopVMTaskId(stopVMTaskSeq),
                    taskName = "Clean_Job#$containerId",
                    taskType = EnvControlTaskType.VM.name,
                    taskAtom = AtomUtils.parseAtomBeanName(DispatchVMShutdownTaskAtom::class.java),
                    status = BuildStatus.QUEUE,
                    taskParams = container.genTaskParams(),
                    executeCount = 1,
                    starter = userId,
                    approver = null,
                    subBuildId = null,
                    additionalOptions = null
                )
            )

            return list
        }
    }
}
