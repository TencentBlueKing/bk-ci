/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the Software), to deal in the Software without restriction, including without limitation the
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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.EndPosition
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement

/**
 * 从编排模型收集终态位置。取消链路拍下的是当时的中间态（如 PAUSE），
 * 构建真正结束后要以插件当前终态（FAILED / REVIEW_ABORT / CANCELED 等）为准。
 *
 * 词条常量与 process 模块 [ProcessMessageCode] 保持同值，供读取侧直接国际化。
 */
object BuildEndPositionCollector {

    const val REASON_PAUSE_TERMINATED = "bkBuildEndFailPauseTerminated"
    const val REASON_PLUGIN_FAIL = "bkBuildEndFailPlugin"
    const val REASON_MUTEX_QUEUE_DISABLED = "bkBuildEndFailMutexQueueDisabled"
    const val REASON_MUTEX_QUEUE = "bkBuildEndFailMutexQueue"
    const val REASON_JOB_ABORTED = "bkBuildEndFailJobAborted"
    const val REASON_JOB_EXEC_TIMEOUT = "bkBuildCancelSystemJobExecTimeout"

    /** 卡片展示用原因上限：脚本错误原文可能数千字，铺在位置列表里会把整张卡片撑开 */
    const val REASON_DISPLAY_MAX = 200
    const val ERROR_MSG_MAX = 512
    const val POSITION_MAX_SIZE = 50

    private val WHITESPACE = Regex("\\s+")

    /**
     * 压缩空白并截断，供卡片 [EndPosition.reason] 展示。
     * 完整错误仍可走日志，不要把脚本堆到终态卡片上。
     */
    fun toDisplayReason(raw: String?, maxLength: Int = REASON_DISPLAY_MAX): String? {
        val normalized = raw?.replace(WHITESPACE, " ")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (normalized.length <= maxLength) return normalized
        return normalized.take(maxLength) + "..."
    }

    /**
     * 失败/超时类位置：失败插件、审核驳回、执行前暂停被终止、Job 超时插件，
     * 以及没有任何插件错误的互斥组 / Job 超时容器。
     */
    fun collectFailPositions(model: Model): List<EndPosition> {
        return traverseModel(model) { ctx -> collectFailFromContainer(ctx) }
    }

    /**
     * 取消类位置：已被取消/终止/仍停在暂停的用户插件。
     * 未执行（UNEXEC）的后续步骤不进入卡片，避免把整条流水线都列进去。
     */
    fun collectCancelPositions(model: Model): List<EndPosition> {
        return traverseModel(model) { ctx -> collectCancelFromContainer(ctx) }
    }

    fun aggregateFailEndType(positions: List<EndPosition>): BuildEndType {
        val types = positions.mapNotNull { it.endType }
            .filter { it != BuildEndType.FAIL_FAST_KILL }
            .distinct()
        return when {
            types.isEmpty() -> BuildEndType.FAIL_EXEC
            types.size == 1 -> types.first()
            else -> BuildEndType.FAIL_MULTIPLE
        }
    }

    private data class ContainerWalk(
        val positions: MutableList<EndPosition>,
        val stageIndex: Int,
        val stageName: String,
        val stageId: String,
        val container: Container,
        val containerSeq: Int,
        val matrixFlag: Boolean
    )

    private fun traverseModel(
        model: Model,
        collect: (ContainerWalk) -> Unit
    ): List<EndPosition> {
        val positions = mutableListOf<EndPosition>()
        model.stages.forEachIndexed { stageIndex, stage ->
            if (stageIndex == 0) return@forEachIndexed
            val stageId = stage.id ?: return@forEachIndexed
            val stageName = stage.name.orEmpty()
            stage.containers.forEachIndexed { containerIndex, container ->
                collect(
                    ContainerWalk(
                        positions = positions,
                        stageIndex = stageIndex,
                        stageName = stageName,
                        stageId = stageId,
                        container = container,
                        containerSeq = containerIndex + 1,
                        matrixFlag = false
                    )
                )
                container.fetchGroupContainers()?.forEach { matrixContainer ->
                    collect(
                        ContainerWalk(
                            positions = positions,
                            stageIndex = stageIndex,
                            stageName = stageName,
                            stageId = stageId,
                            container = matrixContainer,
                            containerSeq = containerIndex + 1,
                            matrixFlag = true
                        )
                    )
                }
            }
        }
        return positions.take(POSITION_MAX_SIZE)
    }

