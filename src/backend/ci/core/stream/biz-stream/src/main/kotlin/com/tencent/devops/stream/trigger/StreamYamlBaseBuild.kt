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

import com.devops.process.yaml.v2.enums.TemplateType
import com.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.utils.PIPELINE_NAME
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
import com.tencent.devops.stream.trigger.pojo.StreamBuildLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.StreamEventService
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
        model: Model,
        updateLastModifyUser: Boolean
    ) {
        val processClient = client.get(ServicePipelineResource::class)
        if (pipeline.pipelineId.isBlank()) {
            // 直接新建
            logger.info("create newpipeline: $pipeline")

            pipeline.pipelineId = processClient.create(
                userId = userId,
                projectId = projectCode,
                pipeline = model,
                channelCode = channelCode
            ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipeline = pipeline.toGitPipeline(),
                version = ymlVersion
            )
            websocketService.pushPipelineWebSocket(
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                userId = userId
            )
        } else {
            // 编辑流水线model
            processClient.edit(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                pipeline = model,
                channelCode = channelCode,
                updateLastModifyUser = updateLastModifyUser
            )
            // 已有的流水线需要更新下Stream这里的状态
            logger.info("update gitPipeline pipeline: $pipeline")
            gitPipelineResourceDao.updatePipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipelineId = pipeline.pipelineId,
                displayName = pipeline.displayName,
                version = ymlVersion
            )
        }
    }

    protected fun preStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        model: Model,
        yamlTransferData: YamlTransferData? = null
    ) {
        // 【ID92537607】 stream 流水线标签不生效
        client.get(UserPipelineGroupResource::class).updatePipelineLabel(
            userId = action.data.eventCommon.userId,
            projectId = action.getProjectCode(),
            pipelineId = pipeline.pipelineId,
            labelIds = model.labels
        )

        // 添加模板跨项目信息
        if (yamlTransferData != null && yamlTransferData.templateData.transferDataMap.isNotEmpty()) {
            client.get(ServiceTemplateAcrossResource::class).batchCreate(
                userId = action.data.eventCommon.userId,
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
        model: Model,
        gitBuildId: Long,
        yamlTransferData: YamlTransferData? = null,
        updateLastModifyUser: Boolean
    ): BuildId? {
        logger.info("|${action.data.context.requestEventId}|startBuild|action|${action.format()}")

        preStartBuild(
            action = action,
            pipeline = pipeline,
            model = model,
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
                userId = action.data.eventCommon.userId,
                gitProjectId = action.data.getGitProjectId().toLong(),
                projectCode = action.getProjectCode(),
                model = model,
                updateLastModifyUser = updateLastModifyUser
            )
            buildId = client.get(ServiceBuildResource::class).manualStartupNew(
                userId = action.data.eventCommon.userId,
                projectId = action.getProjectCode(),
                pipelineId = pipeline.pipelineId,
                values = mapOf(PIPELINE_NAME to pipeline.displayName),
                channelCode = channelCode,
                startType = StartType.SERVICE
            ).data!!.id
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
            streamYamlBuildExpand.buildReportData(action, buildId)
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
                )

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
                    remoteProjectString,
                    action,
                    action.api::getGitProjectInfo
                ).gitProjectId.let { "git_$it" }

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
