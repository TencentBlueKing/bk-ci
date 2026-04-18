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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建简化详情")
data class BuildDetailSimple(
    @get:Schema(title = "构建ID")
    val id: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "构建发起人")
    val userId: String,
    @get:Schema(title = "触发用户")
    val triggerUser: String? = null,
    @get:Schema(title = "触发方式")
    val trigger: String,
    @get:Schema(title = "开始时间")
    val startTime: Long,
    @get:Schema(title = "结束时间")
    val endTime: Long?,
    @get:Schema(title = "构建状态")
    val status: String,
    @get:Schema(title = "当前服务器时间戳")
    val currentTimestamp: Long,
    @get:Schema(title = "构建号")
    val buildNum: Int,
    @get:Schema(title = "取消用户ID")
    val cancelUserId: String?,
    @get:Schema(title = "当前版本")
    val curVersion: Int,
    @get:Schema(title = "最新版本")
    val latestVersion: Int,
    @get:Schema(title = "最新构建号")
    val latestBuildNum: Int,
    @get:Schema(title = "最后修改人")
    val lastModifyUser: String?,
    @get:Schema(title = "执行耗时")
    val executeTime: Long = 0,
    @get:Schema(title = "触发审核人列表")
    val triggerReviewers: List<String>? = null,
    @get:Schema(title = "是否为调试构建")
    val debug: Boolean? = false,
    @get:Schema(title = "阶段总数")
    val totalStageCount: Int,
    @get:Schema(title = "Job总数")
    val totalContainerCount: Int,
    @get:Schema(title = "插件总数")
    val totalElementCount: Int,
    @get:Schema(title = "失败插件数")
    val failedElementCount: Int,
    @get:Schema(title = "活动中插件数")
    val activeElementCount: Int,
    @get:Schema(title = "阶段摘要列表")
    val stageSummary: List<String>,
    @get:Schema(title = "阶段简化信息列表")
    val stages: List<BuildDetailStageSimple>,
    @get:Schema(title = "Job简化信息列表")
    val containers: List<BuildDetailContainerSimple>,
    @get:Schema(title = "失败插件列表")
    val failedElements: List<BuildDetailElementSimple>,
    @get:Schema(title = "活动中插件列表")
    val activeElements: List<BuildDetailElementSimple>,
    @get:Schema(title = "插件预览列表")
    val elementPreview: List<BuildDetailElementSimple>,
    @get:Schema(title = "提示信息列表")
    val notices: List<String>
)

@Schema(title = "阶段简化信息")
data class BuildDetailStageSimple(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?,
    @get:Schema(title = "阶段展示ID")
    val stageIdForUser: String?,
    @get:Schema(title = "阶段状态")
    val status: String?,
    @get:Schema(title = "是否为最终阶段")
    val finalStage: Boolean,
    @get:Schema(title = "Job数量")
    val containerCount: Int,
    @get:Schema(title = "插件数量")
    val elementCount: Int,
    @get:Schema(title = "失败插件数")
    val failedElementCount: Int,
    @get:Schema(title = "活动中插件数")
    val activeElementCount: Int
)

@Schema(title = "Job简化信息")
data class BuildDetailContainerSimple(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?,
    @get:Schema(title = "Job ID")
    val containerId: String?,
    @get:Schema(title = "Job名称")
    val containerName: String,
    @get:Schema(title = "Job状态")
    val status: String?,
    @get:Schema(title = "Job哈希ID")
    val containerHashId: String?,
    @get:Schema(title = "关联jobId")
    val jobId: String?,
    @get:Schema(title = "启动虚拟机状态")
    val startVmStatus: String?,
    @get:Schema(title = "是否为矩阵分组")
    val matrixGroupFlag: Boolean?,
    @get:Schema(title = "插件数量")
    val elementCount: Int,
    @get:Schema(title = "失败插件数")
    val failedElementCount: Int,
    @get:Schema(title = "活动中插件数")
    val activeElementCount: Int
)

@Schema(title = "插件简化信息")
data class BuildDetailElementSimple(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?,
    @get:Schema(title = "Job ID")
    val containerId: String?,
    @get:Schema(title = "Job名称")
    val containerName: String,
    @get:Schema(title = "Job哈希ID")
    val containerHashId: String?,
    @get:Schema(title = "关联jobId")
    val jobId: String?,
    @get:Schema(title = "插件ID")
    val elementId: String?,
    @get:Schema(title = "插件名称")
    val elementName: String,
    @get:Schema(title = "步骤ID")
    val stepId: String?,
    @get:Schema(title = "插件状态")
    val status: String?,
    @get:Schema(title = "插件类型")
    val classType: String,
    @get:Schema(title = "原子代码")
    val atomCode: String,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "错误类型")
    val errorType: String?,
    @get:Schema(title = "错误码")
    val errorCode: Int?,
    @get:Schema(title = "错误信息")
    val errorMsg: String?
)

