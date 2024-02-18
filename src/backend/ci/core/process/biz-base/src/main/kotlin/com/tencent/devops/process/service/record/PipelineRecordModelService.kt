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
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.service.PipelinePostElementService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.pipeline.record.MergeBuildRecordParam
import com.tencent.devops.process.utils.KEY_TASK_ATOM
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("UNCHECKED_CAST")
class PipelineRecordModelService @Autowired constructor(
    private val buildRecordStageDao: BuildRecordStageDao,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    private val pipelinePostElementService: PipelinePostElementService,
    private val dslContext: DSLContext,
    private val modelTaskIdGenerator: ModelTaskIdGenerator
) {

    /**
     * 生成构建变量模型map集合
     * @param mergeBuildRecordParam 合并流水线变量模型参数
     * @return 构建变量模型map集合
     */
    fun generateFieldRecordModelMap(
        mergeBuildRecordParam: MergeBuildRecordParam,
        queryDslContext: DSLContext? = null
    ): Map<String, Any> {
        val projectId = mergeBuildRecordParam.projectId
        val pipelineId = mergeBuildRecordParam.pipelineId
        val buildId = mergeBuildRecordParam.buildId
        val executeCount = mergeBuildRecordParam.executeCount
        val recordModelMap = mergeBuildRecordParam.recordModelMap
        val finalDSLContext = queryDslContext ?: dslContext
        // 获取stage级别变量数据
        val buildRecordStages = buildRecordStageDao.getLatestRecords(
            dslContext = finalDSLContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取job级别变量数据
        val buildNormalRecordContainers = buildRecordContainerDao.getLatestNormalRecords(
            dslContext = finalDSLContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val buildMatrixRecordContainers = buildRecordContainerDao.getLatestMatrixRecords(
            dslContext = finalDSLContext,
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
            dslContext = finalDSLContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            matrixContainerIds = matrixContainerIds
        )
        val buildRecordTasks = if (matrixContainerIds.isNotEmpty()) {
            val buildMatrixRecordTasks = buildRecordTaskDao.getLatestMatrixRecords(
                dslContext = finalDSLContext,
                projectId = projectId,
                buildId = buildId,
                executeCount = executeCount,
                matrixContainerIds = matrixContainerIds
            )
            buildNormalRecordTasks.plus(buildMatrixRecordTasks)
        } else {
            buildNormalRecordTasks
        }
        handleModelRecordStage(
            buildRecordStages = buildRecordStages,
            mergeBuildRecordParam = mergeBuildRecordParam,
            buildRecordContainers = buildRecordContainers,
            buildRecordTasks = buildRecordTasks,
            recordModelMap = recordModelMap
        )
        return recordModelMap
    }

    private fun handleModelRecordStage(
        buildRecordStages: List<BuildRecordStage>,
        mergeBuildRecordParam: MergeBuildRecordParam,
        buildRecordContainers: List<BuildRecordContainer>,
        buildRecordTasks: List<BuildRecordTask>,
        recordModelMap: MutableMap<String, Any>
    ) {
        val stages = mutableListOf<Map<String, Any>>()
        buildRecordStages.forEach { buildRecordStage ->
            val pipelineBaseModelMap = mergeBuildRecordParam.pipelineBaseModelMap
            val baseStages = pipelineBaseModelMap[Model::stages.name] as List<Map<String, Any>>
            val stageVarMap = buildRecordStage.stageVar
            val stageId = buildRecordStage.stageId
            stageVarMap[Stage::id.name] = stageId
            stageVarMap[Stage::status.name] = buildRecordStage.status ?: ""
            stageVarMap[Stage::executeCount.name] = buildRecordStage.executeCount
            val stageBaseMap = baseStages.first {
                it[Stage::id.name] == stageId
            }
            handleStageRecordContainer(
                buildRecordContainers = buildRecordContainers,
                stageId = stageId,
                buildRecordTasks = buildRecordTasks,
                stageVarMap = stageVarMap,
                stageBaseMap = stageBaseMap
            )
            stages.add(stageVarMap)
        }
        recordModelMap[Model::stages.name] = stages
    }

    private fun handleStageRecordContainer(
        buildRecordContainers: List<BuildRecordContainer>,
        stageId: String,
        buildRecordTasks: List<BuildRecordTask>,
        stageVarMap: MutableMap<String, Any>,
        stageBaseMap: Map<String, Any>
    ) {
        val stageRecordContainers =
            buildRecordContainers.filter { it.stageId == stageId }.sortedBy { it.containerId.toInt() }
        val stageRecordTasks = buildRecordTasks.filter { it.stageId == stageId }
        val containers = mutableListOf<Map<String, Any>>()
        val stageNormalRecordContainers = stageRecordContainers.filter { it.matrixGroupId.isNullOrBlank() }
        stageNormalRecordContainers.forEach { stageRecordContainer ->
            val containerVarMap = stageRecordContainer.containerVar
            val containerId = stageRecordContainer.containerId
            containerVarMap[Container::id.name] = containerId
            containerVarMap[Container::status.name] = stageRecordContainer.status ?: ""
            containerVarMap[Container::executeCount.name] = stageRecordContainer.executeCount
            containerVarMap[Container::containPostTaskFlag.name] = stageRecordContainer.containPostTaskFlag ?: false
            val containerBaseMap = (stageBaseMap[Stage::containers.name] as List<Map<String, Any>>).first {
                it[Container::id.name] == containerId
            }
            val containerBaseModelMap = containerBaseMap.deepCopy<MutableMap<String, Any>>()
            handleContainerRecordTask(
                stageRecordTasks = stageRecordTasks,
                containerId = containerId,
                containerVarMap = containerVarMap,
                containerBaseMap = containerBaseModelMap,
                containerExecuteCount = stageRecordContainer.executeCount
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
                        containerExecuteCount = stageRecordContainer.executeCount,
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

    @Suppress("LongParameterList")
    private fun handleContainerRecordTask(
        stageRecordTasks: List<BuildRecordTask>,
        containerId: String,
        containerVarMap: MutableMap<String, Any>,
        containerBaseMap: Map<String, Any>,
        containerExecuteCount: Int,
        matrixTaskFlag: Boolean = false
    ) {
        // 过滤出job下的task变量数据
        val containerRecordTasks = stageRecordTasks.filter { it.containerId == containerId }.sortedBy { it.taskSeq }
        val tasks = mutableListOf<Map<String, Any>>()
        val taskBaseMaps = containerBaseMap[Container::elements.name] as List<Map<String, Any>>
        // 如果job下的task都被跳过，则使用流水线model的element节点生成task模型
        if (containerRecordTasks.isEmpty()) {
            taskBaseMaps.forEach { taskBaseMap ->
                val taskVarMap = generateSkipTaskVarModel(matrixTaskFlag, taskBaseMap, containerExecuteCount)
                tasks.add(taskVarMap)
            }
            containerVarMap[Container::elements.name] = tasks
            return
        }
        val lastElementTaskId = taskBaseMaps[taskBaseMaps.size - 1][Element::id.name].toString()
        var supplementSkipTaskFlag = true
        var lastContainerRecordSeq = 1
        containerRecordTasks.forEachIndexed { index, containerRecordTask ->
            while (containerRecordTask.taskSeq - lastContainerRecordSeq > 1) {
                // 补充跳过的task对象
                val taskBaseMap = taskBaseMaps[lastContainerRecordSeq - 1]
                val taskVarMap = generateSkipTaskVarModel(matrixTaskFlag, taskBaseMap, containerRecordTask.executeCount)
                if (taskVarMap[Element::id.name].toString() == lastElementTaskId) {
                    supplementSkipTaskFlag = false
                }
                tasks.add(taskVarMap)
                lastContainerRecordSeq++
            }
            var taskVarMap = containerRecordTask.taskVar
            val taskId = containerRecordTask.taskId
            if (taskId == lastElementTaskId) {
                supplementSkipTaskFlag = false
            }
            taskVarMap[Element::id.name] = taskId
            taskVarMap[Element::status.name] = containerRecordTask.status ?: ""
            taskVarMap[Element::executeCount.name] = containerRecordTask.executeCount
            val elementPostInfo = containerRecordTask.elementPostInfo
            if (elementPostInfo != null) {
                // 生成post类型task的变量模型
                taskVarMap = doElementPostInfoBus(elementPostInfo, taskVarMap, containerBaseMap)
            }
            if (matrixTaskFlag && elementPostInfo == null) {
                // 生成矩阵task的变量模型
                val taskBaseMap = taskBaseMaps[index]
                taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
            }
            lastContainerRecordSeq = containerRecordTask.taskSeq
            tasks.add(taskVarMap)
        }
        // 判断当前job执行的task任务后是否还有跳过的task任务，有则继续补充跳过的task对象
        if (supplementSkipTaskFlag) {
            doSupplementSkipTaskBus(
                containerRecordTasks = containerRecordTasks,
                taskBaseMaps = taskBaseMaps,
                matrixTaskFlag = matrixTaskFlag,
                containerExecuteCount = containerExecuteCount,
                tasks = tasks
            )
        }
        if (tasks.isNotEmpty()) {
            // 将转换后的task变量数据放入job中
            containerVarMap[Container::elements.name] = tasks
        }
    }

    private fun doSupplementSkipTaskBus(
        containerRecordTasks: List<BuildRecordTask>,
        taskBaseMaps: List<Map<String, Any>>,
        matrixTaskFlag: Boolean,
        containerExecuteCount: Int,
        tasks: MutableList<Map<String, Any>>
    ) {
        val containerResourceRecordTasks = containerRecordTasks.filter {
            it.elementPostInfo == null && it.classType !in listOf(
                QualityGateInElement.classType, QualityGateOutElement.classType
            )
        }.sortedBy { it.taskSeq }
        if (containerResourceRecordTasks.isNotEmpty()) {
            val lastResourceRecordTaskId = containerResourceRecordTasks[containerResourceRecordTasks.size - 1].taskId
            val lastResourceRecordTaskBaseMap =
                taskBaseMaps.firstOrNull { it[Element::id.name] == lastResourceRecordTaskId }
            val lastResourceRecordTaskIndex = taskBaseMaps.indexOf(lastResourceRecordTaskBaseMap)
            val taskBaseMapNum = taskBaseMaps.size
            if (taskBaseMapNum - lastResourceRecordTaskIndex > 1) {
                for (i in lastResourceRecordTaskIndex + 1 until taskBaseMapNum) {
                    val taskVarMap = generateSkipTaskVarModel(matrixTaskFlag, taskBaseMaps[i], containerExecuteCount)
                    tasks.add(taskVarMap)
                }
            }
        }
    }

    private fun doElementPostInfoBus(
        elementPostInfo: ElementPostInfo,
        taskVarMap: MutableMap<String, Any>,
        containerBaseMap: Map<String, Any>
    ): MutableMap<String, Any> {
        var finalTaskVarMap = taskVarMap
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
        val additionalOptionsMap = finalTaskVarMap[Element::additionalOptions.name] as? MutableMap<String, Any>
        val finalAdditionalOptionsMap = if (additionalOptionsMap != null) {
            ModelUtils.generateBuildModelDetail(additionalOptionsMap, JsonUtil.toMutableMap(additionalOptions))
        } else {
            JsonUtil.toMutableMap(additionalOptions)
        }
        finalTaskVarMap[Element::additionalOptions.name] = finalAdditionalOptionsMap
        val parentElementJobIndex = elementPostInfo.parentElementJobIndex
        val taskBaseMap =
            (containerBaseMap[Container::elements.name] as List<Map<String, Any>>)[parentElementJobIndex]
        val taskName = taskBaseMap[Element::name.name]?.toString() ?: ""
        finalTaskVarMap[Element::name.name] = pipelinePostElementService.getPostElementName(taskName)
        finalTaskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), finalTaskVarMap)
        return finalTaskVarMap
    }

    private fun generateSkipTaskVarModel(
        matrixTaskFlag: Boolean,
        taskBaseMap: Map<String, Any>,
        executeCount: Int
    ): MutableMap<String, Any> {
        var taskVarMap = mutableMapOf<String, Any>()
        val taskId = if (matrixTaskFlag) {
            modelTaskIdGenerator.getNextId()
        } else {
            taskBaseMap[Element::id.name].toString()
        }
        taskVarMap[Element::id.name] = taskId
        taskVarMap[Element::status.name] = BuildStatus.SKIP.name
        taskVarMap[Element::executeCount.name] = executeCount
        if (matrixTaskFlag) {
            // 如果跳过的是矩阵类task，则需要生成完整的model对象以便合并
            taskVarMap["@type"] = MatrixStatusElement.classType
            taskVarMap[MatrixStatusElement::originClassType.name] =
                taskBaseMap[MatrixStatusElement::classType.name].toString()
            taskVarMap[MatrixStatusElement::originAtomCode.name] = taskBaseMap[KEY_ATOM_CODE].toString()
            taskVarMap[MatrixStatusElement::originTaskAtom.name] = taskBaseMap[KEY_TASK_ATOM].toString()
            taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
        }
        return taskVarMap
    }
}