    private fun collectFailFromContainer(walk: ContainerWalk) {
        val containerId = walk.containerId() ?: return
        val jobPath = "${walk.stageName}/${walk.container.name}"
        val before = walk.positions.size
        walk.container.elements.forEachIndexed { elementIndex, element ->
            if (isControlElement(element)) return@forEachIndexed
            val classified = classifyFailPlugin(element, walk.container) ?: return@forEachIndexed
            walk.positions.add(
                pluginPosition(
                    walk = walk,
                    containerId = containerId,
                    jobPath = jobPath,
                    element = element,
                    elementIndex = elementIndex,
                    endType = classified.endType,
                    reason = classified.reason,
                    reasonCode = classified.reasonCode,
                    reasonParams = classified.reasonParams
                )
            )
        }
        if (walk.positions.size == before) {
            collectJobLevelFail(walk, containerId, jobPath)
        }
    }

    private fun collectCancelFromContainer(walk: ContainerWalk) {
        val containerId = walk.containerId() ?: return
        val jobPath = "${walk.stageName}/${walk.container.name}"
        val before = walk.positions.size
        walk.container.elements.forEachIndexed { elementIndex, element ->
            if (isControlElement(element)) return@forEachIndexed
            val status = BuildStatus.parse(element.status)
            if (!isCancelVisible(status)) return@forEachIndexed
            walk.positions.add(
                pluginPosition(
                    walk = walk,
                    containerId = containerId,
                    jobPath = jobPath,
                    element = element,
                    elementIndex = elementIndex,
                    endType = null,
                    reason = null,
                    reasonCode = if (element.additionalOptions?.pauseBeforeExec == true) {
                        REASON_PAUSE_TERMINATED
                    } else {
                        null
                    },
                    reasonParams = null
                )
            )
        }
        if (walk.positions.size == before) {
            collectJobLevelCancel(walk, containerId, jobPath)
        }
    }

    private fun collectJobLevelFail(
        walk: ContainerWalk,
        containerId: String,
        jobPath: String
    ) {
        val status = BuildStatus.parse(walk.container.status)
        val mutex = walk.container.enabledMutexGroup()
        val timeoutMinutes = walk.container.jobTimeoutMinutes()
        val classified = when {
            status.isTimeout() && timeoutMinutes != null -> ClassifiedEnd(
                endType = BuildEndType.TIMEOUT_JOB,
                reasonCode = REASON_JOB_EXEC_TIMEOUT,
                reasonParams = listOf(timeoutMinutes.toString())
            )
            mutex != null && (status.isFailure() || status.isCancel()) -> mutex.toFailClassified()
            else -> return
        }
        walk.positions.add(
            jobPosition(walk, containerId, jobPath, status, classified)
        )
    }

    private fun collectJobLevelCancel(
        walk: ContainerWalk,
        containerId: String,
        jobPath: String
    ) {
        val status = BuildStatus.parse(walk.container.status)
        val mutex = walk.container.enabledMutexGroup()
        if (!status.isCancel() || mutex == null) return
        walk.positions.add(
            jobPosition(walk, containerId, jobPath, status, mutex.toFailClassified())
        )
    }

    private fun classifyFailPlugin(element: Element, container: Container): ClassifiedEnd? {
        val status = BuildStatus.parse(element.status)
        val pauseTerminated = element.additionalOptions?.pauseBeforeExec == true &&
            (status.isFailure() || status.isCancel() || status == BuildStatus.TERMINATE)
        val containerStatus = BuildStatus.parse(container.status)
        val timeoutMinutes = container.jobTimeoutMinutes()
        return when {
            status == BuildStatus.REVIEW_ABORT -> ClassifiedEnd(BuildEndType.FAIL_REVIEW)
            status == BuildStatus.QUALITY_CHECK_FAIL -> ClassifiedEnd(BuildEndType.FAIL_QUALITY)
            status.isTimeout() -> ClassifiedEnd(BuildEndType.TIMEOUT_STEP)
            pauseTerminated -> ClassifiedEnd(
                endType = BuildEndType.FAIL_EXEC,
                reasonCode = REASON_PAUSE_TERMINATED
            )
            (status.isFailure() || status == BuildStatus.TERMINATE) &&
                containerStatus.isTimeout() && timeoutMinutes != null -> ClassifiedEnd(
                endType = BuildEndType.TIMEOUT_JOB,
                reasonCode = REASON_JOB_EXEC_TIMEOUT,
                reasonParams = listOf(timeoutMinutes.toString())
            )
            status.isFailure() -> {
                val errorMsg = toDisplayReason(element.errorMsg)
                ClassifiedEnd(
                    endType = BuildEndType.FAIL_EXEC,
                    reason = errorMsg,
                    reasonCode = if (errorMsg == null) REASON_PLUGIN_FAIL else null
                )
            }
            else -> null
        }
    }

