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

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.DependOnJobInfo
import com.tencent.devops.common.pipeline.pojo.EndPosition

/**
 * 阶段定位上下文，用于在 Model 中标识目标 Stage 的位置信息。
 */
data class StagePosition(
    val stageIndex: Int,
    val stageName: String,
    val stageId: String
) {
    /** 阶段级位置编码，用于阶段准入驳回、阶段级质量红线失败等无具体 Job 的场景 */
    val position: String get() = "$stageIndex"
}

/**
 * 容器定位上下文，携带生成 EndPosition 所需的全部信息。
 */
data class ContainerLocation(
    val stagePosition: StagePosition,
    val containerSeq: Int,
    val container: Container,
    val matrixFlag: Boolean
) {
    val position: String get() = "${stagePosition.stageIndex}-$containerSeq"
    val componentPath: String get() = "${stagePosition.stageName}/${container.name}"

    /** 按 element.id 定位其在容器内的序号（从1开始），找不到时返回 null */
    fun taskSeqOf(taskId: String): Int? {
        val index = container.elements.indexOfFirst { it.id == taskId }
        return if (index < 0) null else index + 1
    }
}

/**
 * Model 位置索引：一次遍历建立 stageId / containerId → 位置信息的映射。
 *
 * 失败场景下可能存在多条错误信息，若逐条扫描整个 Model 会退化为 O(错误数 × Model规模)，
 * 因此统一先建索引再做 O(1) 定位。
 */
class ModelPositionIndex internal constructor(
    private val stageMap: Map<String, StagePosition>,
    private val containerMap: Map<String, ContainerLocation>
) {
    fun locateStage(stageId: String?): StagePosition? = stageId?.let { stageMap[it] }

    fun locateContainer(containerId: String?): ContainerLocation? = containerId?.let { containerMap[it] }
}

/**
 * 终态位置信息解析工具，提供从 Model/Container 中生成 EndPosition 的公共方法。
 *
 */
object EndPositionUtils {

    /**
     * 为单个容器生成终态位置信息列表。
     *
     * 粒度规则：
     * - RUNNING 且有运行中的 Task → 返回每个运行中 Task 的 task 级位置（可能多条）
     * - RUNNING 但无运行中 Task → 返回单条 container 级位置
     * - 其他状态（PREPARE_ENV / QUEUE / DEPENDENT_WAITING 等）→ 返回单条 container 级位置
     *
     * @param stagePosition 阶段定位信息（stageIndex、stageName、stageId）
     * @param container Model 中的容器对象
     * @param containerSeq 容器在阶段内的序号（从1开始）
     * @param matrixFlag 是否为矩阵子容器
     * @param dependOnJobs 依赖 Job 导航信息（调用方负责解析，非 RUNNING 状态时传入）
     */
    fun collectContainerEndPositions(
        stagePosition: StagePosition,
        container: Container,
        containerSeq: Int,
        matrixFlag: Boolean = false,
        dependOnJobs: List<DependOnJobInfo>? = null
    ): List<EndPosition> {
        val containerStatus = BuildStatus.parse(container.status)
        val containerName = container.name
        val containerId = container.containerId ?: container.id ?: return emptyList()
        val matrixValue = matrixFlag.takeIf { it }
        val positions = mutableListOf<EndPosition>()

        // RUNNING 状态：优先下钻到具体运行中的 Task，生成 task 级位置信息
        if (containerStatus == BuildStatus.RUNNING) {
            var taskSeq = 0
            container.elements.forEach { element ->
                taskSeq++
                val elementStatus = BuildStatus.parse(element.status)
                if (elementStatus.isRunning()) {
                    positions.add(
                        EndPosition(
                            position = "${stagePosition.stageIndex}-$containerSeq-$taskSeq",
                            componentPath = "${stagePosition.stageName}/$containerName/${element.name}",
                            statusAtEnd = elementStatus.name,
                            stageId = stagePosition.stageId,
                            containerId = containerId,
                            matrixFlag = matrixValue,
                            taskId = element.id
                        )
                    )
                }
            }
            // 无运行中 Task 时回退为 container 级位置
            if (positions.isEmpty()) {
                positions.add(
                    EndPosition(
                        position = "${stagePosition.stageIndex}-$containerSeq",
                        componentPath = "${stagePosition.stageName}/$containerName",
                        statusAtEnd = containerStatus.name,
                        stageId = stagePosition.stageId,
                        containerId = containerId,
                        matrixFlag = matrixValue
                    )
                )
            }
        } else {
            // 非 RUNNING 状态（PREPARE_ENV / QUEUE / DEPENDENT_WAITING 等）：直接记录 container 级位置
            positions.add(
                EndPosition(
                    position = "${stagePosition.stageIndex}-$containerSeq",
                    componentPath = "${stagePosition.stageName}/$containerName",
                    statusAtEnd = containerStatus.name,
                    stageId = stagePosition.stageId,
                    containerId = containerId,
                    matrixFlag = matrixValue,
                    dependOnJobs = dependOnJobs
                )
            )
        }
        return positions
    }

