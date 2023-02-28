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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRecordModelService @Autowired constructor(
    private val buildRecordStageDao: BuildRecordStageDao,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    private val pipelineResDao: PipelineResDao,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineElementService: PipelineElementService,
    private val dslContext: DSLContext
) {

    /**
     * 生成构建变量模型map集合
     * @param projectId 项目标识
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param executeCount 执行次数
     * @return 构建变量模型map集合
     */
    fun generateFieldRecordModelMap(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildRecordModel: BuildRecordModel
    ): Map<String, Any> {
        val recordModelMap = buildRecordModel.modelVar
        // 获取stage级别变量数据
        val buildRecordStages = buildRecordStageDao.getLatestRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取job级别变量数据
        val buildRecordContainers = buildRecordContainerDao.getLatestRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取所有task级别变量数据
        val buildRecordTasks = buildRecordTaskDao.getLatestRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val stages = mutableListOf<Map<String, Any>>()
        var pipelineBaseMap: Map<String, Any>? = null
        // 判断流水线是否具有矩阵特性
        val matrixContainerFlag = buildRecordContainers.indexOfFirst { it.matrixGroupFlag == true } >= 0
        if (matrixContainerFlag) {
            // 查出该次构建对应的流水线基本模型
            val version = buildRecordModel.resourceVersion
            val modelStr = pipelineResVersionDao.getVersionModelString(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            ) ?: pipelineResDao.getVersionModelString(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            ) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = arrayOf("$KEY_PROJECT_ID:$projectId,$KEY_PIPELINE_ID:$pipelineId,$KEY_VERSION:$version")
            )
            val fullModel = JsonUtil.to(modelStr, Model::class.java)
            // 为model填充element
            pipelineElementService.fillElementWhenNewBuild(fullModel, projectId, pipelineId)
            pipelineBaseMap = JsonUtil.toMap(fullModel)
        }
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
                pipelineBaseMap = pipelineBaseMap
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
        pipelineBaseMap: Map<String, Any>? = null
    ) {
        val stageRecordContainers = buildRecordContainers.filter { it.stageId == stageId }
        val stageRecordTasks = buildRecordTasks.filter { it.stageId == stageId }
        val containers = mutableListOf<Map<String, Any>>()
        stageRecordContainers.forEach { stageRecordContainer ->
            val containerVarMap = stageRecordContainer.containerVar
            val containerId = stageRecordContainer.containerId
            containerVarMap[Container::id.name] = containerId
            containerVarMap[Container::status.name] = stageRecordContainer.status ?: ""
            containerVarMap[Container::executeCount.name] = stageRecordContainer.executeCount
            containerVarMap[Container::containPostTaskFlag.name] = stageRecordContainer.containPostTaskFlag ?: false
            handleContainerRecordTask(stageRecordTasks, containerId, containerVarMap)
            val matrixGroupFlag = stageRecordContainer.matrixGroupFlag
            if (matrixGroupFlag == true) {
                val stageBaseMap = (pipelineBaseMap!![Model::stages.name] as List<Map<String, Any>>).first {
                    it[Stage::id.name] == stageId
                }
                val containerBaseMap = (stageBaseMap[Stage::containers.name] as List<Map<String, Any>>).first {
                    it[Container::id.name] == containerId
                }
                // 过滤出矩阵分裂出的job数据
                val matrixRecordContainers = stageRecordContainers.filter { it.matrixGroupId == containerId }
                val groupContainers = mutableListOf<Map<String, Any>>()
                matrixRecordContainers.forEach { matrixRecordContainer ->
                    // 生成矩阵job的变量模型
                    var matrixContainerVarMap = matrixRecordContainer.containerVar
                    val matrixContainerId = matrixRecordContainer.containerId
                    val containerBaseModelMap = containerBaseMap.deepCopy<MutableMap<String, Any>>()
                    matrixContainerVarMap[Container::id.name] = matrixContainerId
                    matrixContainerVarMap[Container::status.name] = matrixRecordContainer.status ?: ""
                    matrixContainerVarMap[Container::executeCount.name] = matrixRecordContainer.executeCount
                    matrixContainerVarMap[Container::containPostTaskFlag.name] =
                        matrixRecordContainer.containPostTaskFlag ?: false
                    handleContainerRecordTask(
                        stageRecordTasks = stageRecordTasks,
                        containerId = matrixContainerId,
                        containerVarMap = matrixContainerVarMap,
                        containerBaseMap = containerBaseModelMap
                    )
                    containerBaseModelMap.remove(VMBuildContainer::matrixControlOption.name)
                    containerBaseModelMap.remove(VMBuildContainer::groupContainers.name)
                    matrixContainerVarMap = ModelUtils.generateBuildModelDetail(
                        baseModelMap = containerBaseModelMap,
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
        containerBaseMap: Map<String, Any>? = null
    ) {
        // 过滤出job下的task变量数据
        val containerRecordTasks = stageRecordTasks.filter { it.containerId == containerId }
        val tasks = mutableListOf<Map<String, Any>>()
        containerRecordTasks.forEachIndexed { index, containerRecordTask ->
            var taskVarMap = containerRecordTask.taskVar
            val taskId = containerRecordTask.taskId
            taskVarMap[Element::id.name] = taskId
            taskVarMap[Element::status.name] = containerRecordTask.status ?: ""
            taskVarMap[Element::executeCount.name] = containerRecordTask.executeCount
            containerBaseMap?.let {
                // 生成矩阵task的变量模型
                val taskBaseMap = (it[Container::elements.name] as List<Map<String, Any>>)[index].toMutableMap()
                taskVarMap = ModelUtils.generateBuildModelDetail(taskBaseMap, taskVarMap)
            }
            tasks.add(taskVarMap)
        }
        // 将转换后的job变量数据放入stage中
        containerVarMap[Container::elements.name] = tasks
    }
}
