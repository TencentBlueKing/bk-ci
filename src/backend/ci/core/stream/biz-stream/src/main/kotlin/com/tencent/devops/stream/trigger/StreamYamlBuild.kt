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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.utils.PIPELINE_START_TIME_TRIGGER_USER_ID
import com.tencent.devops.process.yaml.modelCreate.ModelCreate
import com.tencent.devops.process.yaml.modelCreate.QualityRulesException
import com.tencent.devops.process.yaml.modelCreate.inner.GitData
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.modelCreate.inner.PipelineInfo
import com.tencent.devops.process.yaml.modelCreate.inner.StreamData
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.models.ResourcesPools
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.CROSS_PROJECT_REFERENCE_THIRD_PARTY_BUILD_POOL_ERROR
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.pojo.StreamDeleteEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamGitService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.modelCreate.ModelParameters
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.ModelParametersData
import com.tencent.devops.stream.trigger.pojo.StreamBuildLock
import com.tencent.devops.stream.trigger.pojo.StreamTriggerLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimer
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StreamYamlBuild @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamYamlBaseBuild: StreamYamlBaseBuild,
    private val redisOperation: RedisOperation,
    private val streamTimerService: StreamTimerService,
    private val deleteEventService: DeleteEventService,
    private val streamGitService: StreamGitService,
    private val streamTriggerCache: StreamTriggerCache,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val modelCreate: ModelCreate,
    private val streamGitConfig: StreamGitConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBuild::class.java)
        private val channelCode = ChannelCode.GIT

        // 手动触发参数获取，用来修改用户参数变量
        fun getInputParams(
            userVariables: Map<String, Variable>?,
            inputsData: Map<String, String>?
        ): List<BuildParameters>? {
            if (userVariables.isNullOrEmpty() || inputsData.isNullOrEmpty()) {
                return null
            }

            val result = mutableListOf<BuildParameters>()
            userVariables.forEach manualEach@{ (key, value) ->
                if (!inputsData.containsKey(key)) {
                    return@manualEach
                }

                // inputs包含，但是配置不允许改，直接报错
                if (value.allowModifyAtStartup != true) {
                    throw RuntimeException("variable $key not allow modify at startup")
                }

                // stream的用户变量会被默认填入 variables.
                val realKey = if (key.startsWith(ModelParameters.VARIABLE_PREFIX)) {
                    key
                } else {
                    ModelParameters.VARIABLE_PREFIX.plus(key)
                }

                result.add(
                    BuildParameters(
                        key = realKey,
                        value = inputsData[key]!!,
                        valueType = BuildFormPropertyType.STRING,
                        readOnly = value.readonly
                    )
                )
            }

            return result.ifEmpty { null }
        }
    }

    @Throws(StreamTriggerBaseException::class, ErrorCodeException::class)
    @SuppressWarnings("LongParameterList")
    fun gitStartBuild(
        action: BaseAction,
        triggerResult: TriggerResult,
        startParams: Map<String, String>,
        yaml: ScriptBuildYaml,
        gitBuildId: Long?,
        onlySavePipeline: Boolean,
        yamlTransferData: YamlTransferData?,
        manualInputs: Map<String, String>?
    ): BuildId? {
        logger.info(
            "StreamYamlBuild|gitStartBuild" +
                "|eventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        val pipeline = action.data.context.pipeline!!
        // pipelineId可能为blank所以使用filePath为key
        val triggerLock = StreamTriggerLock(
            redisOperation = redisOperation,
            gitProjectId = action.data.getGitProjectId(),
            filePath = pipeline.filePath
        )
        try {
            val realPipeline: StreamTriggerPipeline
            // 避免出现多个触发拿到空的pipelineId后依次进来创建，所以需要在锁后重新获取pipeline
            try {
                triggerLock.lock()
                realPipeline = pipelineResourceDao.getPipelineByFile(
                    dslContext = dslContext,
                    gitProjectId = action.data.getGitProjectId().toLong(),
                    filePath = pipeline.filePath
                )?.let {
                    StreamTriggerPipeline(it)
                } ?: pipeline
                // 优先创建流水线为了绑定红线
                if (realPipeline.pipelineId.isBlank()) {
                    streamYamlBaseBuild.savePipeline(
                        action = action,
                        pipeline = realPipeline,
                        userId = action.data.getUserId(),
                        gitProjectId = action.data.eventCommon.gitProjectId.toLong(),
                        projectCode = action.getProjectCode(),
                        modelAndSetting = StreamPipelineUtils.createEmptyPipelineAndSetting(realPipeline.displayName),
                        updateLastModifyUser = true
                    )
                }
            } finally {
                triggerLock.unlock()
            }

            // 改名时保存需要修改名称
            realPipeline.displayName = pipeline.displayName
            realPipeline.lastModifier = pipeline.lastModifier
            action.data.context.pipeline = realPipeline

            // 注册各种事件
            saveSpecialTriggerEvent(
                action = action,
                triggerResult = triggerResult,
                yaml = yaml
            )

            return if (gitBuildId != null) {
                startBuildPipeline(
                    action = action,
                    yaml = yaml,
                    gitBuildId = gitBuildId,
                    params = startParams,
                    yamlTransferData = yamlTransferData,
                    manualInputs = manualInputs
                )
            } else if (onlySavePipeline) {
                savePipeline(
                    action = action,
                    yaml = yaml
                )
                null
            } else {
                null
            }
        } catch (e: Throwable) {
            logger.warn("StreamYamlBuild|gitStartBuild|Fail to start the stream build(${action.format()})", e)
            val (block, message, reason) = when (e) {
                is JsonProcessingException, is ParamBlankException, is CustomException -> {
                    Triple(
                        (action.metaData.isStreamMr()),
                        e.message,
                        TriggerReason.PIPELINE_PREPARE_ERROR
                    )
                }
                is QualityRulesException -> {
                    Triple(
                        false,
                        e.message,
                        TriggerReason.CREATE_QUALITY_RULES_ERROR
                    )
                }
                // 指定异常直接扔出在外面统一处理
                is StreamTriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.warn("StreamYamlBuild|gitStartBuild|${action.data.context.requestEventId}|error", e)
                    Triple(false, e.message, TriggerReason.UNKNOWN_ERROR)
                }
            }
            throw StreamTriggerException(
                action = action,
                triggerReason = reason,
                reasonParams = listOf(message ?: ""),
                commitCheck = CommitCheck(
                    block = block,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
    }

    private fun saveSpecialTriggerEvent(
        action: BaseAction,
        triggerResult: TriggerResult,
        yaml: ScriptBuildYaml
    ) {
        val pipeline = action.data.context.pipeline!!
        // 如果是定时触发需要注册事件
        if (triggerResult.timeTrigger) {
            streamTimerService.saveTimer(
                StreamTimer(
                    projectId = action.getProjectCode(),
                    pipelineId = pipeline.pipelineId,
                    userId = action.data.getUserId(),
                    crontabExpressions = listOf(yaml.triggerOn?.schedules?.cron.toString()),
                    gitProjectId = action.data.getGitProjectId().toLong(),
                    // 未填写则在每次触发拉默认分支
                    branchs = yaml.triggerOn?.schedules?.branches?.ifEmpty {
                        listOf(action.data.context.defaultBranch!!)
                    } ?: listOf(action.data.context.defaultBranch!!),
                    always = yaml.triggerOn?.schedules?.always ?: false,
                    channelCode = channelCode,
                    eventId = action.data.context.requestEventId!!,
                    originYaml = action.data.context.originYaml!!
                )
            )
        }

        if (triggerResult.deleteTrigger && deleteEventService.getDeleteEvent(pipeline.pipelineId) == null) {
            deleteEventService.saveDeleteEvent(
                StreamDeleteEvent(
                    gitProjectId = action.data.getGitProjectId().toLong(),
                    pipelineId = pipeline.pipelineId,
                    userId = action.data.getUserId(),
                    eventId = action.data.context.requestEventId!!,
                    originYaml = action.data.context.originYaml!!
                )
            )
        }
        // 储存远程仓库触发的特殊事件
        if (!triggerResult.repoHookName.isNullOrEmpty()) {
            repoTriggerEventService.saveRepoTriggerEvent(
                targetGitProjectId = action.data.getGitProjectId().toLong(),
                sourceGitProjectPath = triggerResult.repoHookName,
                pipelineId = pipeline.pipelineId
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    private fun startBuildPipeline(
        action: BaseAction,
        yaml: ScriptBuildYaml,
        gitBuildId: Long,
        params: Map<String, String> = mapOf(),
        yamlTransferData: YamlTransferData?,
        manualInputs: Map<String, String>?
    ): BuildId? {
        logger.info(
            "StreamYamlBuild|startBuildPipeline" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        val pipeline = action.data.context.pipeline!!
        logger.info(
            "StreamYamlBuild|startBuildPipeline" +
                "|gitBuildId|$gitBuildId|pipeline|${pipeline.pipelineId}" +
                "|event|${action.data.context.requestEventId}"
        )

        val (modelCreateEvent, modelParams) = getModelCreateEventAndParams(
            action = action,
            yaml = yaml,
            webhookParams = params,
            yamlTransferData = yamlTransferData
        )

        // 获取并校验手动触发参数
        val manualValues = getInputParams(yaml.variables, manualInputs)

        // create or refresh pipeline
        val modelAndSetting = modelCreate.createPipelineModel(
            modelName = pipeline.displayName,
            event = modelCreateEvent,
            yaml = replaceYamlPoolName(yaml, action),
            pipelineParams = modelParams.userVariables,
            asCodeSettings = action.data.context.pipelineAsCodeSettings
        )
        // 判断是否更新最后修改人
        val updateLastModifyUser = action.needUpdateLastModifyUser(pipeline.filePath)
        // 兼容定时触发，取流水线最近修改人
        if (updateLastModifyUser && action.getStartType() == StartType.TIME_TRIGGER) {
            modelParams.webHookParams[PIPELINE_START_TIME_TRIGGER_USER_ID] = action.data.getUserId()
        }

        return streamYamlBaseBuild.startBuild(
            action = action,
            pipeline = pipeline,
            modelAndSetting = modelAndSetting,
            gitBuildId = gitBuildId,
            yamlTransferData = yamlTransferData,
            updateLastModifyUser = updateLastModifyUser,
            modelParameters = modelParams,
            manualValues = manualValues
        )
    }

    private fun savePipeline(
        action: BaseAction,
        yaml: ScriptBuildYaml
    ) {
        logger.info(
            "StreamYamlBuild|savePipeline|requestEventId" +
                "|${action.data.context.requestEventId}|action|${action.format()}"
        )

        val (modelCreateEvent, modelParams) = getModelCreateEventAndParams(
            action = action,
            yaml = yaml,
            webhookParams = mapOf(),
            yamlTransferData = null
        )
        val pipeline = action.data.context.pipeline!!
        val modelAndSetting = modelCreate.createPipelineModel(
            modelName = pipeline.displayName,
            event = modelCreateEvent,
            yaml = replaceYamlPoolName(yaml, action),
            pipelineParams = modelParams.userVariables,
            asCodeSettings = action.data.context.pipelineAsCodeSettings
        )
        logger.info(
            "StreamYamlBuild|savePipeline" +
                "|pipeline|${action.data.context.pipeline}|modelAndSetting|$modelAndSetting"
        )

        // 判断是否更新最后修改人
        val updateLastModifyUser = action.needUpdateLastModifyUser(pipeline.filePath)
        StreamBuildLock(
            redisOperation = redisOperation,
            gitProjectId = action.data.getGitProjectId().toLong(),
            pipelineId = pipeline.pipelineId
        ).use {
            it.lock()
            streamYamlBaseBuild.savePipeline(
                action = action,
                pipeline = pipeline,
                userId = action.data.getUserId(),
                gitProjectId = action.data.getGitProjectId().toLong(),
                projectCode = action.getProjectCode(),
                modelAndSetting = modelAndSetting,
                updateLastModifyUser = updateLastModifyUser
            )
        }
    }

    private fun getModelCreateEventAndParams(
        action: BaseAction,
        yaml: ScriptBuildYaml,
        webhookParams: Map<String, String>,
        yamlTransferData: YamlTransferData?
    ): Pair<ModelCreateEvent, ModelParametersData> {
        val streamGitProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = action.data.eventCommon.gitProjectId,
            action = action,
            getProjectInfo = action.api::getGitProjectInfo
        )!!

        val modelParams = ModelParameters.createPipelineParams(
            action = action,
            yaml = yaml,
            streamGitProjectInfo = streamGitProjectInfo,
            webhookParams = webhookParams,
            yamlTransferData = yamlTransferData
        )

        val modelCreateEvent = ModelCreateEvent(
            userId = action.data.getUserId(),
            projectCode = action.data.setting.projectCode!!,
            elementInstallUserId = action.data.setting.enableUser,
            pipelineInfo = PipelineInfo(action.data.context.pipeline!!.pipelineId),
            gitData = GitData(
                repositoryUrl = streamGitProjectInfo.gitHttpUrl,
                gitProjectId = streamGitProjectInfo.gitProjectId.toLong(),
                commitId = action.data.eventCommon.commit.commitId,
                branch = action.data.eventCommon.branch
            ),
            streamData = StreamData(
                gitProjectId = action.data.getGitProjectId().toLong(),
                enableUserId = action.data.setting.enableUser,
                requestEventId = action.data.context.requestEventId!!,
                objectKind = action.metaData.streamObjectKind
            ),
            changeSet = action.getChangeSet(),
            jobTemplateAcrossInfo = getJobTemplateAcrossInfo(yamlTransferData, action),
            checkIfModify = action.checkIfModify()
        )

        return Pair(modelCreateEvent, modelParams)
    }

    // 获取job级别的跨项目模板信息
    private fun getJobTemplateAcrossInfo(
        yamlTransferData: YamlTransferData?,
        action: BaseAction
    ): Map<String, BuildTemplateAcrossInfo>? {
        if (yamlTransferData == null) {
            return null
        }
        // 临时保存远程项目id的映射，就不用去redis里面查了
        val remoteProjectIdMap = mutableMapOf<String, String>()
        yamlTransferData.templateData.transferDataMap.values.forEach { objectData ->
            if (objectData.remoteProjectId in remoteProjectIdMap.keys) {
                return@forEach
            }
            // 将pathWithPathSpace转为数字id
            remoteProjectIdMap[objectData.remoteProjectId] = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitProjectKey = objectData.remoteProjectId,
                action = action,
                getProjectInfo = action.api::getGitProjectInfo
            )?.gitProjectId?.let { GitCommonUtils.getCiProjectId(it, streamGitConfig.getScmType()) } ?: return@forEach
        }

        val results = mutableMapOf<String, BuildTemplateAcrossInfo>()
        yamlTransferData.templateData.transferDataMap.filter { it.value.templateType == TemplateType.JOB }
            .forEach { (objectId, objectData) ->
                results[objectId] = BuildTemplateAcrossInfo(
                    templateId = yamlTransferData.templateData.templateId,
                    templateType = TemplateAcrossInfoType.JOB,
                    // 因为已经将jobId转为了map所以这里不保存，节省空间
                    templateInstancesIds = mutableListOf(),
                    targetProjectId = remoteProjectIdMap[objectData.remoteProjectId]!!
                )
            }
        return results
    }

    // 替换跨项目第三方构建机
    private fun replaceYamlPoolName(yaml: ScriptBuildYaml, action: BaseAction): ScriptBuildYaml {
        yaml.stages.forEach { stage ->
            stage.jobs.forEach { job ->
                job.runsOn.poolName = getEnvName(action, job.runsOn.poolName, yaml.resource?.pools)
            }
        }
        // 替换finally中的构建机
        yaml.finally?.forEach { fina ->
            fina.runsOn.poolName = getEnvName(action, fina.runsOn.poolName, yaml.resource?.pools)
        }
        return yaml
    }

    private fun getEnvName(action: BaseAction, poolName: String, pools: List<ResourcesPools>?): String {
        if (pools.isNullOrEmpty()) {
            return poolName
        }

        pools.filter { !it.from.isNullOrBlank() && !it.name.isNullOrBlank() }.forEach label@{
            if (it.name == poolName) {
                val repoNameAndPool = it.from!!.split("@")
                if (repoNameAndPool.size != 2 || repoNameAndPool[0].isBlank() || repoNameAndPool[1].isBlank()) {
                    return@label
                }

                val gitProjectInfo = streamGitService.getProjectInfo(repoNameAndPool[0])
                    ?: throw StreamTriggerException(
                        action,
                        TriggerReason.PIPELINE_PREPARE_ERROR,
                        listOf(
                            I18nUtil.getCodeLanMessage(
                                CROSS_PROJECT_REFERENCE_THIRD_PARTY_BUILD_POOL_ERROR,
                                I18nUtil.getDefaultLocaleLanguage()
                            )
                        )
                    )

                val result = GitCommonUtils.getCiProjectId(
                    "${gitProjectInfo.gitProjectId}@${repoNameAndPool[1]}",
                    streamGitConfig.getScmType()
                )

                logger.info("StreamYamlBuild|getEnvName|envName|$result")
                return result
            }
        }
        logger.info("StreamYamlBuild|getEnvName|no match. envName|$poolName")
        return poolName
    }
}
