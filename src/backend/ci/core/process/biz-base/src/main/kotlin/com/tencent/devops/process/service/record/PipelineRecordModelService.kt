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

package com.tencent.devops.process.service.record

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.service.PipelinePostElementService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.pipeline.record.MergeBuildRecordParam
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRecordModelService @Autowired constructor(
    private val buildRecordStageDao: BuildRecordStageDao,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    private val pipelinePostElementService: PipelinePostElementService,
    private val dslContext: DSLContext
) {

    /**
     * 生成构建变量模型map集合
     * @param mergeBuildRecordParam 合并流水线变量模型参数
     * @return 构建变量模型map集合
     */
    fun generateFieldRecordModelMap(
        mergeBuildRecordParam: MergeBuildRecordParam
    ): Map<String, Any> {
        val projectId = mergeBuildRecordParam.projectId
        val pipelineId = mergeBuildRecordParam.pipelineId
        val buildId = mergeBuildRecordParam.buildId
        val executeCount = mergeBuildRecordParam.executeCount
        val recordModelMap = mergeBuildRecordParam.recordModelMap
        // 获取stage级别变量数据
        val buildRecordStages = buildRecordStageDao.getLatestRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取job级别变量数据
        val buildNormalRecordContainers = buildRecordContainerDao.getLatestNormalRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val buildMatrixRecordContainers = buildRecordContainerDao.getLatestMatrixRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val buildRecordContainers =
            buildNormalRecordContainers.plus(buildMatrixRecordContainers)
        // 获取task级别变量数据
        val matrixContainerIds = buildMatrixRecordContainers.map { it.containerId }
        val buildNormalRecordTasks = buildRecordTaskDao.getLatestNormalRecords(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            matrixContainerIds = matrixContainerIds
        )
        val buildRecordTasks = if (matrixContainerIds.isNotEmpty()) {
            val buildMatrixRecordTasks = buildRecordTaskDao.getLatestMatrixRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                executeCount = executeCount,
                matrixContainerIds = matrixContainerIds
            )
            buildNormalRecordTasks.plus(buildMatrixRecordTasks)
        } else {
            buildNormalRecordTasks
        }
        val stages = mutableListOf<Map<String, Any>>()
        buildRecordStages.forEach { buildRecordStage ->
            val stageVarMap = buildRecordStage.stageVar
            val stageId = buildRecordStage.stageId
            stageVarMap[Stage::id.name] = stageId
            stageVarMap[Stage::status.name] = buildRecordStage.status ?: ""
            stageVarMap[Stage::executeCount.name] = buildRecordStage.executeCount
            handleStageRecordContainer(
                buildRecordContainers = buildRecordContainers,
                stageId = stageId,
                buildRecordTasks = buildRecordTasks,
                stageVarMap = stageVarMap,
                pipelineBaseModelMap = mergeBuildRecordParam.pipelineBaseModelMap
            )
            stages.add(stageVarMap)
        }
        recordModelMap[Model::stages.name] = stages
        return recordModelMap
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleStageRecordContainer(
        buildRecordContainers: List<BuildRecordContainer>,
        stageId: String,
        buildRecordTasks: List<BuildRecordTask>,
        stageVarMap: MutableMap<String, Any>,
        pipelineBaseModelMap: Map<String, Any>
    ) {
        val stageRecordContainers =
            buildRecordContainers.filter { it.stageId == stageId }.sortedBy { it.containerId.toInt() }
        val stageRecordTasks = buildRecordTasks.filter { it.stageId == stageId }
        val containers = mutableListOf<Map<String, Any>>()
        stageRecordContainers.filter { it.matrixGroupId.isNullOrBlank() }.forEach { stageRecordContainer ->
            val containerVarMap = stageRecordContainer.containerVar
            val containerId = stageRecordContainer.containerId
            containerVarMap[Container::id.name] = containerId
            containerVarMap[Container::status.name] = stageRecordContainer.status ?: ""
            containerVarMap[Container::executeCount.name] = stageRecordContainer.executeCount
            containerVarMap[Container::containPostTaskFlag.name] = stageRecordContainer.containPostTaskFlag ?: false
            val stageBaseMap = (pipelineBaseModelMap[Model::stages.name] as List<Map<String, Any>>).first {
                it[Stage::id.name] == stageId
            }
            val containerBaseMap = (stageBaseMap[Stage::containers.name] as List<Map<String, Any>>).first {
                it[Container::id.name] == containerId
            }
            val containerBaseModelMap = containerBaseMap.deepCopy<MutableMap<String, Any>>()
            handleContainerRecordTask(
                stageRecordTasks = stageRecordTasks,
                containerId = containerId,
                containerVarMap = containerVarMap,
                containerBaseMap = containerBaseModelMap
            )
            val matrixGroupFlag = stageRecordContainer.matrixGroupFlag
            if (matrixGroupFlag == true) {
                // 过滤出矩阵分裂出的job数据
                val matrixRecordContainers =
                    stageRecordContainers.filter { it.matrixGroupId == containerId }.sortedBy { it.containerId.toInt() }
                val groupContainers = mutableListOf<Map<String, Any>>()
                matrixRecordContainers.forEach { matrixRecordContainer ->
                    // 生成矩阵job的变量模型
                    var matrixContainerVarMap = matrixRecordContainer.containerVar
                    val matrixContainerId = matrixRecordContainer.containerId
                    matrixContainerVarMap[Container::id.name] = matrixContainerId
                    matrixContainerVarMap[Container::status.name] = matrixRecordContainer.status ?: ""
                    matrixContainerVarMap[Container::executeCount.name] = matrixRecordContainer.executeCount
                    matrixContainerVarMap[Container::containPostTaskFlag.name] =
                        matrixRecordContainer.containPostTaskFlag ?: false
                    handleContainerRecordTask(
                        stageRecordTasks = stageRecordTasks,
                        containerId = matrixContainerId,
                        containerVarMap = matrixContainerVarMap,
                        containerBaseMap = containerBaseModelMap,
                        matrixTaskFlag = true
                    )
                    containerBaseModelMap.remove(VMBuildContainer::matrixControlOption.name)
                    containerBaseModelMap.remove(VMBuildContainer::groupContainers.name)
                    matrixContainerVarMap = ModelUtils.generateBuildModelDetail(
                        baseModelMap = containerBaseModelMap.deepCopy(),
                        modelFieldRecordMap = matrixContainerVarMap
                    )
                    groupContainers.add(matrixContainerVarMap)
                }
                containerVarMap[VMBuildContainer::groupContainers.name] = groupContainers
            }
            containers.add(containerVarMap)
        }
        stageVarMap[Stage::containers.name] = containers
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleContainerRecordTask(
        stageRecordTasks: List<BuildRecordTask>,
        containerId: String,
        containerVarMap: MutableMap<String, Any>,
        containerBaseMap: Map<String, Any>,
        matrixTaskFlag: Boolean = false
    ) {
        // 过滤出job下的task变量数据
        val containerRecordTasks = stageRecordTasks.filter { it.containerId == containerId }.sortedBy { it.taskSeq }
        val tasks = mutableListOf<Map<String, Any>>()
        containerRecordTasks.forEachIndexed { index, containerRecordTask ->
            var taskVarMap = containerRecordTask.taskVar
            val taskId = containerRecordTask.taskId
            taskVarMap[Element::id.name] = taskId
            taskVarMap[Element::status.name] = containerRecordTask.status ?: ""
            taskVarMap[Element::executeCount.name] = containerRecordTask.executeCount
            val elementPostInfo = containerRecordTask.elementPostInfo
            if (elementPostInfo != null) {
                // 生成post类型task的变量模型
                val additionalOptions = ElementAdditionalOptions(
                    enable = true,
                    continueWhenFailed = true,
                    retryWhenFailed = false,
                    runCondition = pipelinePostElementService.getPostAtomRunCondition(elementPostInfo.postCondition),
                    pauseBeforeExec = null,
                    subscriptionPauseUser = null,
                    otherTask = null,
                    customCondition = null,
                    elementPostInfo = elementPostInfo
                )
                val additionalOptionsMap = taskVarMap[Element::additionalOptions.name] as? MutableMap<String, Any>
                val finalAdditionalOptionsMap = if (additionalOptionsMap != null) {
                    ModelUtils.generateBuildModelDetail(additionalOptionsMap, JsonUtil.toMutableMap(additionalOptions))
                } else {
                    JsonUtil.toMutableMap(additionalOptions)
                }
                taskVarMap[Element::additionalOptions.name] = finalAdditionalOptionsMap
                val parentElementJobIndex = elementPostInfo.parentElementJobIndex
                val taskBaseMap =
                    (containerBaseMap[Container::elements.name] as List<Map<String, Any>>)[parentElementJobIndex]
                val taskName = taskBaseMap[Element::name.name]?.toString() ?: ""
                taskVarMap[Element::name.name] = pipelinePostElementService.getPostElementName(taskName)
                taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
            }
            if (matrixTaskFlag && elementPostInfo == null) {
                // 生成矩阵task的变量模型
                val taskBaseMap =
                    (containerBaseMap[Container::elements.name] as List<Map<String, Any>>)[index]
                taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
            }
            tasks.add(taskVarMap)
        }
        if (tasks.isNotEmpty()) {
            // 将转换后的job变量数据放入stage中
            containerVarMap[Container::elements.name] = tasks
        }
    }
}
