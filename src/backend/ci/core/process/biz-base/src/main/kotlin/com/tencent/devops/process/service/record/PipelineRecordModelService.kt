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

package com.tencent.devops.process.service.record

import com.tencent.devops.common.api.constant.KEY_TASK_ATOM
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

    companion object {
        private const val QUALITY_FLAG = "qualityFlag"
    }

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
        stageNormalRecordContainers.forEach { stageNormalRecordContainer ->
            val containerId = stageNormalRecordContainer.containerId
            val containerVarMap = generateContainerVarMap(stageNormalRecordContainer, containerId)
            val containerBaseMap = (stageBaseMap[Stage::containers.name] as List<Map<String, Any>>).first {
                it[Container::id.name] == containerId
            }
            val containerBaseModelMap = containerBaseMap.deepCopy<MutableMap<String, Any>>()
            handleContainerRecordTask(
                stageRecordTasks = stageRecordTasks,
                buildRecordContainer = stageNormalRecordContainer,
                containerVarMap = containerVarMap,
                containerBaseMap = containerBaseModelMap
            )
            val matrixGroupFlag = stageNormalRecordContainer.matrixGroupFlag
            if (matrixGroupFlag == true) {
                // 过滤出矩阵分裂出的job数据
                val matrixRecordContainers =
                    stageRecordContainers.filter { it.matrixGroupId == containerId }.sortedBy { it.containerId.toInt() }
                val groupContainers = mutableListOf<Map<String, Any>>()
                matrixRecordContainers.forEach { matrixRecordContainer ->
                    // 生成矩阵job的变量模型
                    val matrixContainerId = matrixRecordContainer.containerId
                    var matrixContainerVarMap = generateContainerVarMap(matrixRecordContainer, matrixContainerId)
                    handleContainerRecordTask(
                        stageRecordTasks = stageRecordTasks,
                        buildRecordContainer = matrixRecordContainer,
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

    private fun generateContainerVarMap(
        recordContainer: BuildRecordContainer,
        containerId: String
    ): MutableMap<String, Any> {
        val containerVarMap = recordContainer.containerVar
        containerVarMap[Container::id.name] = containerId
        containerVarMap[Container::status.name] = recordContainer.status ?: ""
        containerVarMap[Container::executeCount.name] = recordContainer.executeCount
        containerVarMap[Container::containPostTaskFlag.name] = recordContainer.containPostTaskFlag ?: false
        containerVarMap[Container::matrixGroupFlag.name] = recordContainer.matrixGroupFlag ?: false
        return containerVarMap
    }

    private fun handleContainerRecordTask(
        stageRecordTasks: List<BuildRecordTask>,
        buildRecordContainer: BuildRecordContainer,
        containerVarMap: MutableMap<String, Any>,
        containerBaseMap: MutableMap<String, Any>,
        matrixTaskFlag: Boolean = false
    ) {
        val containerId = buildRecordContainer.containerId
        // 过滤出job下的task变量数据
        val containerRecordTasks = stageRecordTasks.filter { it.containerId == containerId }.sortedBy { it.taskSeq }
        val tasks = mutableListOf<Map<String, Any>>()
        val taskBaseMaps = (containerBaseMap[Container::elements.name] as List<Map<String, Any>>).toMutableList()
        // 如果job下的task都被跳过，则使用流水线model的element节点生成task模型
        val containerExecuteCount = buildRecordContainer.executeCount
        if (buildRecordContainer.matrixGroupFlag != true && containerRecordTasks.isEmpty()) {
            taskBaseMaps.forEach { taskBaseMap ->
                val taskVarMap = generateSkipTaskVarModel(matrixTaskFlag, taskBaseMap, containerExecuteCount)
                tasks.add(taskVarMap)
            }
            containerVarMap[Container::elements.name] = tasks
            return
        }
        val lastElementTaskId = taskBaseMaps[taskBaseMaps.size - 1][Element::id.name].toString()
        var supplementSkipTaskFlag = true
        var preContainerRecordTaskSeq = 1
        // 获取开机任务的序号
        val startVMTaskSeq = buildRecordContainer.containerVar[Container::startVMTaskSeq.name]?.toString()?.toInt() ?: 1
        containerRecordTasks.forEach { containerRecordTask ->
            handleTaskSeq(startVMTaskSeq, containerRecordTask)
            val taskVarMap = generateTaskVarMap(
                containerRecordTask = containerRecordTask, taskId = containerRecordTask.taskId,
                containerBaseMap = containerBaseMap, matrixTaskFlag = matrixTaskFlag,
                taskBaseMaps = taskBaseMaps
            )
            while (containerRecordTask.taskSeq - preContainerRecordTaskSeq > 1) {
                // 补充跳过的task对象
                val taskBaseMap = taskBaseMaps[preContainerRecordTaskSeq - 1]
                val skipTaskVarMap = generateSkipTaskVarModel(
                    matrixTaskFlag = matrixTaskFlag,
                    taskBaseMap = taskBaseMap,
                    executeCount = containerRecordTask.executeCount,
                    mergeFlag = containerBaseMap[QUALITY_FLAG] == true
                )
                if (skipTaskVarMap[Element::id.name].toString() == lastElementTaskId) {
                    supplementSkipTaskFlag = false
                }
                tasks.add(skipTaskVarMap)
                preContainerRecordTaskSeq++
            }
            if (containerRecordTask.elementPostInfo != null || containerRecordTask.taskId == lastElementTaskId) {
                // 当job含有post任务或者db中存在model最后一个插件的任务记录，则不需要再补充跳过的task任务
                supplementSkipTaskFlag = false
            }
            preContainerRecordTaskSeq = containerRecordTask.taskSeq
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

    private fun handleTaskSeq(
        startVMTaskSeq: Int,
        containerRecordTask: BuildRecordTask
    ) {
        if (startVMTaskSeq < 1 || (startVMTaskSeq > 1 && startVMTaskSeq > containerRecordTask.taskSeq)) {
            // 当开机任务的序号大于1时，说明第一个任务不是开机任务，job含有内置插件任务，需要重新调整开机任务前面的task任务的taskSeq值
            containerRecordTask.taskSeq += 1
        }
    }

    private fun generateTaskVarMap(
        containerRecordTask: BuildRecordTask,
        taskId: String,
        containerBaseMap: MutableMap<String, Any>,
        matrixTaskFlag: Boolean,
        taskBaseMaps: MutableList<Map<String, Any>>
    ): MutableMap<String, Any> {
        var taskVarMap = containerRecordTask.taskVar
        taskVarMap[Element::id.name] = taskId
        taskVarMap[Element::status.name] = containerRecordTask.status ?: ""
        taskVarMap[Element::executeCount.name] = containerRecordTask.executeCount
        taskVarMap[Element::asyncStatus.name] = containerRecordTask.asyncStatus ?: ""
        val elementPostInfo = containerRecordTask.elementPostInfo
        if (elementPostInfo != null) {
            // 生成post类型task的变量模型
            taskVarMap = doElementPostInfoBus(elementPostInfo, taskVarMap, containerBaseMap)
        }
        val classType = containerRecordTask.classType
        val atomCode = containerRecordTask.atomCode
        val qualityTaskFlag = atomCode in listOf(QualityGateInElement.classType, QualityGateOutElement.classType)
        val taskBaseMapIndex = containerRecordTask.taskSeq - 2
        if (qualityTaskFlag) {
            // 补充质量红线相关信息以便详情页模型数据组装合并
            taskVarMap["@type"] = classType
            taskVarMap[KEY_ATOM_CODE] = atomCode
            taskBaseMaps.add(taskBaseMapIndex, taskVarMap)
            // 把当前job中含有质量红线的标识写入job模型
            containerBaseMap[QUALITY_FLAG] = true
        }
        // 当前job是矩阵类型或者质量红线任务标识为true，且当前任务不是post任务或者质量红线任务，则需要生成完整的task变量模型以便后面和model合并
        val mergeTaskVarFlag = (matrixTaskFlag || containerBaseMap[QUALITY_FLAG] == true) &&
            elementPostInfo == null && !qualityTaskFlag
        if (mergeTaskVarFlag) {
            // 生成完整的task的变量模型
            val taskBaseMap = taskBaseMaps[taskBaseMapIndex]
            taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
        }
        return taskVarMap
    }

    private fun doSupplementSkipTaskBus(
        containerRecordTasks: List<BuildRecordTask>,
        taskBaseMaps: List<Map<String, Any>>,
        matrixTaskFlag: Boolean,
        containerExecuteCount: Int,
        tasks: MutableList<Map<String, Any>>
    ) {
        val containerResourceRecordTasks = containerRecordTasks.filter {
            it.elementPostInfo == null
        }.sortedBy { it.taskSeq }
        if (containerResourceRecordTasks.isNotEmpty()) {
            val lastResourceRecordTaskSeq = containerResourceRecordTasks[containerResourceRecordTasks.size - 1].taskSeq
            val lastResourceRecordTaskIndex = lastResourceRecordTaskSeq - 2
            val taskBaseMapNum = taskBaseMaps.size
            if (taskBaseMapNum - lastResourceRecordTaskIndex > 1) {
                // 如果job里含有质量红线任务，则后续跳过的任务都需要生成完整的变量模型以便和model合并
                val mergeFlag = containerResourceRecordTasks.firstOrNull {
                    it.atomCode in listOf(QualityGateInElement.classType, QualityGateOutElement.classType)
                } != null
                for (i in lastResourceRecordTaskIndex + 1 until taskBaseMapNum) {
                    val taskVarMap = generateSkipTaskVarModel(
                        matrixTaskFlag = matrixTaskFlag,
                        taskBaseMap = taskBaseMaps[i],
                        executeCount = containerExecuteCount,
                        mergeFlag = mergeFlag
                    )
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
        finalTaskVarMap[Element::stepId.name] = "" // 为了避免影响父插件，post任务stepId需置为空
        finalTaskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), finalTaskVarMap)
        return finalTaskVarMap
    }

    private fun generateSkipTaskVarModel(
        matrixTaskFlag: Boolean,
        taskBaseMap: Map<String, Any>,
        executeCount: Int,
        mergeFlag: Boolean = false
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
                taskBaseMap[MatrixStatusElement.classType].toString()
            taskVarMap[MatrixStatusElement::originAtomCode.name] = taskBaseMap[KEY_ATOM_CODE].toString()
            taskVarMap[MatrixStatusElement::originTaskAtom.name] = taskBaseMap[KEY_TASK_ATOM].toString()
            taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
        } else if (mergeFlag) {
            taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap.deepCopy(), taskVarMap)
        }
        return taskVarMap
    }
}
