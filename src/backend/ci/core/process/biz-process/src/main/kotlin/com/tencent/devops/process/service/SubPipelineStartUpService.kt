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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SubPipelineStartUpService(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineService: PipelineService,
    private val buildVariableService: BuildVariableService,
    private val buildService: PipelineBuildService,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineStartUpService::class.java)
    }

    /**
     * 启动子流水线方法，带入子流水线运行参数
     * @param projectId 流水线所在项目的ID，只能启动同一个项目下的子流水线
     * @param parentPipelineId 启动子流水线的流水线ID
     * @param buildId 本次流水线构建的ID
     * @param callPipelineId 子流水线ID
     * @param taskId 本次构建任务的ID
     * @param runMode 子流水线运行方式
     * @param values 子流水线启动参数
     */
    fun callPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        buildId: String,
        callProjectId: String = "",
        callPipelineId: String,
        atomCode: String,
        taskId: String,
        runMode: String,
        channelCode: ChannelCode? = null,
        values: Map<String, String>
    ): Result<ProjectBuildId> {
        val project = if (callProjectId.isNotBlank()) {
            callProjectId
        } else {
            projectId
        }

        // 通过 runVariables获取 userId 和 channelCode
        val runVariables = buildVariableService.getAllVariable(buildId)
        val userId =
            runVariables[PIPELINE_START_USER_ID] ?: runVariables[PipelineVarUtil.newVarToOldVar(PIPELINE_START_USER_ID)]
            ?: "null"
        val triggerUser =
            runVariables[PIPELINE_START_USER_NAME] ?: runVariables[PipelineVarUtil.newVarToOldVar(
                PIPELINE_START_USER_NAME
            )]
            ?: userId

        logger.info("[$buildId]|callPipelineStartup|$userId|$triggerUser|$project|$callProjectId|$projectId|$parentPipelineId|$callPipelineId|$taskId")
        val callChannelCode = channelCode ?: ChannelCode.valueOf(
            runVariables[PIPELINE_START_CHANNEL]
                ?: return MessageCodeUtil.generateResponseDataObject(
                    messageCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )
        )
        // 获取子流水线启动参数
        val startParams = mutableMapOf<String, Any>()
        values.forEach {
            startParams[it.key] = parseVariable(it.value, runVariables)
        }

        val existPipelines = HashSet<String>()
        existPipelines.add(parentPipelineId)
        try {
            checkSubpipeline(atomCode, project, callPipelineId, existPipelines)
        } catch (e: OperationException) {
            return MessageCodeUtil.generateResponseDataObject(ProcessMessageCode.ERROR_SUBPIPELINE_CYCLE_CALL)
        }

        val subBuildId = buildService.subpipelineStartup(
            userId = userId,
            startType = StartType.PIPELINE,
            projectId = project,
            parentPipelineId = parentPipelineId,
            parentBuildId = buildId,
            parentTaskId = taskId,
            pipelineId = callPipelineId,
            channelCode = callChannelCode,
            parameters = startParams,
            checkPermission = false,
            isMobile = false,
            triggerUser = triggerUser
        )
        pipelineBuildTaskDao.updateSubBuildId(
            dslContext = dslContext,
            buildId = buildId,
            taskId = taskId,
            subBuildId = subBuildId
        )

        return Result(
            ProjectBuildId(
                id = subBuildId,
                projectId = project
            )
        )
    }

    /**
     * 解析子流水线启动参数
     * @param value 子流水线启动参数
     * @param runVariables 本地运行时变量
     */
    fun parseVariable(value: String?, runVariables: Map<String, String>): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return EnvUtils.parseEnv(value!!, runVariables)
    }

    /**
     * 检查本次子流水线调用是否有循环调用问题，注意：
     * 在加入新的子流水线调用插件后，为防止新旧插件循环调用的情况，需要检查MarketBuildLessAtomElement和
     * SubPipelineCallElement两种类型的插件是否调用了当前流水线的情况
     * @param projectId 流水线所在项目的ID
     * @param pipelineId 子流水线ID
     * @param existPipelines 保存当前递归次时父流水线的ID
     */
    fun checkSubpipeline(atomCode: String, projectId: String, pipelineId: String, existPipelines: HashSet<String>) {

        if (existPipelines.contains(pipelineId)) {
            throw OperationException("子流水线不允许循环调用")
        }
        existPipelines.add(pipelineId)
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val existModel = pipelineRepositoryService.getModel(pipelineId, pipeline.version) ?: return

        val currentExistPipelines = HashSet(existPipelines)
        existModel.stages.forEachIndexed stage@{ index, stage ->
            if (index == 0) {
                // Ignore the trigger container
                return@stage
            }
            stage.containers.forEach container@{ container ->
                if (container !is NormalContainer) {
                    // 只在无构建环境中
                    return@container
                }

                container.elements.forEach element@{ element ->
                    // 只能是无构建环境插件
                    if (element !is MarketBuildLessAtomElement && element !is SubPipelineCallElement) {
                        return@element
                    }
                    if (element is MarketBuildLessAtomElement && element.getAtomCode() != atomCode) {
                        return@element
                    }
                    if (element is SubPipelineCallElement && element.subPipelineId.isBlank()) {
                        return@element
                    }

                    if (element is MarketBuildLessAtomElement) {
                        val map = element.data
                        val msg = map["input"] as? Map<*, *> ?: return@element
                        val subPip = msg["subPip"]
                        logger.info("callPipelineStartup: ${msg["projectId"]} $projectId")
                        val subPro =
                            if (msg["projectId"] == null || msg["projectId"].toString()
                                    .isBlank()
                            ) projectId else msg["projectId"]
                        val exist = HashSet(currentExistPipelines)
                        checkSubpipeline(atomCode, subPro as String, subPip as String, exist)
                        existPipelines.addAll(exist)
                    } else if (element is SubPipelineCallElement) {
                        val exist = HashSet(currentExistPipelines)
                        checkSubpipeline(atomCode, projectId, element.subPipelineId, exist)
                        existPipelines.addAll(exist)
                    }
                }
            }
        }
    }

    /**
     * 获取流水线的手动启动参数，返回至前端渲染界面。
     * @param userId 流水线启东人的用户ID
     * @param projectId 流水线所在项目ID
     * @param pipelineId 流水线ID
     */
    fun subpipManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<SubPipelineStartUpInfo>> {
        if (pipelineId.isBlank() || projectId.isBlank())
            return Result(ArrayList())
        val result = buildService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS)
        val parameter = ArrayList<SubPipelineStartUpInfo>()
        val prop = result.properties

        for (item in prop) {
            if (item.type == BuildFormPropertyType.MULTIPLE || item.type == BuildFormPropertyType.ENUM) {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                val defaultValue = item.defaultValue.toString()
                for (option in item.options!!) {
                    valueList.add(
                        StartUpInfo(
                            option.key,
                            option.value
                        )
                    )
                }
                val info = SubPipelineStartUpInfo(
                    key = item.id,
                    keyDisable = true,
                    keyType = "input",
                    keyListType = "",
                    keyUrl = "",
                    keyUrlQuery = ArrayList(),
                    keyList = keyList,
                    keyMultiple = false,
                    value = if (item.type == BuildFormPropertyType.MULTIPLE) {
                        if (defaultValue.isNullOrBlank()) {
                            ArrayList()
                        } else {
                            defaultValue.split(",")
                        }
                    } else {
                        defaultValue
                    },
                    valueDisable = false,
                    valueType = "select",
                    valueListType = "list",
                    valueUrl = "",
                    valueUrlQuery = ArrayList(),
                    valueList = valueList,
                    valueMultiple = if (item.type == BuildFormPropertyType.MULTIPLE) {
                        true
                    } else {
                        false
                    }
                )
                parameter.add(info)
            } else {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                val info = SubPipelineStartUpInfo(
                    key = item.id,
                    keyDisable = true,
                    keyType = "input",
                    keyListType = "",
                    keyUrl = "",
                    keyUrlQuery = ArrayList(),
                    keyList = keyList,
                    keyMultiple = false,
                    value = item.defaultValue,
                    valueDisable = false,
                    valueType = "input",
                    valueListType = "",
                    valueUrl = "",
                    valueUrlQuery = ArrayList(),
                    valueList = valueList,
                    valueMultiple = false
                )
                parameter.add(info)
            }
        }
        return Result(parameter)
    }

    fun getSubVar(buildId: String, taskId: String): Result<Map<String, String>> {
        logger.info("getSubVar | $buildId | $taskId")
        val taskRecord = pipelineBuildTaskDao.get(
            dslContext = dslContext,
            buildId = buildId,
            taskId = taskId
        ) ?: return Result(emptyMap())
        logger.info("getSubVar sub buildId :${taskRecord.subBuildId}")

        val subBuildId = taskRecord.subBuildId
        return Result(buildVariableService.getAllVariable(subBuildId))
    }

    fun getPipelineByName(projectId: String, pipelineName: String): Result<List<PipelineId?>> {
        val pipelines = pipelineService.getPipelineIdByNames(projectId, setOf(pipelineName), true)

        val data: MutableList<PipelineId?> = mutableListOf()
        if (pipelines.isNotEmpty()) {
            pipelines.forEach { (k, v) ->
                if (k == pipelineName) {
                    data.add(
                        PipelineId(
                            id = v
                        )
                    )
                }
            }
        }

        return Result(data)
    }
}