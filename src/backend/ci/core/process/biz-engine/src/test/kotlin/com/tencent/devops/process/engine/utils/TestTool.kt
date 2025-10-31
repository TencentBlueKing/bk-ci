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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import java.time.LocalDateTime

@Suppress("ALL")
object TestTool {

    const val projectId = "devops1"
    const val buildId = "b-12345678901234567890123456789012"
    const val pipelineId = "p-12345678901234567890123456789012"
    const val stageId = "stage-1"
    const val containerHashId = "c-12345678901234567890123456789012"
    private const val firstContainerIdInt = 1

    fun genVmBuildContainer(
        jobControlOption: JobControlOption? = null,
        vmSeqId: Int = firstContainerIdInt,
        status: BuildStatus = BuildStatus.RUNNING
    ): PipelineBuildContainer {
        val vmContainerType = "vmBuild"
        val startTime = LocalDateTime.now().minusMinutes(10)
        val containerCost = 5
        return PipelineBuildContainer(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = vmSeqId.toString(),
            seq = vmSeqId,
            containerHashId = containerHashId,
            jobId = "job-123",
            containerType = vmContainerType,
            status = status,
            startTime = startTime,
            endTime = null,
            controlOption = PipelineBuildContainerControlOption(
                jobControlOption = jobControlOption ?: JobControlOption(
                    enable = true, timeout = 600, runCondition = JobRunCondition.STAGE_RUNNING
                ),
                mutexGroup = null
            ),
            cost = containerCost,
            containPostTaskFlag = null,
            matrixGroupId = null,
            matrixGroupFlag = null
        )
    }

    fun genTask(
        taskId: String,
        vmContainer: PipelineBuildContainer,
        elementAdditionalOptions: ElementAdditionalOptions? = null,
        taskAtom: String = ""
    ): PipelineBuildTask {
        with(vmContainer) {
            return PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerType = vmContainer.containerType,
                containerId = containerId,
                startTime = startTime?.plusSeconds(vmContainer.cost.toLong()),
                status = status,
                stageId = stageId,
                taskId = taskId,
                taskAtom = taskAtom,
                taskName = "Demo",
                taskParams = mutableMapOf(),
                taskSeq = 1,
                taskType = vmContainer.containerType,
                starter = "user1",
                containerHashId = containerId,
                approver = null,
                subProjectId = null,
                subBuildId = null,
                additionalOptions = elementAdditionalOptions ?: elementAdditionalOptions(),
                stepId = null
            )
        }
    }

    fun elementAdditionalOptions(
        enable: Boolean = true,
        runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
        customVariables: MutableList<NameAndValue>? = null
    ): ElementAdditionalOptions {
        return ElementAdditionalOptions(
            enable = enable,
            continueWhenFailed = false,
            retryWhenFailed = false,
            runCondition = runCondition, customVariables = customVariables,
            retryCount = 0,
            timeout = 100,
            otherTask = null,
            customCondition = null,
            pauseBeforeExec = null,
            subscriptionPauseUser = null
        )
    }
}
