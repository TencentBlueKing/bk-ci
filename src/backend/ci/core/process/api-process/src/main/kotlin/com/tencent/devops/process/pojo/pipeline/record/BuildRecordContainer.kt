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

package com.tencent.devops.process.pojo.pipeline.record

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.ElementUtils
import com.tencent.devops.process.pojo.app.StartBuildContext
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Suppress("LongParameterList", "LongMethod")
@Schema(title = "构建详情记录-插件任务")
data class BuildRecordContainer(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "编排版本号", required = true)
    val resourceVersion: Int,
    @get:Schema(title = "步骤ID", required = true)
    val stageId: String,
    @get:Schema(title = "作业容器ID", required = true)
    val containerId: String,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "执行变量", required = true)
    val containerVar: MutableMap<String, Any>,
    @get:Schema(title = "作业容器类型", required = true)
    val containerType: String,
    @get:Schema(title = "构建状态", required = false)
    var status: String? = null,
    @get:Schema(title = "是否为构建矩阵组", required = false)
    val containPostTaskFlag: Boolean? = null,
    @get:Schema(title = "是否为构建矩阵组", required = false)
    val matrixGroupFlag: Boolean? = null,
    @get:Schema(title = "所在矩阵组ID", required = false)
    val matrixGroupId: String? = null,
    @get:Schema(title = "开始时间", required = true)
    var startTime: LocalDateTime? = null,
    @get:Schema(title = "结束时间", required = true)
    var endTime: LocalDateTime? = null,
    @get:Schema(title = "业务时间戳集合", required = true)
    var timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>
) {
    companion object {

        @Suppress("ComplexMethod")
        fun MutableList<BuildRecordContainer>.addRecords(
            stageId: String,
            stageEnableFlag: Boolean,
            container: Container,
            context: StartBuildContext,
            buildStatus: BuildStatus?,
            taskBuildRecords: MutableList<BuildRecordTask>?
        ) {
            val containerVar = mutableMapOf<String, Any>()
            containerVar[Container::name.name] = container.name
            container.containerHashId?.let {
                containerVar[Container::containerHashId.name] = it
            }
            val startVMTaskSeq = container.startVMTaskSeq
            startVMTaskSeq?.let {
                containerVar[Container::startVMTaskSeq.name] = it
            }
            if (container is TriggerContainer) {
                containerVar[container::params.name] = container.params
                container.buildNo?.let {
                    containerVar[container::buildNo.name] = it
                }
                container.templateParams?.let {
                    containerVar[container::templateParams.name] = it
                }
            } else if (container is VMBuildContainer) {
                container.showBuildResource?.let {
                    containerVar[VMBuildContainer::showBuildResource.name] = it
                }
            }
            this.add(
                BuildRecordContainer(
                    projectId = context.projectId,
                    pipelineId = context.pipelineId,
                    resourceVersion = context.resourceVersion,
                    buildId = context.buildId,
                    stageId = stageId,
                    containerId = container.id ?: "",
                    containerType = container.getClassType(),
                    executeCount = context.executeCount,
                    containPostTaskFlag = container.containPostTaskFlag,
                    matrixGroupFlag = container.matrixGroupFlag,
                    status = buildStatus?.name,
                    containerVar = containerVar,
                    timestamps = mapOf()
                )
            )
            if (taskBuildRecords == null || container.matrixGroupFlag == true) return
            container.elements.forEachIndexed { index, element ->
                if (buildStatus == BuildStatus.SKIP && !ElementUtils.getTaskAddFlag(
                        element = element,
                        stageEnableFlag = stageEnableFlag,
                        containerEnableFlag = container.containerEnabled(),
                        originMatrixContainerFlag = container.matrixGroupFlag == true
                    )
                ) {
                    // 不保存跳过的非post任务记录或非质量红线记录
                    return@forEachIndexed
                }
                val taskSeq = if (startVMTaskSeq != null && startVMTaskSeq > 1 && index < startVMTaskSeq - 1) {
                    // 开机任务前的任务的序号需要在index基础上加1
                    index + 1
                } else {
                    // 开机任务后的任务的序号需要在index基础上加2
                    index + 2
                }
                taskBuildRecords.add(
                    BuildRecordTask(
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        stageId = stageId,
                        containerId = container.id ?: "",
                        taskId = element.id ?: "",
                        classType = element.getClassType(),
                        atomCode = element.getAtomCode(),
                        executeCount = context.executeCount,
                        resourceVersion = context.resourceVersion,
                        taskSeq = taskSeq,
                        status = buildStatus?.name,
                        taskVar = element.initTaskVar(),
                        timestamps = mapOf(),
                        elementPostInfo = element.additionalOptions?.elementPostInfo?.takeIf { info ->
                            info.parentElementId != element.id
                        }
                    )
                )
            }
        }
    }
}
