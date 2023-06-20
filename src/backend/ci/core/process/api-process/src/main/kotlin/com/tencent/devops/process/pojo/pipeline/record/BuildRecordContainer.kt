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

package com.tencent.devops.process.pojo.pipeline.record

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.process.pojo.app.StartBuildContext
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@Suppress("LongParameterList", "LongMethod")
@ApiModel("构建详情记录-插件任务")
data class BuildRecordContainer(
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("编排版本号", required = true)
    val resourceVersion: Int,
    @ApiModelProperty("步骤ID", required = true)
    val stageId: String,
    @ApiModelProperty("作业容器ID", required = true)
    val containerId: String,
    @ApiModelProperty("执行次数", required = true)
    val executeCount: Int,
    @ApiModelProperty("执行变量", required = true)
    val containerVar: MutableMap<String, Any>,
    @ApiModelProperty("作业容器类型", required = true)
    val containerType: String,
    @ApiModelProperty("构建状态", required = false)
    var status: String? = null,
    @ApiModelProperty("是否为构建矩阵组", required = false)
    val containPostTaskFlag: Boolean? = null,
    @ApiModelProperty("是否为构建矩阵组", required = false)
    val matrixGroupFlag: Boolean? = null,
    @ApiModelProperty("所在矩阵组ID", required = false)
    val matrixGroupId: String? = null,
    @ApiModelProperty("开始时间", required = true)
    var startTime: LocalDateTime? = null,
    @ApiModelProperty("结束时间", required = true)
    var endTime: LocalDateTime? = null,
    @ApiModelProperty("业务时间戳集合", required = true)
    var timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>
) {
    companion object {

        @Suppress("ComplexMethod")
        fun MutableList<BuildRecordContainer>.addRecords(
            stageId: String,
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
                    containerId = container.containerId!!,
                    containerType = container.getClassType(),
                    executeCount = context.executeCount,
                    matrixGroupFlag = container.matrixGroupFlag,
                    status = buildStatus?.name,
                    containerVar = containerVar,
                    timestamps = mapOf()
                )
            )
            if (taskBuildRecords == null) return
            container.elements.forEachIndexed { index, element ->
                taskBuildRecords.add(
                    BuildRecordTask(
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        stageId = stageId,
                        containerId = container.containerId!!,
                        taskId = element.id!!,
                        classType = element.getClassType(),
                        atomCode = element.getTaskAtom(),
                        executeCount = context.executeCount,
                        resourceVersion = context.resourceVersion,
                        taskSeq = index,
                        status = buildStatus?.name,
                        taskVar = mutableMapOf(),
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
