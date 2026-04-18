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
    @get:Schema(title = "失败插件列表")
    val failedElements: List<BuildDetailElementSimple>,
    @get:Schema(title = "提示信息列表")
    val notices: List<String>
)

@Schema(title = "失败插件信息")
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
    @get:Schema(title = "Job下的插件完整信息列表")
    val element: Element
)

fun ModelDetail.toBuildDetailSimple(): BuildDetailSimple {
    val allElements = model.stages.flatMap { stage ->
        stage.expandContainers().flatMap { container ->
            container.elements.map { element ->
                BuildElementEntry(stage = stage, container = container, element = element)
            }
        }
    }
    val failedElements = allElements.filter { it.element.status.isFailureStatus() }
    val activeElements = allElements.filter { it.element.status.isActiveStatus() }
    val notices = mutableListOf("返回为 AI 简化详情，不包含完整 model 字段；如需进一步定位请结合构建日志。")
    if (failedElements.isEmpty() && activeElements.isEmpty()) {
        notices.add("未发现失败中或运行中的插件，可优先参考 stageSummary 判断构建进度。")
    }
    val stageSummary = model.stages.map { stage ->
        val containers = stage.expandContainers()
        val elements = containers.flatMap { it.elements }
        val stageName = stage.name ?: stage.id ?: "unknown-stage"
        "$stageName [${stage.status ?: "UNKNOWN"}] " +
            "containers=${containers.size}, elements=${elements.size}, " +
            "failed=${elements.count { it.status.isFailureStatus() }}, " +
            "active=${elements.count { it.status.isActiveStatus() }}"
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
        totalStageCount = model.stages.size,
        totalContainerCount = model.stages.sumOf { it.expandContainers().size },
        totalElementCount = allElements.size,
        failedElementCount = failedElements.size,
        activeElementCount = activeElements.size,
        stageSummary = stageSummary,
        failedElements = failedElements.map { it.toSimple() },
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
            element = element
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
