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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.SubPipelineRefDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Suppress("ALL")
@Service
class SubPipelineRefService @Autowired constructor(
    private val dslContext: DSLContext,
    private val modelTaskDao: PipelineModelTaskDao,
    private val pipelineResDao: PipelineResourceDao,
    private val objectMapper: ObjectMapper,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val subPipelineRefDao: SubPipelineRefDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRefService::class.java)
        private val pattern = Pattern.compile("(p-)?[a-f\\d]{32}")
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
                            atomCodeList = listOf("SubPipelineExec"),
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

    fun updateSubPipelineRef(userId: String, projectId: String, pipelineId: String) {
        logger.info("update sub pipeline ref|$userId|$projectId|$pipelineId")
        val model: Model?
        val modelString = pipelineResDao.getLatestVersionModelString(dslContext, projectId, pipelineId)
        if (modelString.isNullOrBlank()) {
            logger.warn("model not found: [$userId|$projectId|$pipelineId]")
            return
        }
        try {
            model = objectMapper.readValue(modelString, Model::class.java)
        } catch (ignored: Exception) {
            logger.warn("parse process($pipelineId) model fail", ignored)
            return
        }
        // 渠道
        val channel = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.channel ?: ChannelCode.BS.name
        try {
            // 解析model，读取子流水线引用信息
            val stageSize = model.stages.size
            val subPipelineRefList = mutableListOf<SubPipelineRef>()
            val triggerStage = model.stages.getOrNull(0)
                ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB)
            val contextMap = getContextMap(triggerStage)
            if (stageSize > 1) {
                model.stages.subList(1, stageSize).forEach { stage ->
                    analysisSubPipelineRefAndSave(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        channel = channel,
                        stage = stage,
                        subPipelineRefList = subPipelineRefList,
                        contextMap = contextMap
                    )
                }
            }
            dslContext.transaction { configuration ->
                subPipelineRefDao.batchAdd(
                    dslContext = DSL.using(configuration),
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
        subPipelineRefList: MutableList<SubPipelineRef>,
        contextMap: Map<String, String>
    ) {
        if (!stage.isStageEnable()) {
            return
        }
        stage.containers.forEach c@{ container ->
            if (container is TriggerContainer || !container.isContainerEnable()) {
                return@c
            }
            container.elements.forEach { element ->
                getSubPipelineInfo(
                    element = element,
                    projectId = projectId,
                    contextMap = contextMap
                )?.let {
                    subPipelineRefList.add(
                        SubPipelineRef(
                            pipelineId = pipelineId,
                            projectId = projectId,
                            pipelineName = it.third,
                            taskId = element.id ?: "",
                            taskName = element.name,
                            stageName = stage.name ?: "",
                            containerName = container.name,
                            subProjectId = it.first,
                            subPipelineId = it.second,
                            channel = channel
                        )
                    )
                }
            }
        }
    }

    fun getSubPipelineInfo(
        element: Element,
        projectId: String,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? = when (element) {
        is SubPipelineCallElement -> {
            resolveSubPipelineCall(
                projectId = projectId,
                element = element,
                contextMap = contextMap
            )
        }

        is MarketBuildAtomElement -> {
            resolveSubPipelineExec(
                projectId = projectId,
                inputMap = element.data["input"] as Map<String, Any>,
                contextMap = contextMap
            )
        }

        is MarketBuildLessAtomElement -> {
            resolveSubPipelineExec(
                projectId = projectId,
                inputMap = element.data["input"] as Map<String, Any>,
                contextMap = contextMap
            )
        }

        else -> null
    }

    fun getContextMap(triggerStage: Stage): Map<String /* 流水线变量名 */, String> {
        val triggerContainer = (triggerStage.containers.getOrNull(0) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
        )) as TriggerContainer
        // 检查触发容器
        val paramsMap = PipelineUtils.checkPipelineParams(triggerContainer.params)
        return PipelineVarUtil.fillVariableMap(paramsMap.mapValues { it.value.defaultValue.toString() })
    }

    private fun resolveSubPipelineCall(
        projectId: String,
        element: SubPipelineCallElement,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? {
        val subPipelineType = element.subPipelineType ?: SubPipelineType.ID
        val subPipelineId = element.subPipelineId
        val subPipelineName = element.subPipelineName
        return getSubPipelineInfo(
            projectId = projectId,
            subProjectId = projectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        )
    }

    private fun resolveSubPipelineExec(
        projectId: String,
        inputMap: Map<String, Any>,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? {
        val subProjectId = inputMap.getOrDefault("projectId", projectId).toString()
        val subPipelineTypeStr = inputMap.getOrDefault("subPipelineType", "ID")
        val subPipelineName = inputMap["subPipelineName"]?.toString()
        val subPipelineId = inputMap["subPip"]?.toString()
        val subPipelineType = when (subPipelineTypeStr) {
            "ID" -> SubPipelineType.ID
            "NAME" -> SubPipelineType.NAME
            else -> return null
        }
        return getSubPipelineInfo(
            projectId = projectId,
            subProjectId = subProjectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        )
    }

    private fun getSubPipelineInfo(
        projectId: String,
        subProjectId: String,
        subPipelineType: SubPipelineType,
        subPipelineId: String?,
        subPipelineName: String?,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? {
        return when (subPipelineType) {
            SubPipelineType.ID -> {
                if (subPipelineId.isNullOrBlank()) {
                    return null
                }
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                    projectId = subProjectId, pipelineId = subPipelineId
                ) ?: run {
                    logger.info(
                        "sub-pipeline not found|projectId:$projectId|subPipelineType:$subPipelineType|" +
                                "subProjectId:$subProjectId|subPipelineId:$subPipelineId"
                    )
                    return null
                }
                Triple(subProjectId, subPipelineId, pipelineInfo.pipelineName)
            }

            SubPipelineType.NAME -> {
                if (subPipelineName.isNullOrBlank()) {
                    return null
                }
                val finalSubProjectId = EnvUtils.parseEnv(subProjectId, contextMap)
                var finalSubPipelineName = EnvUtils.parseEnv(subPipelineName, contextMap)
                var finalSubPipelineId = pipelineRepositoryService.listPipelineIdByName(
                    projectId = finalSubProjectId,
                    pipelineNames = setOf(finalSubPipelineName),
                    filterDelete = true
                )[finalSubPipelineName]
                // 流水线名称直接使用流水线ID代替
                if (finalSubPipelineId.isNullOrBlank() && pattern.matcher(finalSubPipelineName).matches()) {
                    finalSubPipelineId = finalSubPipelineName
                    finalSubPipelineName = pipelineRepositoryService.getPipelineInfo(
                        projectId = finalSubProjectId, pipelineId = finalSubPipelineName
                    )?.pipelineName ?: ""
                }
                if (finalSubPipelineId.isNullOrBlank() || finalSubPipelineName.isEmpty()) {
                    logger.info(
                        "sub-pipeline not found|projectId:$projectId|subPipelineType:$subPipelineType|" +
                                "subProjectId:$subProjectId|subPipelineName:$subPipelineName"
                    )
                    return null
                }
                Triple(finalSubProjectId, finalSubPipelineId, finalSubPipelineName)
            }
        }
    }

    fun checkCircularDependency(
        projectId: String,
        pipelineId: String,
        existsPipeline: List<String>
    ): ElementCheckResult {
        // :TODO 递归校验
//        val subPipelineList = subPipelineRefDao.list(dslContext, projectId, pipelineId)
//        val intersect = existsPipeline.intersect(subPipelineList)
//        if (intersect.isNotEmpty()) {
//            return ElementCheckResult(
//                result = false,
//                errorTitle = "",
//                errorMessage = ""
//            )
//        }
        return ElementCheckResult(result = true)
    }
}
