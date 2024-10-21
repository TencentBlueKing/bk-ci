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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CURRENT_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_OTHER_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE
import com.tencent.devops.process.dao.SubPipelineRefDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import com.tencent.devops.process.service.SubPipelineCheckService
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class SubPipelineRefService @Autowired constructor(
    private val dslContext: DSLContext,
    private val modelTaskDao: PipelineModelTaskDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val subPipelineRefDao: SubPipelineRefDao,
    private val subPipelineService: SubPipelineCheckService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRefService::class.java)
        private val SUB_PIPELINE_ATOM_CODES = listOf("SubPipelineExec", "subPipelineCall")
    }

    fun createAllSubPipelineRef(projectId: String?, userId: String) {
        ThreadPoolUtil.submitAction(
            action = {
                val startTime = System.currentTimeMillis()
                logger.info("Start to create sub pipeline ref")
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
                logger.info("Start to create sub pipeline ref")
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

    fun cleanSubPipelineRef(userId: String, projectId: String, pipelineId: String) {
        val changeCount = subPipelineRefDao.deleteAll(
            dslContext = dslContext,
            pipelineId = pipelineId,
            projectId = projectId
        )
        logger.info("user[$userId] delete sub pipeline ref|$projectId|$pipelineId|$changeCount")
    }

    fun updateSubPipelineRef(userId: String, projectId: String, pipelineId: String) {
        logger.info("update sub pipeline ref|$userId|$projectId|$pipelineId")
        val model = subPipelineService.getModel(projectId, pipelineId) ?: return
        // 渠道
        val channel = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.channel ?: ChannelCode.BS.name
        try {
            // 解析model，读取子流水线引用信息
            val stageSize = model.stages.size
            val pipelineName = model.name
            val subPipelineRefList = mutableListOf<SubPipelineRef>()
            val triggerStage = model.stages.getOrNull(0)
                ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB)
            val contextMap = getContextMap(triggerStage)
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
            logger.info("analysis sub pipeline ref|$subPipelineRefList")

            dslContext.transaction { configuration ->
                val transaction = DSL.using(configuration)
                val existsRefs = subPipelineRefDao.list(
                    dslContext = transaction,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                val targetRefs = subPipelineRefList.associateBy { "${it.pipelineId}_${it.element.id}" }
                val needDeleteIds = existsRefs.filter { !targetRefs.containsKey("${it.pipelineId}_${it.taskId}") }
                    .map { it.id }
                // 删除无效数据
                subPipelineRefDao.batchDelete(dslContext = transaction, ids = needDeleteIds)
                // 添加新数据
                subPipelineRefDao.batchAdd(
                    dslContext = transaction,
                    subPipelineRefList = subPipelineRefList
                )
            }
        } catch (e: Exception) {
            logger.warn("analysisSubPipelineRefAndSave failed", e)
        }
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
        stage.containers.forEachIndexed c@{ jobIndex, container ->
            if (container is TriggerContainer || !container.containerEnabled()) {
                return@c
            }
            container.elements.forEachIndexed{ taskIndex, element ->
                if (element.elementEnabled()) {
                    subPipelineService.getSubPipelineParam(
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
                                stageName = stage.name ?: "",
                                containerName = container.name,
                                subProjectId = it.projectId,
                                subPipelineId = it.pipelineId,
                                channel = channel,
                                subPipelineName = it.pipelineName,
                                containerSeq = "${stageIndex + 1}_${jobIndex + 1}_${taskIndex + 1}",
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

    fun checkCircularDependency(
        subPipelineRef: SubPipelineRef,
        rootPipelineKey: String,
        existsPipeline: HashMap<String, SubPipelineRef>
    ): ElementCheckResult {
        with(subPipelineRef) {
            logger.info("check circular dependency|subPipelineRef[$this]|existsPipeline[$existsPipeline]")
            val pipelineRefKey = "${subProjectId}_$subPipelineId"
            if (existsPipeline.contains(pipelineRefKey)) {
                logger.warn("subPipeline does not allow loop calls|projectId:$subProjectId|pipelineId:$subPipelineId")
                val parentPipelineRef = existsPipeline[pipelineRefKey]!!
                val (msgCode, params) = when {
                    // [当前流水线] -> [当前流水线]
                    "${projectId}_$pipelineId" == rootPipelineKey -> {
                        BK_CURRENT_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to emptyArray<String>()
                    }
                    // [其他流水线] -> [当前流水线]
                    pipelineRefKey == rootPipelineKey -> {
                        val editUrl = getPipelineEditUrl(projectId, pipelineId)
                        BK_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to arrayOf(
                            editUrl,
                            "${subPipelineRef.pipelineName} [$containerSeq]"
                        )
                    }
                    // [其他流水线_1] -> [其他流水线_2]
                    // [其他流水线_2] -> ... ->[其他流水线_1]
                    else -> {
                        val editUrlBase = getPipelineEditUrl(parentPipelineRef.projectId, parentPipelineRef.pipelineId)
                        val editUrl = getPipelineEditUrl(projectId, pipelineId)
                        BK_OTHER_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to arrayOf(
                            editUrl,
                            "${subPipelineRef.pipelineName} [${subPipelineRef.containerSeq}]",
                            editUrlBase,
                            parentPipelineRef.pipelineName.ifBlank { subPipelineRef.pipelineName }
                        )
                    }
                }

                return ElementCheckResult(
                    result = false,
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = msgCode,
                        params = params
                    )
                )
            }
            val subRefList = subPipelineRefDao.list(
                dslContext = dslContext,
                projectId = subProjectId,
                pipelineId = subPipelineId
            ).map {
                SubPipelineRef(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    channel = it.channel,
                    element = EmptyElement(
                        id = it.taskId,
                        name = it.taskName
                    ),
                    stageName = it.stageName,
                    containerName = it.containerName,
                    subPipelineId = it.subPipelineId,
                    subProjectId = it.subProjectId,
                    subPipelineName = it.subPipelineName ?: "",
                    taskProjectId = it.taskProjectId,
                    taskPipelineType = SubPipelineType.valueOf(it.taskPipelineType),
                    taskPipelineId = it.taskPipelineId,
                    taskPipelineName = it.taskPipelineName,
                    containerSeq = it.containerSeq
                )
            }
            logger.info("check circular dependency|subRefList[$subRefList]")
            if (subRefList.isEmpty()) return ElementCheckResult(true)
            subRefList.forEach {
                val exist = HashMap(existsPipeline)
                exist["${it.projectId}_${it.pipelineId}"] = it
                logger.info(
                    "callPipelineStartup|" +
                            "supProjectId:${it.subProjectId},subPipelineId:${it.subPipelineId}," +
                            "subElementId:${it.element.id},parentProjectId:${it.projectId}," +
                            "parentPipelineId:${it.pipelineId}"
                )
                val checkResult = checkCircularDependency(
                    subPipelineRef = it,
                    rootPipelineKey = rootPipelineKey,
                    existsPipeline = exist
                )
                // 检查不成功，直接返回
                if (!checkResult.result) {
                    return checkResult
                }
                existsPipeline.putAll(exist)
            }
            return ElementCheckResult(true)
        }
    }

    fun getContextMap(triggerStage: Stage): Map<String /* 流水线变量名 */, String> {
        val triggerContainer = (triggerStage.containers.getOrNull(0) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
        )) as TriggerContainer
        // 检查触发容器
        val paramsMap = PipelineUtils.checkPipelineParams(triggerContainer.params)
        return PipelineVarUtil.fillVariableMap(paramsMap.mapValues { it.value.defaultValue.toString() })
    }

    fun deleteElement(projectId: String, pipelineId: String, taskId: String) {
        logger.info("delete sub pipeline ref|projectId[$projectId]|pipelineId[$pipelineId]|taskId[$taskId]")
        subPipelineRefDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId
        )
    }

    // 流水线编辑界面
    fun getPipelineEditUrl(projectId: String, pipelineId: String) = "/console/pipeline/$projectId/$pipelineId/edit"
}
