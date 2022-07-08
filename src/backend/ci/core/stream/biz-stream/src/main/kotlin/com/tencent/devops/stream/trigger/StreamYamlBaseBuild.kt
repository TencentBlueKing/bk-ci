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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.api.service.ServiceWebhookBuildResource
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.webhook.WebhookTriggerParams
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.service.StreamWebsocketService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.expand.StreamYamlBuildExpand
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.pojo.ModelParametersData
import com.tencent.devops.stream.trigger.pojo.StreamBuildLock
import com.tencent.devops.stream.trigger.pojo.StreamTriggerLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.StreamCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamYamlBaseBuild @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamEventSaveService: StreamEventService,
    private val websocketService: StreamWebsocketService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamYamlBuildExpand: StreamYamlBuildExpand
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBaseBuild::class.java)
        private const val ymlVersion = "v2.0"
    }

    private val channelCode = ChannelCode.GIT

    private val buildRunningDesc = "Running."

    fun savePipeline(
        pipeline: StreamTriggerPipeline,
        userId: String,
        gitProjectId: Long,
        projectCode: String,
        modelAndSetting: PipelineModelAndSetting,
        updateLastModifyUser: Boolean
    ) {
        // 计算model的md值，缓存逻辑使用
        val md5 = StreamCommonUtils.getMD5(JsonUtil.toJson(modelAndSetting.model))
        val processClient = client.get(ServicePipelineResource::class)
        if (pipeline.pipelineId.isBlank()) {
            // 直接新建
            logger.info("create newpipeline: $pipeline")

            pipeline.pipelineId = processClient.create(
                userId = userId,
                projectId = projectCode,
                pipeline = modelAndSetting.model,
                channelCode = channelCode
            ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipeline = pipeline.toGitPipeline(),
                version = ymlVersion,
                md5 = md5
            )
            websocketService.pushPipelineWebSocket(
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                userId = userId
            )
        } else {
            val (oldMd5, displayName, version) = gitPipelineResourceDao.getLastEditMd5ById(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipelineId = pipeline.pipelineId
            )

            // md5不一致时不更新蓝盾的model
            if (oldMd5 != md5) {
                logger.info("${pipeline.pipelineId} use md5 cache")
                // 编辑流水线model
                processClient.edit(
                    userId = userId,
                    projectId = projectCode,
                    pipelineId = pipeline.pipelineId,
                    pipeline = modelAndSetting.model,
                    channelCode = channelCode,
                    updateLastModifyUser = updateLastModifyUser
                )
            }

            // 已有的流水线需要更新下Stream这里的状态
            if (oldMd5 != md5 || displayName != pipeline.displayName || version != ymlVersion) {
                logger.info("update gitPipeline pipeline: $pipeline")
                gitPipelineResourceDao.updatePipeline(
                    dslContext = dslContext,
                    gitProjectId = gitProjectId,
                    pipelineId = pipeline.pipelineId,
                    displayName = pipeline.displayName,
                    version = ymlVersion,
                    md5 = md5
                )
            }
        }

        processClient.saveSetting(
            userId = userId,
            projectId = projectCode,
            pipelineId = pipeline.pipelineId,
            setting = modelAndSetting.setting.copy(
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                pipelineName = modelAndSetting.model.name
            ),
            updateLastModifyUser = updateLastModifyUser,
            channelCode = channelCode
        )
    }

    fun createNewPipeLine(pipeline: StreamTriggerPipeline, projectCode: String, action: BaseAction) {
        // pipelineId可能为blank所以使用filePath为key
        val gitProjectId = pipeline.gitProjectId
        val triggerLock = StreamTriggerLock(
            redisOperation = redisOperation,
            gitProjectId = gitProjectId,
            filePath = pipeline.filePath
        )
        val realPipeline: StreamTriggerPipeline
        // 避免出现多个触发拿到空的pipelineId后依次进来创建，所以需要在锁后重新获取pipeline
        triggerLock.use {
            triggerLock.lock()
            realPipeline = getRealPipeLine(gitProjectId, pipeline)
            // 优先创建流水线为了前台显示
            if (realPipeline.pipelineId.isBlank()) {
                // 在蓝盾那边创建流水线
                savePipeline(
                    pipeline = realPipeline,
                    userId = pipeline.creator ?: "",
                    gitProjectId = gitProjectId.toLong(),
                    projectCode = projectCode,
                    modelAndSetting = StreamPipelineUtils.createEmptyPipelineAndSetting(projectCode),
                    updateLastModifyUser = true
                )
                streamPipelineBranchService.saveOrUpdate(
                    gitProjectId = action.data.getGitProjectId().toLong(),
                    pipelineId = realPipeline.pipelineId,
                    branch = action.data.eventCommon.branch
                )
            }
        }
    }

    fun getPipelineByFile(
        gitProjectId: Long,
        filePath: String
    ): TGitPipelineResourceRecord? {
        return gitPipelineResourceDao.getPipelineByFile(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            filePath = filePath
        )
    }

    private fun getRealPipeLine(gitProjectId: String, pipeline: StreamTriggerPipeline) =
        getPipelineByFile(
            gitProjectId = gitProjectId.toLong(),
            filePath = pipeline.filePath
        )?.let {
            StreamTriggerPipeline(it)
        } ?: pipeline

    protected fun preStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        model: Model,
        yamlTransferData: YamlTransferData? = null
    ) {
        // 【ID92537607】 stream 流水线标签不生效
        client.get(UserPipelineGroupResource::class).updatePipelineLabel(
            userId = action.data.getUserId(),
            projectId = action.getProjectCode(),
            pipelineId = pipeline.pipelineId,
            labelIds = model.labels
        )

        // 添加模板跨项目信息
        if (yamlTransferData != null && yamlTransferData.templateData.transferDataMap.isNotEmpty()) {
            client.get(ServiceTemplateAcrossResource::class).batchCreate(
                userId = action.data.getUserId(),
                projectId = action.getProjectCode(),
                pipelineId = pipeline.pipelineId,
                templateAcrossInfos = yamlTransferData.getTemplateAcrossInfo(action)
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    fun startBuild(
        pipeline: StreamTriggerPipeline,
        action: BaseAction,
        modelAndSetting: PipelineModelAndSetting,
        gitBuildId: Long,
        yamlTransferData: YamlTransferData? = null,
        updateLastModifyUser: Boolean,
        modelParameters: ModelParametersData
    ): BuildId? {
        logger.info("|${action.data.context.requestEventId}|startBuild|action|${action.format()}")

        preStartBuild(
            action = action,
            pipeline = pipeline,
            model = modelAndSetting.model,
            yamlTransferData = yamlTransferData
        )

        // 修改流水线并启动构建，需要加锁保证事务性
        val buildLock = StreamBuildLock(
            redisOperation = redisOperation,
            gitProjectId = action.data.getGitProjectId().toLong(),
            pipelineId = pipeline.pipelineId
        )
        var buildId = ""
        try {
            buildLock.lock()
            logger.info(
                "Stream Build start, gitProjectId[${action.data.getGitProjectId()}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]"
            )
            savePipeline(
                pipeline = pipeline,
                userId = action.data.getUserId(),
                gitProjectId = action.data.getGitProjectId().toLong(),
                projectCode = action.getProjectCode(),
                modelAndSetting = modelAndSetting,
                updateLastModifyUser = updateLastModifyUser
            )
            buildId = client.get(ServiceWebhookBuildResource::class).webhookTrigger(
                userId = action.data.getUserId(),
                projectId = action.getProjectCode(),
                pipelineId = pipeline.pipelineId,
                params = WebhookTriggerParams(
                    params = modelParameters.webHookParams,
                    startValues = mapOf(PIPELINE_NAME to pipeline.displayName)
                ),
                channelCode = channelCode,
                startType = StartType.SERVICE
            ).data!!
            logger.info(
                "Stream Build success, gitProjectId[${action.data.getGitProjectId()}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId], buildId[$buildId]"
            )
        } catch (ignore: Throwable) {
            errorStartBuild(
                action = action,
                pipeline = pipeline,
                gitBuildId = gitBuildId,
                ignore = ignore,
                yamlTransferData = yamlTransferData
            )
        } finally {
            buildLock.unlock()
            // 这里可以做构建相关的数据上报之类的工作
            streamYamlBuildExpand.buildReportData(action, buildId, pipeline)
        }

        if (buildId.isBlank()) {
            return null
        }

        afterStartBuild(
            action = action,
            pipeline = pipeline,
            buildId = buildId,
            gitBuildId = gitBuildId,
            yamlTransferData = yamlTransferData
        )

        return BuildId(buildId)
    }

    protected fun errorStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        gitBuildId: Long,
        ignore: Throwable,
        yamlTransferData: YamlTransferData?
    ) {
        logger.error(
            "Stream Build failed, gitProjectId[${action.data.getGitProjectId()}], " +
                "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]",
            ignore
        )
        // 清除已经保存的构建记录
        val build = gitRequestEventBuildDao.getByGitBuildId(dslContext, gitBuildId)
        if (build != null) gitRequestEventBuildDao.removeBuild(dslContext, gitBuildId)

        // 保存失败构建记录
        streamEventSaveService.saveBuildNotBuildEvent(
            action = action,
            reason = TriggerReason.PIPELINE_RUN_ERROR.name,
            reasonDetail = ignore.message ?: TriggerReason.PIPELINE_RUN_ERROR.detail,
            sendCommitCheck = action.needSendCommitCheck(),
            commitCheckBlock = (action.metaData.isStreamMr()),
            version = ymlVersion
        )

        // 删除模板跨项目信息
        if (yamlTransferData?.templateData != null) {
            client.get(ServiceTemplateAcrossResource::class).delete(
                projectId = action.getProjectCode(),
                pipelineId = pipeline.pipelineId,
                templateId = yamlTransferData.templateData.templateId,
                buildId = null
            )
        }
    }

    @Suppress("NestedBlockDepth")
    protected fun afterStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        buildId: String,
        gitBuildId: Long,
        yamlTransferData: YamlTransferData?
    ) {
        try {
            // 更新流水线和构建记录状态
            gitPipelineResourceDao.updatePipelineBuildInfo(dslContext, pipeline.toGitPipeline(), buildId, ymlVersion)
            gitRequestEventBuildDao.update(dslContext, gitBuildId, pipeline.pipelineId, buildId, ymlVersion)

            // 成功构建的添加 流水线-分支 记录
            if (action.needSaveOrUpdateBranch()) {
                streamPipelineBranchService.saveOrUpdate(
                    gitProjectId = action.data.getGitProjectId().toLong(),
                    pipelineId = pipeline.pipelineId,
                    branch = action.data.eventCommon.branch
                )
            }

            // 发送commitCheck
            if (action.data.setting.enableCommitCheck && action.needSendCommitCheck()) {
                // 发commit check 需要用到触发库的相关信息
                val streamGitProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                    gitProjectKey = action.data.eventCommon.gitProjectId,
                    action = action,
                    getProjectInfo = action.api::getGitProjectInfo
                )!!

                action.sendCommitCheck(
                    buildId = buildId,
                    gitProjectName = streamGitProjectInfo.name,
                    state = StreamCommitCheckState.PENDING,
                    block = action.data.setting.enableMrBlock &&
                        action.metaData.isStreamMr(),
                    context = "${pipeline.filePath}@${action.metaData.streamObjectKind.name}",
                    targetUrl = StreamPipelineUtils.genStreamV2BuildUrl(
                        homePage = streamGitConfig.streamUrl ?: throw ParamBlankException("启动配置缺少 streamUrl"),
                        gitProjectId = action.data.getGitProjectId(),
                        pipelineId = pipeline.pipelineId,
                        buildId = buildId
                    ),
                    description = buildRunningDesc
                )
            }

            // 更新跨项目模板信息
            if (yamlTransferData?.templateData != null) {
                client.get(ServiceTemplateAcrossResource::class).update(
                    projectId = action.getProjectCode(),
                    pipelineId = pipeline.pipelineId,
                    templateId = yamlTransferData.templateData.templateId,
                    buildId = buildId
                )
            }
        } catch (ignore: Exception) {
            logger.error(
                "Stream after Build failed, gitProjectId[${action.data.getGitProjectId()}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]",
                ignore
            )
        }
    }

    private fun YamlTransferData.getTemplateAcrossInfo(
        action: BaseAction
    ): List<BuildTemplateAcrossInfo> {
        val remoteProjectIdMap = mutableMapOf<String, MutableMap<TemplateAcrossInfoType, BuildTemplateAcrossInfo>>()
        templateData.transferDataMap.values.forEach { objectData ->
            val templateType = objectData.templateType.toAcrossType()!!
            val remoteProjectString = objectData.remoteProjectId

            if (remoteProjectString !in remoteProjectIdMap.keys) {
                // 将pathWithPathSpace转为数字id
                val remoteProjectIdLong = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                    gitProjectKey = remoteProjectString,
                    action = action,
                    getProjectInfo = action.api::getGitProjectInfo
                )?.gitProjectId?.let { "git_$it" } ?: return@forEach

                remoteProjectIdMap[remoteProjectString] = TemplateAcrossInfoType.values().associateWith {
                    BuildTemplateAcrossInfo(
                        templateId = templateData.templateId,
                        templateType = it,
                        templateInstancesIds = listOf(),
                        targetProjectId = remoteProjectIdLong
                    )
                }.toMutableMap()
            }

            val oldData = remoteProjectIdMap[remoteProjectString]!![templateType]!!

            remoteProjectIdMap[remoteProjectString]!![templateType] = oldData.copy(
                templateInstancesIds = mutableListOf<String>().apply {
                    addAll(oldData.templateInstancesIds)
                    add(objectData.objectId)
                }
            )
        }

        return remoteProjectIdMap.values.flatMap { it.values }.filter { it.templateInstancesIds.isNotEmpty() }
    }

    private fun TemplateType.toAcrossType(): TemplateAcrossInfoType? {
        return when (this) {
            TemplateType.STEP -> TemplateAcrossInfoType.STEP
            TemplateType.JOB -> TemplateAcrossInfoType.JOB
            else -> null
        }
    }
}
