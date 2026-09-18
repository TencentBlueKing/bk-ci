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
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.EndPosition
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement

/**
 * 从编排模型收集失败位置：取消链路拍下的是当时的中间态（如 PAUSE），
 * 构建真正结束后要以插件当前终态（FAILED / REVIEW_ABORT 等）为准。
 */
object BuildEndPositionCollector {

    private const val POSITION_MAX_SIZE = 50

    fun collectFailPositions(model: Model): List<EndPosition> {
        val positions = mutableListOf<EndPosition>()
        model.stages.forEachIndexed { stageIndex, stage ->
            if (stageIndex == 0) return@forEachIndexed
            val stageId = stage.id ?: return@forEachIndexed
            val stageName = stage.name.orEmpty()
            stage.containers.forEachIndexed { containerIndex, container ->
                collectFromContainer(
                    positions = positions,
                    stageIndex = stageIndex,
                    stageName = stageName,
                    stageId = stageId,
                    container = container,
                    containerSeq = containerIndex + 1,
                    matrixFlag = false
                )
                container.fetchGroupContainers()?.forEach { matrixContainer ->
                    collectFromContainer(
                        positions = positions,
                        stageIndex = stageIndex,
                        stageName = stageName,
                        stageId = stageId,
                        container = matrixContainer,
                        containerSeq = containerIndex + 1,
                        matrixFlag = true
                    )
                }
            }
        }
        return positions.take(POSITION_MAX_SIZE)
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

    private fun collectFromContainer(
        positions: MutableList<EndPosition>,
        stageIndex: Int,
        stageName: String,
        stageId: String,
        container: Container,
        containerSeq: Int,
        matrixFlag: Boolean
    ) {
        val containerId = container.containerId?.takeIf { it.isNotBlank() }
            ?: container.id?.takeIf { it.isNotBlank() }
            ?: return
        val jobPath = "$stageName/${container.name}"
        container.elements.forEachIndexed { elementIndex, element ->
            if (isControlElement(element)) return@forEachIndexed
            val status = BuildStatus.parse(element.status)
            val endType = failEndTypeOf(status) ?: return@forEachIndexed
            val review = element as? ManualReviewUserTaskElement
            positions.add(
                EndPosition(
                    position = "$stageIndex-$containerSeq-${elementIndex + 1}",
                    componentPath = "$jobPath/${element.name}",
                    statusAtEnd = status.name,
                    endType = endType,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = element.id,
                    matrixFlag = matrixFlag.takeIf { it },
                    operator = review?.actualReviewUsers?.firstOrNull(),
                    reviewSuggest = review?.suggest?.takeIf { it.isNotBlank() },
                    containerHashId = container.containerHashId,
                    stepId = element.stepId
                )
            )
        }
    }

    private fun failEndTypeOf(status: BuildStatus): BuildEndType? = when {
        status == BuildStatus.REVIEW_ABORT -> BuildEndType.FAIL_REVIEW
        status == BuildStatus.QUALITY_CHECK_FAIL -> BuildEndType.FAIL_QUALITY
        status.isFailure() -> BuildEndType.FAIL_EXEC
        else -> null
    }

    private fun isControlElement(element: Element): Boolean {
        val id = element.id ?: return false
        return id.startsWith("startVM-") || id.startsWith("stopVM-") || id.startsWith("end-")
    }
}
