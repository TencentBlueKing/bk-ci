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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_SUB_PIPELINE_NOT_ALLOWED_CIRCULAR_CALL
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("LongParameterList", "ComplexMethod", "ReturnCount", "NestedBlockDepth")
@Service
class SubPipelineStartUpService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val subPipelineStatusService: SubPipelineStatusService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelinePermissionService: PipelinePermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineStartUpService::class.java)
        private const val SYNC_RUN_MODE = "syn"
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
        val fixProjectId = callProjectId.ifBlank { projectId }

        // 通过 runVariables获取 userId 和 channelCode
        val runVariables = buildVariableService.getAllVariable(projectId, parentPipelineId, buildId)
        val userId =
            runVariables[PIPELINE_START_USER_ID] ?: runVariables[PipelineVarUtil.newVarToOldVar(PIPELINE_START_USER_ID)]
                ?: "null"
        val triggerUser =
            runVariables[PIPELINE_START_USER_NAME] ?: runVariables[
                PipelineVarUtil.newVarToOldVar(
                    PIPELINE_START_USER_NAME
                )
            ]
                ?: userId

        logger.info(
            "[$buildId]|callPipelineStartup|$userId|$triggerUser|$fixProjectId|$callProjectId" +
                "|$projectId|$parentPipelineId|$callPipelineId|$taskId"
        )
        val callChannelCode = channelCode ?: ChannelCode.valueOf(
            runVariables[PIPELINE_START_CHANNEL]
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId),
                    language = I18nUtil.getLanguage(userId)
                )
        )
        // 获取子流水线启动参数
        val startParams = mutableMapOf<String, String>()
        values.forEach {
            startParams[it.key] = parseVariable(it.value, runVariables)
        }

        val existPipelines = HashSet<String>()
        existPipelines.add(parentPipelineId)
        try {
            checkSub(atomCode, projectId = fixProjectId, pipelineId = callPipelineId, existPipelines = existPipelines)
        } catch (e: OperationException) {
            return I18nUtil.generateResponseDataObject(
                messageCode = ProcessMessageCode.ERROR_SUBPIPELINE_CYCLE_CALL,
                language = I18nUtil.getLanguage(userId)
            )
        }

        val subBuildId = subPipelineStartup(
            userId = userId,
            projectId = fixProjectId,
            parentProjectId = projectId,
            parentPipelineId = parentPipelineId,
            parentBuildId = buildId,
            parentTaskId = taskId,
            pipelineId = callPipelineId,
            channelCode = callChannelCode,
            parameters = startParams,
            triggerUser = triggerUser
        )
        pipelineTaskService.updateSubBuildId(
            projectId = projectId,
            buildId = buildId,
            taskId = taskId,
            subBuildId = subBuildId,
            subProjectId = fixProjectId
        )
        if (runMode == SYNC_RUN_MODE) {
            subPipelineStatusService.onStart(subBuildId)
        }
        return Result(
            ProjectBuildId(
                id = subBuildId,
                projectId = fixProjectId
            )
        )
    }

    private fun subPipelineStartup(
        userId: String,
        projectId: String,
        parentProjectId: String,
        parentPipelineId: String,
        parentBuildId: String,
        parentTaskId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        parameters: Map<String, String>,
        isMobile: Boolean = false,
        triggerUser: String? = null
    ): String {

        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                params = arrayOf(pipelineId),
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        val parentPipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = parentProjectId,
            pipelineId = parentPipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            params = arrayOf(parentPipelineId),
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
        )

        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId, pipelineId = pipelineId, version = readyToBuildPipelineInfo.version)

            val triggerContainer = model.stages[0].containers[0] as TriggerContainer
            // #6090 拨乱反正
            val params = buildParamCompatibilityTransformer.parseTriggerParam(triggerContainer.params, parameters)

            params[PIPELINE_START_PIPELINE_USER_ID] =
                BuildParameters(key = PIPELINE_START_PIPELINE_USER_ID, value = triggerUser ?: userId)
            params[PIPELINE_START_PARENT_PROJECT_ID] =
                BuildParameters(key = PIPELINE_START_PARENT_PROJECT_ID, value = parentProjectId)
            params[PIPELINE_START_PARENT_PIPELINE_ID] =
                BuildParameters(key = PIPELINE_START_PARENT_PIPELINE_ID, value = parentPipelineId)
            params[PIPELINE_START_PARENT_BUILD_ID] =
                BuildParameters(key = PIPELINE_START_PARENT_BUILD_ID, value = parentBuildId)
            params[PIPELINE_START_PARENT_BUILD_TASK_ID] =
                BuildParameters(key = PIPELINE_START_PARENT_BUILD_TASK_ID, value = parentTaskId)
            // 兼容子流水线插件按照名称调用,传递的参数没有在子流水线变量中声明，仍然可以传递
            parameters.forEach {
                if (!params.containsKey(it.key)) {
                    params[it.key] = BuildParameters(key = it.key, value = it.value)
                }
            }
            // 校验父流水线最后修改人是否有子流水线执行权限
            checkPermission(userId = parentPipelineInfo.lastModifyUser, projectId = projectId, pipelineId = pipelineId)

            // 子流水线的调用不受频率限制
            val subBuildId = pipelineBuildService.startPipeline(
                userId = readyToBuildPipelineInfo.lastModifyUser,
                pipeline = readyToBuildPipelineInfo,
                startType = StartType.PIPELINE,
                pipelineParamMap = params,
                channelCode = channelCode,
                isMobile = isMobile,
                model = model,
                frequencyLimit = false
            ).id
            // 更新父流水线关联子流水线构建id
            pipelineTaskService.updateSubBuildId(
                projectId = parentProjectId,
                buildId = parentBuildId,
                taskId = parentTaskId,
                subBuildId = subBuildId,
                subProjectId = readyToBuildPipelineInfo.projectId
            )
            return subBuildId
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to start sub-pipeline($pipelineId)")
        }
    }

    private fun getModel(projectId: String, pipelineId: String, version: Int? = null) =
        pipelineRepositoryService.getModel(projectId, pipelineId, version)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )

    /**
     * 解析子流水线启动参数
     * @param value 子流水线启动参数
     * @param runVariables 本地运行时变量
     */
    private fun parseVariable(value: String?, runVariables: Map<String, String>): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return EnvUtils.parseEnv(value, runVariables)
    }

    /**
     * 检查本次子流水线调用是否有循环调用问题，注意：
     * 在加入新的子流水线调用插件后，为防止新旧插件循环调用的情况，需要检查MarketBuildLessAtomElement和
     * SubPipelineCallElement两种类型的插件是否调用了当前流水线的情况
     * @param projectId 流水线所在项目的ID
     * @param pipelineId 子流水线ID
     * @param existPipelines 保存当前递归次时父流水线的ID
     */
    private fun checkSub(atomCode: String, projectId: String, pipelineId: String, existPipelines: HashSet<String>) {

        if (existPipelines.contains(pipelineId)) {
            logger.warn("subPipeline does not allow loop calls|projectId:$projectId|pipelineId:$pipelineId")
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_SUB_PIPELINE_NOT_ALLOWED_CIRCULAR_CALL,
                    params = arrayOf(projectId, pipelineId)
                )
            )
        }
        existPipelines.add(pipelineId)
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val existModel = pipelineRepositoryService.getModel(projectId, pipelineId, pipeline.version) ?: return

        val currentExistPipelines = HashSet(existPipelines)
        existModel.stages.forEachIndexed stage@{ index, stage ->
            if (index == 0) {
                // Ignore the trigger container
                return@stage
            }
            stage.containers.forEach container@{ container ->
                container.elements.forEach element@{ element ->
                    if (!needCheckSubElement(element, atomCode)) {
                        return@element
                    }

                    if (element is SubPipelineCallElement) {
                        val exist = HashSet(currentExistPipelines)
                        checkSub(atomCode, projectId, pipelineId = element.subPipelineId, existPipelines = exist)
                        existPipelines.addAll(exist)
                    } else {
                        val map = when (element) {
                            is MarketBuildLessAtomElement -> element.data
                            is MarketBuildAtomElement -> element.data
                            else -> return@element
                        }
                        val msg = map["input"] as? Map<*, *> ?: return@element
                        val subPip = msg["subPip"]?.toString() ?: return@element
                        logger.info(
                            "callPipelineStartup|" +
                                "supProjectId:${msg["projectId"]},subPipelineId:$subPip,subElementId:${element.id}," +
                                "parentProjectId:$projectId, parentPipelineId:$pipelineId"
                        )
                        val subProj = msg["projectId"]?.toString()?.ifBlank { projectId } ?: projectId
                        val exist = HashSet(currentExistPipelines)
                        checkSub(atomCode, projectId = subProj, pipelineId = subPip, existPipelines = exist)
                        existPipelines.addAll(exist)
                    }
                }
            }
        }
    }

    private fun needCheckSubElement(element: Element, atomCode: String): Boolean {
        return when {
            !element.isElementEnable() -> false
            (element is MarketBuildLessAtomElement || element is MarketBuildAtomElement) &&
                element.getAtomCode() != atomCode -> false
            element is SubPipelineCallElement && element.subPipelineId.isBlank() -> false
            else -> true
        }
    }

    fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE
            )
        ) {
            logger.info("sub-pipeline calling has no execution permission|$userId|$projectId|$pipelineId")
        }
    }

    /**
     * 获取流水线的手动启动参数，返回至前端渲染界面。
     * @param userId 流水线启东人的用户ID
     * @param projectId 流水线所在项目ID
     * @param pipelineId 流水线ID
     */
    fun subPipelineManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<SubPipelineStartUpInfo>> {
        if (pipelineId.isBlank() || projectId.isBlank()) {
            return Result(ArrayList())
        }
        val result = pipelineBuildFacadeService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS)
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
                        if (defaultValue.isBlank()) {
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
                    valueMultiple = item.type == BuildFormPropertyType.MULTIPLE
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

    fun getSubVar(projectId: String, buildId: String, taskId: String): Result<Map<String, String>> {
        val task = pipelineTaskService.getByTaskId(projectId = projectId, buildId = buildId, taskId = taskId)
            ?: return Result(emptyMap())

        logger.info("getSubVar sub build :${task.subBuildId}|${task.subProjectId}")

        val subBuildId = task.subBuildId ?: return Result(emptyMap())
        val subProjectId = task.subProjectId ?: return Result(emptyMap())
        val subPipelineId = pipelineRuntimeService.getBuildInfo(subProjectId, subBuildId)?.pipelineId
            ?: return Result(emptyMap())
        return Result(buildVariableService.getAllVariable(subProjectId, subPipelineId, subBuildId))
    }

    fun getPipelineByName(projectId: String, pipelineName: String): Result<List<PipelineId?>> {
        val pipelines = pipelineListFacadeService.getPipelineIdByNames(projectId, setOf(pipelineName), true)

        val data: MutableList<PipelineId?> = mutableListOf()
        if (pipelines.isNotEmpty()) {
            pipelines.forEach { (k, v) ->
                if (k == pipelineName) data.add(PipelineId(id = v))
            }
        }

        return Result(data)
    }

    fun getSubPipelineStatus(projectId: String, buildId: String): Result<SubPipelineStatus> {
        return Result(subPipelineStatusService.getSubPipelineStatus(projectId, buildId))
    }
}