    private fun pluginPosition(
        walk: ContainerWalk,
        containerId: String,
        jobPath: String,
        element: Element,
        elementIndex: Int,
        endType: BuildEndType?,
        reason: String?,
        reasonCode: String?,
        reasonParams: List<String>?
    ): EndPosition {
        val status = BuildStatus.parse(element.status)
        val review = element as? ManualReviewUserTaskElement
        return EndPosition(
            position = "${walk.stageIndex}-${walk.containerSeq}-${elementIndex + 1}",
            componentPath = "$jobPath/${element.name}",
            statusAtEnd = status.name,
            endType = endType,
            reason = reason,
            reasonCode = reasonCode,
            reasonParams = reasonParams,
            stageId = walk.stageId,
            containerId = containerId,
            taskId = element.id,
            matrixFlag = walk.matrixFlag.takeIf { it },
            errorMsg = toDisplayReason(element.errorMsg, ERROR_MSG_MAX),
            operator = review?.actualReviewUsers?.firstOrNull(),
            reviewSuggest = review?.suggest?.takeIf { it.isNotBlank() },
            containerHashId = walk.container.containerHashId,
            stepId = element.stepId
        )
    }

    private fun jobPosition(
        walk: ContainerWalk,
        containerId: String,
        jobPath: String,
        status: BuildStatus,
        classified: ClassifiedEnd
    ) = EndPosition(
        position = "${walk.stageIndex}-${walk.containerSeq}",
        componentPath = jobPath,
        statusAtEnd = status.name,
        endType = classified.endType,
        reason = classified.reason,
        reasonCode = classified.reasonCode,
        reasonParams = classified.reasonParams,
        stageId = walk.stageId,
        containerId = containerId,
        matrixFlag = walk.matrixFlag.takeIf { it },
        containerHashId = walk.container.containerHashId
    )

    private fun ContainerWalk.containerId(): String? {
        return container.containerId?.takeIf { it.isNotBlank() }
            ?: container.id?.takeIf { it.isNotBlank() }
    }

    private fun isCancelVisible(status: BuildStatus): Boolean {
        return status.isCancel() ||
            status == BuildStatus.TERMINATE ||
            status.isPause() ||
            status == BuildStatus.EXEC_TIMEOUT ||
            status == BuildStatus.HEARTBEAT_TIMEOUT
    }

    private fun isControlElement(element: Element): Boolean {
        val id = element.id ?: return false
        return id.startsWith("startVM-") || id.startsWith("stopVM-") || id.startsWith("end-")
    }

    private fun Container.enabledMutexGroup(): MutexGroup? {
        val group = when (this) {
            is VMBuildContainer -> mutexGroup
            is NormalContainer -> mutexGroup
            else -> null
        }
        return group?.takeIf { it.enable && it.fetchRuntimeMutexGroup().isNotBlank() }
    }

    private fun Container.jobTimeoutMinutes(): Int? {
        val minutes = when (this) {
            is VMBuildContainer -> jobControlOption?.timeout
            is NormalContainer -> jobControlOption?.timeout
            else -> null
        }
        return minutes?.takeIf { it > 0 }
    }

    private fun MutexGroup.toFailClassified(): ClassifiedEnd {
        val groupName = fetchRuntimeMutexGroup()
        return ClassifiedEnd(
            endType = BuildEndType.FAIL_EXEC,
            reasonCode = if (queueEnable) REASON_MUTEX_QUEUE else REASON_MUTEX_QUEUE_DISABLED,
            reasonParams = listOf(groupName)
        )
    }

    private data class ClassifiedEnd(
        val endType: BuildEndType,
        val reason: String? = null,
        val reasonCode: String? = null,
        val reasonParams: List<String>? = null
    )
}
