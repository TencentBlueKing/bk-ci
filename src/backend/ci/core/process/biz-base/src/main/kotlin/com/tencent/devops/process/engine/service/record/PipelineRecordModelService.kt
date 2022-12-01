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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.constant.ID
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordPipelineDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRecordModelService @Autowired constructor(
    private val buildRecordPipelineDao: BuildRecordPipelineDao,
    private val buildRecordStageDao: BuildRecordStageDao,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
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
        executeCount: Int
    ): Map<String, Any> {
        // 获取流水线级别变量数据
        val buildRecordPipeline = buildRecordPipelineDao.getRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val recordModelMap = buildRecordPipeline.pipelineVar
        // 获取stage级别变量数据
        val buildRecordStages = buildRecordStageDao.getRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取job级别变量数据
        val buildRecordContainers = buildRecordContainerDao.getRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        // 获取所有task级别变量数据
        val buildRecordTasks = buildRecordTaskDao.getRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = null,
            executeCount = executeCount
        )
        val stages = mutableListOf<Map<String, Any>>()
        buildRecordStages.forEach { buildRecordStage ->
            val stageVarMap = buildRecordStage.stageVar
            val stageId = buildRecordStage.stageId
            stageVarMap[ID] = stageId
            handleStageRecordContainer(
                buildRecordContainers = buildRecordContainers,
                stageId = stageId,
                buildRecordTasks = buildRecordTasks,
                stageVarMap = stageVarMap
            )
            stages.add(stageVarMap)
        }
        recordModelMap["stages"] = stages
        return recordModelMap
    }

    private fun handleStageRecordContainer(
        buildRecordContainers: List<BuildRecordContainer>,
        stageId: String,
        buildRecordTasks: List<BuildRecordTask>,
        stageVarMap: MutableMap<String, Any>
    ) {
        val stageRecordContainers = buildRecordContainers.filter { it.stageId == stageId }
        val stageRecordTasks = buildRecordTasks.filter { it.stageId == stageId }
        val containers = mutableListOf<Map<String, Any>>()
        stageRecordContainers.filter { it.matrixGroupFlag != true }.forEach { stageRecordContainer ->
            val containerVarMap = stageRecordContainer.containerVar
            val containerId = stageRecordContainer.containerId
            containerVarMap[ID] = containerId
            handleContainerRecordTask(stageRecordTasks, containerId, containerVarMap)
            val matrixGroupFlag = stageRecordContainer.matrixGroupFlag
            if (matrixGroupFlag == true) {
                // 过滤出矩阵分裂出的job数据
                val matrixRecordContainers = stageRecordContainers.filter { it.matrixGroupId == containerId }
                val groupContainers = mutableListOf<Map<String, Any>>()
                matrixRecordContainers.forEach { matrixRecordContainer ->
                    val matrixContainerVarMap = matrixRecordContainer.containerVar
                    val matrixContainerId = matrixRecordContainer.containerId
                    matrixContainerVarMap[ID] = matrixContainerId
                    handleContainerRecordTask(stageRecordTasks, matrixContainerId, matrixContainerVarMap)
                    groupContainers.add(matrixContainerVarMap)
                }
                containerVarMap["groupContainers"] = groupContainers
            }
            containers.add(containerVarMap)
        }
        stageVarMap["containers"] = containers
    }

    private fun handleContainerRecordTask(
        stageRecordTasks: List<BuildRecordTask>,
        containerId: String,
        containerVarMap: MutableMap<String, Any>
    ) {
        // 过滤出job下的task变量数据
        val containerRecordTasks = stageRecordTasks.filter { it.containerId == containerId }
        val tasks = mutableListOf<Map<String, Any>>()
        containerRecordTasks.forEach { containerRecordTask ->
            val taskVarMap = containerRecordTask.taskVar
            val taskId = containerRecordTask.taskId
            taskVarMap[ID] = taskId
            tasks.add(taskVarMap)
        }
        // 将转换后的job变量数据放入stage中
        containerVarMap["elements"] = tasks
    }
}
