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

package com.tencent.devops.stream.trigger.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.ci.v2.YamlTransferData
import com.tencent.devops.common.ci.v2.enums.TemplateType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic.STREAM_BUILD_INFO_TOPIC
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.StreamBuildLock
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.isFork
import com.tencent.devops.stream.pojo.isMr
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.pojo.v2.StreamBuildInfo
import com.tencent.devops.stream.trigger.GitCIEventService
import com.tencent.devops.stream.trigger.GitCheckService
import com.tencent.devops.stream.trigger.StreamTriggerCache
import com.tencent.devops.stream.utils.CommitCheckUtils
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.stream.v2.service.StreamPipelineBranchService
import com.tencent.devops.stream.v2.service.StreamScmService
import com.tencent.devops.stream.v2.service.StreamWebsocketService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamYamlBaseBuild @Autowired constructor(
    private val client: Client,
    private val kafkaClient: KafkaClient,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val tokenService: StreamGitTokenService,
    private val gitCIEventSaveService: GitCIEventService,
    private val websocketService: StreamWebsocketService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val gitCheckService: GitCheckService,
    private val streamGitConfig: StreamGitConfig,
    private val streamGitTokenService: StreamGitTokenService,
    private val streamTriggerCache: StreamTriggerCache,
    private val oauthService: StreamOauthService,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBaseBuild::class.java)
        private const val ymlVersion = "v2.0"
    }

    private val channelCode = ChannelCode.GIT

    private val buildRunningDesc = "Running."

    fun savePipeline(
        pipeline: GitProjectPipeline,
        gitRequestEventForHandle: GitRequestEventForHandle,
        gitCIBasicSetting: GitCIBasicSetting,
        modelAndSetting: PipelineModelAndSetting,
        updateLastModifyUser: Boolean
    ) {
        val processClient = client.get(ServicePipelineResource::class)
        if (pipeline.pipelineId.isBlank()) {
            // 直接新建
            logger.info("create newpipeline: $pipeline")

            pipeline.pipelineId =
                processClient.create(
                    userId = gitRequestEventForHandle.userId,
                    projectId = gitCIBasicSetting.projectCode!!,
                    pipeline = modelAndSetting.model,
                    channelCode = channelCode
                ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitCIBasicSetting.gitProjectId,
                pipeline = pipeline,
                version = ymlVersion
            )
            websocketService.pushPipelineWebSocket(
                projectId = "git_${gitCIBasicSetting.gitProjectId}",
                pipelineId = pipeline.pipelineId,
                userId = gitRequestEventForHandle.userId
            )
        } else {
            // 编辑流水线model
            processClient.edit(
                userId = gitRequestEventForHandle.userId,
                projectId = gitCIBasicSetting.projectCode!!,
                pipelineId = pipeline.pipelineId,
                pipeline = modelAndSetting.model,
                channelCode = channelCode,
                updateLastModifyUser = updateLastModifyUser
            )
            // 已有的流水线需要更新下Stream这里的状态
            logger.info("update gitPipeline pipeline: $pipeline")
            gitPipelineResourceDao.updatePipeline(
                dslContext = dslContext,
                gitProjectId = gitCIBasicSetting.gitProjectId,
                pipelineId = pipeline.pipelineId,
                displayName = pipeline.displayName,
                version = ymlVersion
            )
        }
        processClient.saveSetting(
            userId = gitRequestEventForHandle.userId,
            projectId = gitCIBasicSetting.projectCode!!,
            pipelineId = pipeline.pipelineId,
            setting = modelAndSetting.setting.copy(
                projectId = gitCIBasicSetting.projectCode!!,
                pipelineId = pipeline.pipelineId,
                pipelineName = modelAndSetting.model.name
            ),
            updateLastModifyUser = updateLastModifyUser
        )
    }

    private fun preStartBuild(
        pipeline: GitProjectPipeline,
        gitRequestEventForHandle: GitRequestEventForHandle,
        gitCIBasicSetting: GitCIBasicSetting,
        model: Model,
        yamlTransferData: YamlTransferData? = null
    ) {
        // 【ID92537607】 stream 流水线标签不生效
        client.get(UserPipelineGroupResource::class).updatePipelineLabel(
            userId = gitRequestEventForHandle.userId,
            projectId = gitCIBasicSetting.projectCode!!,
            pipelineId = pipeline.pipelineId,
            labelIds = model.labels
        )

        // 添加模板跨项目信息
        if (yamlTransferData != null && yamlTransferData.templateData.transferDataMap.isNotEmpty()) {
            client.get(ServiceTemplateAcrossResource::class).batchCreate(
                userId = gitRequestEventForHandle.userId,
                projectId = gitCIBasicSetting.projectCode!!,
                pipelineId = pipeline.pipelineId,
                templateAcrossInfos = yamlTransferData.getTemplateAcrossInfo(
                    gitRequestEventId = gitRequestEventForHandle.id!!,
                    gitProjectId = gitRequestEventForHandle.gitProjectId
                )
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    fun startBuild(
        pipeline: GitProjectPipeline,
        gitRequestEventForHandle: GitRequestEventForHandle,
        gitCIBasicSetting: GitCIBasicSetting,
        modelAndSetting: PipelineModelAndSetting,
        gitBuildId: Long,
        yamlTransferData: YamlTransferData? = null,
        updateLastModifyUser: Boolean
    ): BuildId? {
        preStartBuild(
            pipeline = pipeline,
            gitRequestEventForHandle = gitRequestEventForHandle,
            gitCIBasicSetting = gitCIBasicSetting,
            model = modelAndSetting.model,
            yamlTransferData = yamlTransferData
        )

        // 修改流水线并启动构建，需要加锁保证事务性
        val buildLock = StreamBuildLock(
            redisOperation = redisOperation,
            gitProjectId = gitRequestEventForHandle.gitProjectId,
            pipelineId = pipeline.pipelineId
        )
        var buildId = ""
        try {
            buildLock.lock()
            logger.info(
                "Stream Build start, gitProjectId[${gitCIBasicSetting.gitProjectId}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]"
            )
            savePipeline(
                pipeline = pipeline,
                gitRequestEventForHandle = gitRequestEventForHandle,
                gitCIBasicSetting = gitCIBasicSetting,
                modelAndSetting = modelAndSetting,
                updateLastModifyUser = updateLastModifyUser
            )
            buildId = client.get(ServiceBuildResource::class).manualStartupNew(
                userId = gitRequestEventForHandle.userId,
                projectId = gitCIBasicSetting.projectCode!!,
                pipelineId = pipeline.pipelineId,
                values = mapOf(PIPELINE_NAME to pipeline.displayName),
                channelCode = channelCode,
                startType = StartType.SERVICE
            ).data!!.id
            logger.info(
                "Stream Build success, gitProjectId[${gitCIBasicSetting.gitProjectId}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId], buildId[$buildId]"
            )
        } catch (ignore: Throwable) {
            errorStartBuild(
                gitCIBasicSetting = gitCIBasicSetting,
                pipeline = pipeline,
                gitBuildId = gitBuildId,
                ignore = ignore,
                gitRequestEventForHandle = gitRequestEventForHandle,
                yamlTransferData = yamlTransferData
            )
        } finally {
            buildLock.unlock()
            if (buildId.isNotEmpty()) {
                kafkaClient.send(
                    STREAM_BUILD_INFO_TOPIC, JsonUtil.toJson(
                        StreamBuildInfo(
                            buildId = buildId,
                            streamYamlUrl = "${gitCIBasicSetting.homepage}/blob/" +
                                "${gitRequestEventForHandle.gitRequestEvent.commitId}/${pipeline.filePath}",
                            washTime = LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                        )
                    )
                )
            }
        }

        if (buildId.isBlank()) {
            return null
        }

        afterStartBuild(
            pipeline = pipeline,
            buildId = buildId,
            gitBuildId = gitBuildId,
            gitRequestEventForHandle = gitRequestEventForHandle,
            gitCIBasicSetting = gitCIBasicSetting,
            yamlTransferData = yamlTransferData
        )

        return BuildId(buildId)
    }

    private fun errorStartBuild(
        gitCIBasicSetting: GitCIBasicSetting,
        pipeline: GitProjectPipeline,
        gitBuildId: Long,
        ignore: Throwable,
        gitRequestEventForHandle: GitRequestEventForHandle,
        yamlTransferData: YamlTransferData?
    ) {
        logger.error(
            "Stream Build failed, gitProjectId[${gitCIBasicSetting.gitProjectId}], " +
                "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]",
            ignore
        )
        // 清除已经保存的构建记录
        val build = gitRequestEventBuildDao.getByGitBuildId(dslContext, gitBuildId)
        if (build != null) gitRequestEventBuildDao.removeBuild(dslContext, gitBuildId)

        // 保存失败构建记录
        gitCIEventSaveService.saveRunNotBuildEvent(
            userId = gitRequestEventForHandle.userId,
            eventId = gitRequestEventForHandle.id!!,
            pipelineId = pipeline.pipelineId,
            pipelineName = pipeline.displayName,
            filePath = pipeline.filePath,
            originYaml = build?.originYaml,
            normalizedYaml = build?.normalizedYaml,
            reason = TriggerReason.PIPELINE_RUN_ERROR.name,
            reasonDetail = ignore.message ?: TriggerReason.PIPELINE_RUN_ERROR.detail,
            gitProjectId = gitRequestEventForHandle.gitProjectId,
            sendCommitCheck = true,
            commitCheckBlock = (gitRequestEventForHandle.gitRequestEvent.objectKind ==
                TGitObjectKind.MERGE_REQUEST.value),
            version = ymlVersion,
            branch = gitRequestEventForHandle.branch
        )

        // 删除模板跨项目信息
        if (yamlTransferData?.templateData != null) {
            client.get(ServiceTemplateAcrossResource::class).delete(
                projectId = gitCIBasicSetting.projectCode!!,
                pipelineId = pipeline.pipelineId,
                templateId = yamlTransferData.templateData.templateId,
                buildId = null
            )
        }
    }

    @Suppress("NestedBlockDepth")
    private fun afterStartBuild(
        pipeline: GitProjectPipeline,
        buildId: String,
        gitBuildId: Long,
        gitRequestEventForHandle: GitRequestEventForHandle,
        gitCIBasicSetting: GitCIBasicSetting,
        yamlTransferData: YamlTransferData?
    ) {
        try {
            // 更新流水线和构建记录状态
            gitPipelineResourceDao.updatePipelineBuildInfo(dslContext, pipeline, buildId, ymlVersion)
            gitRequestEventBuildDao.update(dslContext, gitBuildId, pipeline.pipelineId, buildId, ymlVersion)
            // 成功构建的添加 流水线-分支 记录
            with(gitRequestEventForHandle) {
                if (needSaveOrUpdateBranch()) {
                    streamPipelineBranchService.saveOrUpdate(
                        gitProjectId = gitCIBasicSetting.gitProjectId,
                        pipelineId = pipeline.pipelineId,
                        branch = gitRequestEvent.branch
                    )
                }

                // 发送commitCheck
                if (CommitCheckUtils.needSendCheck(gitRequestEventForHandle.gitRequestEvent, gitCIBasicSetting)) {
                    // 发commit check 需要用到触发库的相关信息
                    val streamGitProjectInfo = with(gitRequestEventForHandle) {
                        streamTriggerCache.getAndSaveRequestGitProjectInfo(
                            gitRequestEventId = id!!,
                            gitProjectId = gitRequestEvent.gitProjectId.toString(),
                            token = streamGitTokenService.getToken(gitRequestEvent.gitProjectId),
                            useAccessToken = true,
                            getProjectInfo = streamScmService::getProjectInfoRetry
                        )
                    }
                    gitCheckService.pushCommitCheck(
                        streamGitProjectInfo = streamGitProjectInfo,
                        commitId = gitRequestEvent.commitId,
                        description = buildRunningDesc,
                        mergeRequestId = gitRequestEvent.mergeRequestId,
                        projectId = gitProjectId,
                        buildId = buildId,
                        userId = gitRequestEvent.userId,
                        status = GitCICommitCheckState.PENDING,
                        context = "${pipeline.filePath}@${gitRequestEvent.objectKind.toUpperCase()}",
                        targetUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                            homePage = streamGitConfig.tGitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                            gitProjectId = gitCIBasicSetting.gitProjectId,
                            pipelineId = pipeline.pipelineId,
                            buildId = buildId
                        ),
                        block = gitRequestEvent.objectKind == TGitObjectKind.MERGE_REQUEST.value &&
                            gitCIBasicSetting.enableMrBlock,
                        pipelineId = pipeline.pipelineId
                    )
                }
            }

            // 更新跨项目模板信息
            if (yamlTransferData?.templateData != null) {
                client.get(ServiceTemplateAcrossResource::class).update(
                    projectId = gitCIBasicSetting.projectCode!!,
                    pipelineId = pipeline.pipelineId,
                    templateId = yamlTransferData.templateData.templateId,
                    buildId = buildId
                )
            }
        } catch (ignore: Exception) {
            logger.error(
                "Stream after Build failed, gitProjectId[${gitCIBasicSetting.gitProjectId}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]",
                ignore
            )
        }
    }

    private fun GitRequestEventForHandle.needSaveOrUpdateBranch() =
        !gitRequestEvent.isFork() &&
            (gitRequestEvent.objectKind == TGitObjectKind.PUSH.value || gitRequestEvent.isMr()) &&
            !checkRepoTrigger

    private fun YamlTransferData.getTemplateAcrossInfo(
        gitRequestEventId: Long,
        gitProjectId: Long
    ): List<BuildTemplateAcrossInfo> {
        val remoteProjectIdMap = mutableMapOf<String, MutableMap<TemplateAcrossInfoType, BuildTemplateAcrossInfo>>()
        templateData.transferDataMap.values.forEach { objectData ->
            val templateType = objectData.templateType.toAcrossType()!!
            val remoteProjectString = objectData.remoteProjectId

            if (remoteProjectString !in remoteProjectIdMap.keys) {
                // 将pathWithPathSpace转为数字id
                val remoteProjectIdLong = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                    gitRequestEventId = gitRequestEventId,
                    gitProjectId = remoteProjectString,
                    token = oauthService.getGitCIEnableToken(gitProjectId).accessToken,
                    useAccessToken = true,
                    getProjectInfo = streamScmService::getProjectInfoRetry
                ).gitProjectId.toString().let { "git_$it" }

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
