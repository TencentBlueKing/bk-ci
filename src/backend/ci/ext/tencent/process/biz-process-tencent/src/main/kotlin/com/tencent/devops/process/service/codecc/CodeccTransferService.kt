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

package com.tencent.devops.process.service.codecc

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.dao.TencentPipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.service.PipelineInfoFacadeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.min

@Service@Suppress("ALL")
class CodeccTransferService @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineInfoDao: PipelineInfoDao,
    private val tencentPipelineBuildDao: TencentPipelineBuildDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeccTransferService::class.java)
    }

    fun addToolSetToPipeline(
        projectId: String,
        pipelineIds: Set<String>?,
        toolRuleSet: String,
        language: ProjectLanguage = ProjectLanguage.C_CPP
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val limit = 10
        var offset = 0
        var breakFlag = false
        val signPipelineIdList = pipelineIds?.toList() ?: emptyList()
        do {
            var pipelineIdList: List<String>? = null
            if (signPipelineIdList.isNotEmpty()) {
                if (offset >= signPipelineIdList.size) {
                    breakFlag = true
                } else {
                    pipelineIdList = signPipelineIdList.subList(
                        fromIndex = offset, toIndex = min((offset + limit), signPipelineIdList.size)
                    )
                    if (pipelineIdList.size < limit) {
                        breakFlag = true
                    }
                }
            } else {
                val tmpList = pipelineInfoDao.listPipelineInfoByProject(
                    dslContext = dslContext, projectId = projectId, limit = limit, offset = offset
                )
                if (tmpList == null || tmpList.isEmpty()) {
                    breakFlag = true
                } else {
                    pipelineIdList = tmpList.filter { it.channel == ChannelCode.BS.name }.map { it.pipelineId }
                        .toSet().toList()
                }
            }
            if (pipelineIdList != null && pipelineIdList.isNotEmpty()) {
                pipelineTaskService.list(projectId, pipelineIdList).map {
                    val resultMsg = try {
                        doAddToolSetToPipeline(projectId, it, toolRuleSet)
                    } catch (e: Exception) {
                        logger.error("add pipeline ${it.key} rule($toolRuleSet) fail", e)
                        e.message ?: "unexpected error occur"
                    }
                    result[it.key] = resultMsg
                }
            }
            offset += limit
        } while (!breakFlag)
        return result
    }

    private fun doAddToolSetToPipeline(
        projectId: String,
        it: Map.Entry<String, List<PipelineModelTask>>,
        toolRuleSet: String,
        language: ProjectLanguage = ProjectLanguage.C_CPP
    ): String {
        val toolRuleSetName = language.name + "_RULE"

        val pipelineId = it.key

        val newCodeccTask = it.value.filter { task -> task.taskParams["atomCode"] == "CodeccCheckAtom" }
        if (newCodeccTask.isEmpty()) {
            return "$pipelineId do not contains new codecc element"
        }

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)!!
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        logger.info("get pipeline info for pipeline: $pipelineId, $pipelineInfo")
        var needUpdate = false
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    if (element.getAtomCode() == "CodeccCheckAtom") {
                        logger.info("get new codecc element for pipeline: $pipelineId")
                        val newElement = element as MarketBuildAtomElement
                        val input = newElement.data["input"] as Map<String, Any>
                        val languages = input["languages"] as List<String>
                        if (languages.contains(language.name)) {
                            needUpdate = true

                            // add the first place
                            val languageRuleSetMap = input["languageRuleSetMap"] as MutableMap<String, List<String>>
                            val langRule = languageRuleSetMap[toolRuleSetName] as? MutableList<String>
                            if (langRule == null) {
                                languageRuleSetMap["toolRuleSetName"] = listOf(toolRuleSet)
                            } else if (!langRule.contains(toolRuleSet)) {
                                langRule.add(toolRuleSet)
                            }

                            // add the second place
                            val langRule2 = input[toolRuleSetName] as MutableList<String>
                            if (!langRule2.contains(toolRuleSet)) langRule2.add(toolRuleSet)

                            logger.info("update pipieline rule list: $langRule\n $langRule2")
                        }
                    }
                }
            }
        }

        if (!needUpdate) return "do not contains $language language, do not update"

        // save pipeline
        logger.info("edit pipeline: $pipelineId")
        pipelineInfoFacadeService.editPipeline(
            userId = pipelineInfo?.lastModifyUser ?: "",
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            channelCode = pipelineInfo?.channelCode ?: ChannelCode.BS,
            checkPermission = false,
            checkTemplate = false
        )

        return "add rule($toolRuleSet) to pipeline($pipelineId) success"
    }

    fun getHistoryBuildScan(
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?
    ): List<BuildBasicInfo> {
        var queueTimeStartTimeTemp = queueTimeStartTime
        val dayTimeMillis = 24 * 60 * 60 * 1000
        if (queueTimeStartTime != null && queueTimeStartTime > 0 && queueTimeEndTime != null && queueTimeEndTime > 0) {
            if (queueTimeEndTime - queueTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                queueTimeStartTimeTemp = queueTimeEndTime - dayTimeMillis
            }
        }

        var startTimeStartTimeTemp = startTimeStartTime
        if (startTimeStartTime != null && startTimeStartTime > 0 && startTimeEndTime != null && startTimeEndTime > 0) {
            if (startTimeEndTime - startTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                startTimeStartTimeTemp = startTimeEndTime - dayTimeMillis
            }
        }

        var endTimeStartTimeTemp = endTimeStartTime
        if (endTimeStartTime != null && endTimeStartTime > 0 && endTimeEndTime != null && endTimeEndTime > 0) {
            if (endTimeEndTime - endTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                endTimeStartTimeTemp = endTimeEndTime - dayTimeMillis
            }
        }

        val list = tencentPipelineBuildDao.listScanPipelineBuildList(
            dslContext,
            status,
            trigger,
            queueTimeStartTimeTemp,
            queueTimeEndTime,
            startTimeStartTimeTemp,
            startTimeEndTime,
            endTimeStartTimeTemp,
            endTimeEndTime
        )
        val result = mutableListOf<BuildBasicInfo>()
        val buildIds = mutableSetOf<String>()
        list.forEach {
            val buildId = it.buildId
            if (buildIds.contains(buildId)) {
                return@forEach
            }
            buildIds.add(buildId)
            result.add(genBuildBaseInfo(it))
        }
        return result
    }

    private fun genBuildBaseInfo(
        tPipelineBuildHistoryRecord: TPipelineBuildHistoryRecord
    ): BuildBasicInfo {
        return with(tPipelineBuildHistoryRecord) {
            BuildBasicInfo(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = version
            )
        }
    }
}