    /**
     * 一次遍历 Model 建立位置索引，供需要批量定位的场景（如失败位置收集）使用。
     *
     * 详情页不展示触发器阶段（Model 中的第一个 Stage），因此索引跳过 Stage-0，
     * 页面上的第一个 Stage 对应的 stageIndex 从 1 开始。
     */
    fun buildPositionIndex(model: Model): ModelPositionIndex {
        val stageMap = LinkedHashMap<String, StagePosition>()
        val containerMap = HashMap<String, ContainerLocation>()
        model.stages.forEachIndexed { stageIndex, stage ->
            if (stageIndex == 0) return@forEachIndexed
            val stageId = stage.id ?: return@forEachIndexed
            val stagePosition = StagePosition(stageIndex, stage.name.orEmpty(), stageId)
            stageMap[stageId] = stagePosition
            stage.containers.forEachIndexed { containerIndex, container ->
                val containerSeq = containerIndex + 1
                indexContainer(containerMap, stagePosition, container, containerSeq, matrixFlag = false)
                // 矩阵子容器沿用父容器序号，与 findContainerInStage 的定位口径保持一致
                container.fetchGroupContainers()?.forEach { matrixContainer ->
                    indexContainer(containerMap, stagePosition, matrixContainer, containerSeq, matrixFlag = true)
                }
            }
        }
        return ModelPositionIndex(stageMap, containerMap)
    }

    private fun indexContainer(
        containerMap: MutableMap<String, ContainerLocation>,
        stagePosition: StagePosition,
        container: Container,
        containerSeq: Int,
        matrixFlag: Boolean
    ) {
        // 容器真实ID：优先 containerId，兜底 id（兼容历史数据）
        val containerId = container.containerId ?: container.id ?: return
        containerMap[containerId] = ContainerLocation(stagePosition, containerSeq, container, matrixFlag)
    }

    /**
     * 从 Model 中按 stageId + containerId 定位指定容器，生成终态位置信息（支持 task 级粒度）。
     * 适用于已知具体受影响容器的场景（如心跳超时、Job 执行超时）。
     *
     * @param model 流水线编排模型
     * @param targetStageId 目标容器所在阶段ID
     * @param targetContainerId 目标容器ID
     * @return 位置信息列表，找不到目标容器时返回空列表
     */
    fun resolveEndPositions(
        model: Model,
        targetStageId: String,
        targetContainerId: String
    ): List<EndPosition> {
        // 查找目标阶段（跳过 Stage-0 系统保留阶段）
        val stage = model.stages
            .filterIndexed { index, _ -> index > 0 }
            .firstOrNull { it.id == targetStageId } ?: return emptyList()

        val stageIndex = model.stages.indexOf(stage)
        val stageName = stage.name.orEmpty()
        return findContainerInStage(
            stage = stage,
            stagePosition = StagePosition(stageIndex, stageName, targetStageId),
            targetContainerId = targetContainerId
        )
    }

    /**
     * 在指定阶段内按 containerId 查找容器（含 Matrix 子容器），找到后委托 collectContainerEndPositions 生成位置信息。
     * 使用 for 循环替代嵌套 lambda，避免深层嵌套和标签混淆。
     */
    private fun findContainerInStage(
        stage: Stage,
        stagePosition: StagePosition,
        targetContainerId: String
    ): List<EndPosition> {
        for ((containerIndex, container) in stage.containers.withIndex()) {
            val containerSeq = containerIndex + 1
            // 获取容器真实 ID：优先 containerId，兜底使用 id（兼容历史数据）
            val cId = container.containerId ?: container.id ?: continue
            if (cId == targetContainerId) {
                return collectContainerEndPositions(
                    stagePosition = stagePosition,
                    container = container,
                    containerSeq = containerSeq
                )
            }
            // 检查 Matrix 子容器（Group 容器下的分组容器）
            val matrixContainers = container.fetchGroupContainers() ?: continue
            for (matrixContainer in matrixContainers) {
                val mcId = matrixContainer.containerId ?: matrixContainer.id ?: continue
                if (mcId == targetContainerId) {
                    return collectContainerEndPositions(
                        stagePosition = stagePosition,
                        container = matrixContainer,
                        containerSeq = containerSeq,
                        matrixFlag = true
                    )
                }
            }
        }
        return emptyList()
    }
}
