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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import jakarta.xml.bind.Element
import org.springframework.stereotype.Component

/**
 * 生成运行环境操作的插件任务
 */
@Suppress("ALL")
@Component
class VmOperateTaskGenerator {

    companion object {
        const val START_VM_TASK_ATOM = "dispatchVMStartupTaskAtom"
        const val SHUTDOWN_VM_TASK_ATOM = "dispatchVMShutdownTaskAtom"
        const val START_NORMAL_TASK_ATOM = "dispatchBuildLessDockerStartupTaskAtom"
        const val SHUTDOWN_NORMAL_TASK_ATOM = "dispatchBuildLessDockerShutdownTaskAtom"

        fun isVmAtom(task: PipelineBuildTask) = isStartVM(task) || isStopVM(task)

        fun isVmAtom(atomCode: String) = isStartVM(atomCode) || isStopVM(atomCode)

        fun isStartVM(task: PipelineBuildTask) =
            task.taskAtom == START_VM_TASK_ATOM || task.taskAtom == START_NORMAL_TASK_ATOM

        fun isStopVM(task: PipelineBuildTask) =
            task.taskAtom == SHUTDOWN_VM_TASK_ATOM || task.taskAtom == SHUTDOWN_NORMAL_TASK_ATOM

        fun isStartVM(atomCode: String) =
            atomCode.startsWith(START_VM_TASK_ATOM) || atomCode.startsWith(START_NORMAL_TASK_ATOM)

        fun isStopVM(atomCode: String) =
            atomCode.startsWith(SHUTDOWN_VM_TASK_ATOM) || atomCode.startsWith(SHUTDOWN_NORMAL_TASK_ATOM)
    }

    /**
     * 生成编译环境的开机插件任务
     */
    fun makeStartVMContainerTask(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        container: Container,
        taskSeq: Int,
        userId: String,
        executeCount: Int
    ): PipelineBuildTask {

        val taskParams = container.genTaskParams()
        taskParams["elements"] = emptyList<Element>() // elements在此无用，还可能因为过多导致存储溢出问题，清0
        val atomCode: String
        val taskType: String
        val taskName: String
        val taskAtom: String
        var timeout: Long? = null
        var timeoutVar: String? = null
        if (container is VMBuildContainer) {
            val buildType = container.dispatchType?.buildType()?.name ?: BuildType.DOCKER.name
            val baseOS = container.baseOS.name
            atomCode = "$START_VM_TASK_ATOM-$buildType-$baseOS"
            taskType = EnvControlTaskType.VM.name
            taskName = "Prepare_Job#${container.id!!}"
            taskAtom = START_VM_TASK_ATOM
            timeout = container.jobControlOption?.timeout?.toLong()
            timeoutVar = container.jobControlOption?.timeoutVar
        } else {
            atomCode = START_NORMAL_TASK_ATOM
            taskType = EnvControlTaskType.NORMAL.name
            taskName = "Prepare_Job#${container.id!!}(N)"
            taskAtom = START_NORMAL_TASK_ATOM
            if (container is NormalContainer) {
                timeout = container.jobControlOption?.timeout?.toLong()
                timeoutVar = container.jobControlOption?.timeoutVar
            }
        }
        val additionalOptions = ElementAdditionalOptions(
            runCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL,
            timeout = timeout,
            timeoutVar = timeoutVar
        )
        return PipelineBuildTask(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = container.id!!,
            containerHashId = container.containerHashId ?: "",
            containerType = container.getClassType(),
            taskSeq = taskSeq,
            taskId = VMUtils.genStartVMTaskId(container.id!!),
            taskName = taskName,
            taskType = taskType,
            taskAtom = taskAtom,
            status = BuildStatus.QUEUE,
            taskParams = taskParams,
            executeCount = executeCount,
            starter = userId,
            approver = null,
            subProjectId = null,
            subBuildId = null,
            additionalOptions = additionalOptions,
            atomCode = atomCode,
            stepId = null,
            jobId = container.jobId
        )
    }

    /**
     * 生成准备停机插件任务列表
     */
    fun makeShutdownVMContainerTasks(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        container: Container,
        containerSeq: Int,
        taskSeq: Int,
        userId: String,
        executeCount: Int
    ): List<PipelineBuildTask> {

        val list: MutableList<PipelineBuildTask> = mutableListOf()

        val taskType: String
        val taskAtom: String
        if (container is VMBuildContainer) {
            taskType = EnvControlTaskType.VM.name
            taskAtom = SHUTDOWN_VM_TASK_ATOM
        } else {
            taskType = EnvControlTaskType.NORMAL.name
            taskAtom = SHUTDOWN_NORMAL_TASK_ATOM
        }

        val containerId = container.id!!
        val containerType = container.getClassType()
        val endTaskSeq = VMUtils.genVMTaskSeq(containerSeq, taskSeq - 1)

        var taskName = "Wait_Finish_Job#${container.id!!}($taskType)"
        var additionalOptions = container.opts(taskName = taskName, taskSeq = taskSeq)

        // end-1xxx 无后续任务的结束节点
        list.add(
            PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                containerHashId = container.containerHashId ?: "",
                containerType = containerType,
                taskSeq = endTaskSeq,
                taskId = VMUtils.genEndPointTaskId(endTaskSeq),
                taskName = taskName,
                taskType = taskType,
                taskAtom = "",
                status = BuildStatus.QUEUE,
                taskParams = mutableMapOf(),
                executeCount = executeCount,
                starter = userId,
                approver = null,
                subProjectId = null,
                subBuildId = null,
                additionalOptions = additionalOptions,
                atomCode = "$SHUTDOWN_VM_TASK_ATOM-END",
                stepId = null,
                jobId = container.jobId
            )
        )

        // stopVM-1xxx 停止虚拟机节点
        val stopVMTaskSeq = VMUtils.genVMTaskSeq(containerSeq, taskSeq)
        val taskParams = container.genTaskParams()
        taskParams["elements"] = emptyList<Element>() // elements可能过多导致存储问题
        taskName = "Clean_Job#$containerId($taskType)"
        additionalOptions = container.opts(taskName = taskName, taskSeq = taskSeq)

        list.add(
            PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                containerHashId = container.containerHashId ?: "",
                containerType = containerType,
                taskSeq = stopVMTaskSeq,
                taskId = VMUtils.genStopVMTaskId(stopVMTaskSeq),
                taskName = taskName,
                taskType = taskType,
                taskAtom = taskAtom,
                status = BuildStatus.QUEUE,
                taskParams = taskParams,
                executeCount = executeCount,
                starter = userId,
                approver = null,
                subProjectId = null,
                subBuildId = null,
                additionalOptions = additionalOptions,
                atomCode = "$SHUTDOWN_VM_TASK_ATOM-FINISH",
                stepId = null,
                jobId = container.jobId
            )
        )

        return list
    }

    private fun Container.opts(taskName: String, taskSeq: Int) = ElementAdditionalOptions(
        continueWhenFailed = true,
        timeout = 1, // 1分钟超时
        runCondition = RunCondition.PARENT_TASK_FINISH,
        elementPostInfo = ElementPostInfo(
            parentElementId = VMUtils.genStartVMTaskId(id!!),
            postCondition = "",
            postEntryParam = "",
            parentElementName = taskName,
            parentElementJobIndex = taskSeq
        )
    )
}
