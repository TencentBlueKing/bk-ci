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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.service.SubPipelineRefService
import com.tencent.devops.process.engine.service.SubPipelineTaskService
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class SubPipelineUpgradeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val modelTaskDao: PipelineModelTaskDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val subPipelineRefService: SubPipelineRefService,
    @Lazy
    private val subPipelineTaskService: SubPipelineTaskService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineUpgradeService::class.java)
        private val SUB_PIPELINE_ATOM_CODES = listOf("SubPipelineExec", "subPipelineCall")
    }

    fun createAllSubPipelineRef(projectId: String?, userId: String) {
        ThreadPoolUtil.submitAction(
            action = {
                val startTime = System.currentTimeMillis()
                logger.info("Start to create sub pipeline ref|projectId=$projectId")
                var offset = 0
                val limit = 1000
                try {
                    do {
                        val records = modelTaskDao.batchGetPipelineIdByAtomCode(
                            dslContext = dslContext,
                            projectId = projectId,
                            atomCodeList = SUB_PIPELINE_ATOM_CODES,
                            limit = limit,
                            offset = offset
                        )
                        val count = records?.size ?: 0
                        records?.forEach {
                            try {
                                updateSubPipelineRef(
                                    userId = userId,
                                    projectId = it.value1(),
                                    pipelineId = it.value2()
                                )
                            } catch (ignored: Exception) {
                                logger.warn(
                                    "Failed to update sub pipeline ref|${it.value1()}|${it.value2()}",
                                    ignored
                                )
                            }
                        }
                        offset += limit
                    } while (count == 1000)
                } catch (ignored: Exception) {
                    logger.warn("Failed to update sub pipeline ref", ignored)
                } finally {
                    logger.info("Finish to update sub pipeline ref|${System.currentTimeMillis() - startTime}")
                }
            },
            actionTitle = "createAllSubPipelineRef"
        )
    }

    fun createSubPipelineRef(projectId: String, pipelineId: String, userId: String) {
        ThreadPoolUtil.submitAction(
            action = {
                val startTime = System.currentTimeMillis()
                logger.info("Start to create sub pipeline ref|projectId=$projectId|pipelineId=$pipelineId")
                try {
                    updateSubPipelineRef(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId
                    )
                } catch (ignored: Exception) {
                    logger.warn("Failed to update sub pipeline ref|$projectId|$pipelineId", ignored)
                } finally {
                    logger.info("Finish to update sub pipeline ref|${System.currentTimeMillis() - startTime}")
                }
            },
            actionTitle = "createAllSubPipelineRef"
        )
    }

    fun updateSubPipelineRef(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        channel: String = ChannelCode.BS.name
    ) {
        try {
            // 解析model，读取子流水线引用信息
            val stageSize = model.stages.size
            val pipelineName = model.name
            val subPipelineRefList = mutableListOf<SubPipelineRef>()
            val contextMap = subPipelineTaskService.getContextMap(model.stages)
            if (stageSize > 1) {
                model.stages.subList(1, stageSize).forEachIndexed { stageIndex, stage ->
                    analysisSubPipelineRefAndSave(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        channel = channel,
                        stage = stage,
                        pipelineName = pipelineName,
                        subPipelineRefList = subPipelineRefList,
                        contextMap = contextMap,
                        stageIndex = stageIndex
                    )
                }
            }
            // 更新引用信息
            updateSubPipelineRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                subPipelineRefList = subPipelineRefList
            )
        } catch (e: Exception) {
            logger.warn("analysisSubPipelineRefAndSave failed", e)
        }
    }

    fun updateSubPipelineRecord(
        projectId: String,
        pipelineId: String,
        subPipelineRefList: List<SubPipelineRef>
    ) {
        dslContext.transaction { configuration ->
            val transaction = DSL.using(configuration)
            val existsRefs = subPipelineRefService.list(
                transaction = transaction,
                projectId = projectId,
                pipelineId = pipelineId
            )
            val targetRefs = subPipelineRefList.associateBy { "${it.pipelineId}_${it.element.id}" }
            val needDeleteInfos = existsRefs.filter { !targetRefs.containsKey("${it.pipelineId}_${it.taskId}") }
                .map { Triple(it.projectId, it.pipelineId, it.taskId) }.toSet()
            // 删除无效数据
            subPipelineRefService.batchDelete(transaction = transaction, infos = needDeleteInfos)
            // 添加新数据
            subPipelineRefService.batchAdd(
                transaction = transaction,
                subPipelineRefList = subPipelineRefList
            )
        }
    }

    fun updateSubPipelineRef(userId: String, projectId: String, pipelineId: String) {
        logger.info("update sub pipeline ref|$userId|$projectId|$pipelineId")
        val model = subPipelineTaskService.getModel(projectId, pipelineId) ?: return
        // 渠道
        val channel = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.channel ?: ChannelCode.BS.name
        updateSubPipelineRef(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            channel = channel
        )
    }

    private fun analysisSubPipelineRefAndSave(
        projectId: String,
        pipelineId: String,
        stage: Stage,
        channel: String,
        pipelineName: String,
        subPipelineRefList: MutableList<SubPipelineRef>,
        contextMap: Map<String, String>,
        stageIndex: Int
    ) {
        if (!stage.stageEnabled()) {
            return
        }
        stage.containers.forEachIndexed c@{ containerIndex, container ->
            if (container is TriggerContainer || !container.containerEnabled()) {
                return@c
            }
            container.elements.forEachIndexed { taskIndex, element ->
                if (element.elementEnabled()) {
                    subPipelineTaskService.getSubPipelineParam(
                        element = element,
                        projectId = projectId,
                        contextMap = contextMap
                    )?.let {
                        subPipelineRefList.add(
                            SubPipelineRef(
                                pipelineId = pipelineId,
                                projectId = projectId,
                                pipelineName = pipelineName,
                                element = element,
                                subProjectId = it.projectId,
                                subPipelineId = it.pipelineId,
                                channel = channel,
                                subPipelineName = it.pipelineName,
                                taskPosition = "${stageIndex + 1}-${containerIndex + 1}-${taskIndex + 1}",
                                taskProjectId = it.taskProjectId,
                                taskPipelineId = it.taskPipelineId,
                                taskPipelineType = it.taskPipelineType,
                                taskPipelineName = it.taskPipelineName
                            )
                        )
                    }
                }
            }
        }
    }

    fun delSubPipelineRef(
        userId: String,
        projectId: String,
        pipelineId: String?
    ) {
        logger.info("delSubPipelineRef|userId:$userId|projectId:$projectId|pipelineId:$pipelineId")
        subPipelineRefService.deleteAll(
            transaction = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }
}