fun ModelDetail.toBuildDetailSimple(): BuildDetailSimple {
    val stageDetails = model.stages.map { stage ->
        val containers = stage.expandContainers()
        val entries = containers.flatMap { container ->
            container.elements.map { element ->
                BuildElementEntry(stage = stage, container = container, element = element)
            }
        }
        BuildDetailStageSimple(
            stageId = stage.id,
            stageName = stage.name,
            stageIdForUser = stage.stageIdForUser,
            status = stage.status,
            finalStage = stage.finally,
            containerCount = containers.size,
            elementCount = entries.size,
            failedElementCount = entries.count { it.element.status.isFailureStatus() },
            activeElementCount = entries.count { it.element.status.isActiveStatus() }
        )
    }
    val containerDetails = model.stages.flatMap { stage ->
        stage.expandContainers().map { container ->
            val failedCount = container.elements.count { it.status.isFailureStatus() }
            val activeCount = container.elements.count { it.status.isActiveStatus() }
            BuildDetailContainerSimple(
                stageId = stage.id,
                stageName = stage.name,
                containerId = container.id,
                containerName = container.name,
                status = container.status,
                containerHashId = container.containerHashId,
                jobId = container.jobId,
                startVmStatus = container.startVMStatus,
                matrixGroupFlag = container.matrixGroupFlag,
                elementCount = container.elements.size,
                failedElementCount = failedCount,
                activeElementCount = activeCount
            )
        }
    }
    val allElements = model.stages.flatMap { stage ->
        stage.expandContainers().flatMap { container ->
            container.elements.map { element ->
                BuildElementEntry(stage = stage, container = container, element = element)
            }
        }
    }
    val failedElements = allElements.filter { it.element.status.isFailureStatus() }
    val activeElements = allElements.filter { it.element.status.isActiveStatus() }
    val notices = mutableListOf(
        "返回为 AI 简化详情，不包含完整 model 字段；如需进一步定位请结合构建日志。"
    )
    if (failedElements.isEmpty() && activeElements.isEmpty()) {
        notices.add("未发现失败中或运行中的插件，可优先参考 stageSummary 判断构建进度。")
    }
    val stageSummary = stageDetails.map { stage ->
        val stageName = stage.stageName ?: stage.stageId ?: "unknown-stage"
        "$stageName [${stage.status ?: "UNKNOWN"}] " +
            "containers=${stage.containerCount}, elements=${stage.elementCount}, " +
            "failed=${stage.failedElementCount}, active=${stage.activeElementCount}"
    }
    return BuildDetailSimple(
        id = id,
        pipelineId = pipelineId,
        pipelineName = pipelineName,
        userId = userId,
        triggerUser = triggerUser,
        trigger = trigger,
        startTime = startTime,
        endTime = endTime,
        status = status,
        currentTimestamp = currentTimestamp,
        buildNum = buildNum,
        cancelUserId = cancelUserId,
        curVersion = curVersion,
        latestVersion = latestVersion,
        latestBuildNum = latestBuildNum,
        lastModifyUser = lastModifyUser,
        executeTime = executeTime,
        triggerReviewers = triggerReviewers,
        debug = debug,
        totalStageCount = stageDetails.size,
        totalContainerCount = containerDetails.size,
        totalElementCount = allElements.size,
        failedElementCount = failedElements.size,
        activeElementCount = activeElements.size,
        stageSummary = stageSummary,
        stages = stageDetails,
        containers = containerDetails,
        failedElements = failedElements.map { it.toSimple() },
        activeElements = activeElements.map { it.toSimple() },
        elementPreview = allElements.map { it.toSimple() },
        notices = notices
    )
}

private data class BuildElementEntry(
    val stage: Stage,
    val container: Container,
    val element: Element
) {
    fun toSimple(): BuildDetailElementSimple {
        return BuildDetailElementSimple(
            stageId = stage.id,
            stageName = stage.name,
            containerId = container.id,
            containerName = container.name,
            containerHashId = container.containerHashId,
            jobId = container.jobId,
            elementId = element.id,
            elementName = element.name,
            stepId = element.stepId,
            status = element.status,
            classType = element.getClassType(),
            atomCode = element.getAtomCode(),
            enabled = element.elementEnabled(),
            errorType = element.errorType,
            errorCode = element.errorCode,
            errorMsg = element.errorMsg
        )
    }
}

private fun Stage.expandContainers(): List<Container> {
    return containers.flatMap { container ->
        listOf(container) + (container.fetchGroupContainers() ?: emptyList())
    }
}

private fun String?.isFailureStatus(): Boolean {
    val buildStatus = toBuildStatus() ?: return false
    return buildStatus.isFailure()
}

private fun String?.isActiveStatus(): Boolean {
    val buildStatus = toBuildStatus() ?: return false
    return !buildStatus.isFinish() && !buildStatus.isNeverRun()
}

private fun String?.toBuildStatus(): BuildStatus? {
    if (this.isNullOrBlank()) {
        return null
    }
    return runCatching { BuildStatus.valueOf(this) }.getOrNull()
}
