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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.yaml.modelCreate.ModelCreate
import com.tencent.devops.process.yaml.modelCreate.QualityRulesException
import com.tencent.devops.process.yaml.modelCreate.inner.GitData
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.modelCreate.inner.PipelineInfo
import com.tencent.devops.process.yaml.modelCreate.inner.StreamData
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.models.ResourcesPools
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.pojo.StreamDeleteEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.modelCreate.ModelParameters
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.StreamTriggerLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimer
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
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
    private val streamTriggerCache: StreamTriggerCache,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val modelCreate: ModelCreate
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBuild::class.java)
        private val channelCode = ChannelCode.GIT
    }

    @Throws(StreamTriggerBaseException::class, ErrorCodeException::class)
    @SuppressWarnings("LongParameterList")
    fun gitStartBuild(
        action: BaseAction,
        triggerResult: TriggerResult,
        yaml: ScriptBuildYaml,
        gitBuildId: Long?,
        onlySavePipeline: Boolean,
        yamlTransferData: YamlTransferData?
    ): BuildId? {
        logger.info("|${action.data.context.requestEventId}|gitStartBuild|action|${action.format()}")

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
                        pipeline = realPipeline,
                        userId = action.data.getUserId(),
                        gitProjectId = action.data.eventCommon.gitProjectId.toLong(),
                        projectCode = action.getProjectCode(),
                        modelAndSetting = createTriggerModel(action.getProjectCode()),
                        updateLastModifyUser = true
                    )
                }
            } finally {
                triggerLock.unlock()
            }

            // 改名时保存需要修改名称
            realPipeline.displayName = pipeline.displayName
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
                    params = triggerResult.startParams,
                    yamlTransferData = yamlTransferData
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
            logger.warn("Fail to start the stream build(${action.format()})", e)
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
                        TriggerReason.CREATE_QUALITY_RULRS_ERROR
                    )
                }
                // 指定异常直接扔出在外面统一处理
                is StreamTriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.error("gitStartBuild|event: ${action.data.context.requestEventId} unknow error", e)
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

    private fun createTriggerModel(projectCode: String) = PipelineModelAndSetting(
        model = Model(
            name = StreamPipelineUtils.genBKPipelineName(projectCode),
            desc = "",
            stages = listOf(
                Stage(
                    id = VMUtils.genStageId(1),
                    name = VMUtils.genStageId(1),
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "构建触发",
                            elements = listOf(
                                ManualTriggerElement(
                                    name = "手动触发",
                                    id = "T-1-1-1"
                                )
                            )
                        )
                    )
                )
            )
        ),
        setting = PipelineSetting()
    )

    @SuppressWarnings("LongParameterList")
    private fun startBuildPipeline(
        action: BaseAction,
        yaml: ScriptBuildYaml,
        gitBuildId: Long,
        params: Map<String, String> = mapOf(),
        yamlTransferData: YamlTransferData?
    ): BuildId? {
        logger.info("|${action.data.context.requestEventId}|startBuildPipeline|action|${action.format()}")

        val pipeline = action.data.context.pipeline!!
        logger.info(
            "Git request gitBuildId:$gitBuildId, pipeline:${pipeline.pipelineId}," +
                " event: ${action.data.context.requestEventId}"
        )

        val (modelCreateEvent, modelParams) = getModelCreateEventAndParams(
            action = action,
            yaml = yaml,
            webhookParams = params,
            yamlTransferData = yamlTransferData
        )

        // create or refresh pipeline
        val modelAndSetting = modelCreate.createPipelineModel(
            modelName = StreamPipelineUtils.genBKPipelineName(action.getProjectCode()),
            event = modelCreateEvent,
            yaml = replaceYamlPoolName(yaml, action),
            pipelineParams = modelParams
        )
        logger.info("startBuildPipeline gitBuildId:$gitBuildId, pipeline:$pipeline, modelAndSetting: $modelAndSetting")

        // 判断是否更新最后修改人
        val changeSet = action.getChangeSet()
        val updateLastModifyUser = !changeSet.isNullOrEmpty() && changeSet.contains(pipeline.filePath)

        return streamYamlBaseBuild.startBuild(
            action = action,
            pipeline = pipeline,
            modelAndSetting = modelAndSetting,
            gitBuildId = gitBuildId,
            yamlTransferData = yamlTransferData,
            updateLastModifyUser = updateLastModifyUser
        )
    }

    private fun savePipeline(
        action: BaseAction,
        yaml: ScriptBuildYaml
    ) {
        logger.info("|${action.data.context.requestEventId}|savePipeline|action|${action.format()}")

        val (modelCreateEvent, modelParams) = getModelCreateEventAndParams(
            action = action,
            yaml = yaml,
            webhookParams = mapOf(),
            yamlTransferData = null
        )

        val modelAndSetting = modelCreate.createPipelineModel(
            modelName = StreamPipelineUtils.genBKPipelineName(action.getProjectCode()),
            event = modelCreateEvent,
            yaml = replaceYamlPoolName(yaml, action),
            pipelineParams = modelParams
        )
        logger.info("savePipeline pipeline:${action.data.context.pipeline}, modelAndSetting: $modelAndSetting")

        // 判断是否更新最后修改人
        val pipeline = action.data.context.pipeline!!
        val changeSet = action.getChangeSet()
        val updateLastModifyUser = !changeSet.isNullOrEmpty() && changeSet.contains(pipeline.filePath)

        streamYamlBaseBuild.savePipeline(
            pipeline = pipeline,
            userId = action.data.getUserId(),
            gitProjectId = action.data.getGitProjectId().toLong(),
            projectCode = action.getProjectCode(),
            modelAndSetting = modelAndSetting,
            updateLastModifyUser = updateLastModifyUser
        )
    }

    private fun getModelCreateEventAndParams(
        action: BaseAction,
        yaml: ScriptBuildYaml,
        webhookParams: Map<String, String>,
        yamlTransferData: YamlTransferData?
    ): Pair<ModelCreateEvent, List<BuildFormProperty>> {
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
            jobTemplateAcrossInfo = getJobTemplateAcrossInfo(yamlTransferData, action)
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
            )?.gitProjectId?.let { "git_$it" } ?: return@forEach
        }

        val results = mutableMapOf<String, BuildTemplateAcrossInfo>()
        yamlTransferData.templateData.transferDataMap.filter { it.value.templateType == TemplateType.JOB }
            .forEach { (objectId, objectData) ->
                results[objectId] = BuildTemplateAcrossInfo(
                    templateId = yamlTransferData.templateData.templateId,
                    templateType = TemplateAcrossInfoType.JOB,
                    // 因为已经将jobId转为了map所以这里不保存，节省空间
                    templateInstancesIds = emptyList(),
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
        return yaml
    }

    private fun getEnvName(action: BaseAction, poolName: String, pools: List<ResourcesPools>?): String {
        if (pools.isNullOrEmpty()) {
            return poolName
        }

        pools.filter { !it.from.isNullOrBlank() && !it.name.isNullOrBlank() }.forEach label@{
            if (it.name == poolName) {
                try {
                    val repoNameAndPool = it.from!!.split("@")
                    if (repoNameAndPool.size != 2 || repoNameAndPool[0].isBlank() || repoNameAndPool[1].isBlank()) {
                        return@label
                    }

                    val gitProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                        gitProjectKey = repoNameAndPool[0],
                        action = action,
                        getProjectInfo = action.api::getGitProjectInfo
                    )!!

                    val result = "git_${gitProjectInfo.gitProjectId}@${repoNameAndPool[1]}"

                    logger.info("Get envName from Resource.pools success. envName: $result")
                    return result
                } catch (e: Exception) {
                    logger.error("Get projectInfo from git failed, envName: $poolName. exception:", e)
                    return poolName
                }
            }
        }
        logger.info("Get envName from Resource.pools no match. envName: $poolName")
        return poolName
    }
}
