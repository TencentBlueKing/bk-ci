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
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCommitFinishEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.process.api.service.ServicePipelineBuildCommitResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.api.service.ServiceWebhookBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.webhook.WebhookTriggerParams
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.STARTUP_CONFIG_MISSING
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.GitRequestRepoEventDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.message.UserMessageType
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.service.StreamWebsocketService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.expand.StreamYamlBuildExpand
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.pojo.ModelParametersData
import com.tencent.devops.stream.trigger.pojo.StreamBuildLock
import com.tencent.devops.stream.trigger.pojo.StreamTriggerLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.GitCommonUtils
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
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestRepoEventDao: GitRequestRepoEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamEventSaveService: StreamEventService,
    private val websocketService: StreamWebsocketService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamYamlBuildExpand: StreamYamlBuildExpand,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBaseBuild::class.java)
        private const val ymlVersion = "v2.0"
        private const val BUILD_COMMIT_PAGE_SIZE = 200
        private const val STREAM_MODEL_MD5_CACHE_PROJECTS_KEY = "stream:model.md5.cache:project:list"
    }

    private val channelCode = ChannelCode.GIT

    private val buildRunningDesc = "Running."

    fun savePipeline(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        userId: String,
        gitProjectId: Long,
        projectCode: String,
        modelAndSetting: PipelineModelAndSetting,
        updateLastModifyUser: Boolean
    ) {
        val processClient = client.get(ServicePipelineResource::class)
        try {
            if (pipeline.pipelineId.isBlank()) {
                // 直接新建
                logger.info("StreamYamlBaseBuild|savePipeline|create newpipeline|$pipeline")

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
                    md5 = null
                )
                websocketService.pushPipelineWebSocket(
                    projectId = projectCode,
                    pipelineId = pipeline.pipelineId,
                    userId = userId
                )
            } else if (confirmProjectUseModelMd5Cache(projectCode)) {
                // 计算model的md值，缓存逻辑使用
                val md5 = calculateModelMd5(modelAndSetting.model)
                // 开启了md5缓存的项目
                val (oldMd5, displayName, version) = gitPipelineResourceDao.getLastEditMd5ById(
                    dslContext = dslContext,
                    gitProjectId = gitProjectId,
                    pipelineId = pipeline.pipelineId
                )

                // md5不一致时更新蓝盾的model
                if (oldMd5 != md5) {
                    // 编辑流水线model
                    processClient.edit(
                        userId = userId,
                        projectId = projectCode,
                        pipelineId = pipeline.pipelineId,
                        pipeline = modelAndSetting.model,
                        channelCode = channelCode,
                        updateLastModifyUser = updateLastModifyUser
                    )
                } else {
                    logger.info("${pipeline.pipelineId} use md5 cache")
                }

                // 已有的流水线需要更新下Stream这里的状态
                if (oldMd5 != md5 || displayName != pipeline.displayName || version != ymlVersion) {
                    logger.info("StreamYamlBaseBuild|savePipeline|update pipeline|$pipeline")
                    gitPipelineResourceDao.updatePipeline(
                        dslContext = dslContext,
                        gitProjectId = gitProjectId,
                        pipelineId = pipeline.pipelineId,
                        displayName = pipeline.displayName,
                        version = ymlVersion,
                        md5 = md5
                    )
                }
            } else {
                // 编辑流水线model
                processClient.edit(
                    userId = userId,
                    projectId = projectCode,
                    pipelineId = pipeline.pipelineId,
                    pipeline = modelAndSetting.model,
                    channelCode = channelCode,
                    updateLastModifyUser = updateLastModifyUser
                )
                // 已有的流水线需要更新下Stream这里的状态
                logger.info("StreamYamlBaseBuild|savePipeline|update pipeline|$pipeline")
                gitPipelineResourceDao.updatePipeline(
                    dslContext = dslContext,
                    gitProjectId = gitProjectId,
                    pipelineId = pipeline.pipelineId,
                    displayName = pipeline.displayName,
                    version = ymlVersion,
                    md5 = null
                )
            }
            processClient.saveSetting(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                setting = modelAndSetting.setting.copy(
                    projectId = projectCode,
                    pipelineId = pipeline.pipelineId,
                    pipelineName = modelAndSetting.model.name,
                    maxConRunningQueueSize = null
                ),
                updateLastModifyUser = updateLastModifyUser,
                channelCode = channelCode
            )
        } catch (e: Throwable) {
            logger.warn("StreamYamlBaseBuild|savePipeline|failed|error|${e.message}")
            throw StreamTriggerException(
                action = action,
                triggerReason = TriggerReason.SAVE_PIPELINE_FAILED,
                reasonParams = listOf(e.message ?: "")
            )
        }
    }

    // 计算蓝盾model的md5
    private fun calculateModelMd5(model: Model): String? {
        // 需要在计算前先做一次深拷贝
        val modelJ = JsonUtil.toJson(model, false)
        val nModel = JsonUtil.to(modelJ, Model::class.java)

        // 之后将model中插件的ID置位空，因为用户无法定义插件ID，所以有的肯定都是随机生成的，md5对比会受影响
        nModel.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    element.id = null
                }
            }
        }
        return StreamCommonUtils.getMD5(JsonUtil.toJson(nModel, false))
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
                    action = action,
                    pipeline = realPipeline,
                    userId = pipeline.creator ?: "",
                    gitProjectId = gitProjectId.toLong(),
                    projectCode = projectCode,
                    modelAndSetting = StreamPipelineUtils.createEmptyPipelineAndSetting(pipeline.displayName),
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

    private fun preStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        model: Model,
        yamlTransferData: YamlTransferData? = null
    ) {
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
        modelParameters: ModelParametersData,
        manualValues: List<BuildParameters>?
    ): BuildId? {
        logger.info(
            "StreamYamlBaseBuild|startBuild" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        preStartBuild(
            action = action,
            pipeline = pipeline,
            model = modelAndSetting.model,
            yamlTransferData = yamlTransferData
        )
        // 更新yaml变更列表到db
        val forkMrYamlList = action.forkMrYamlList()
        if (forkMrYamlList.isNotEmpty()) {
            gitRequestEventDao.updateChangeYamlList(dslContext, action.data.context.requestEventId!!, forkMrYamlList)
        }

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
                "StreamYamlBaseBuild|startBuild|start|gitProjectId|${action.data.getGitProjectId()}|" +
                    "pipelineId|${pipeline.pipelineId}|gitBuildId|$gitBuildId"
            )
            savePipeline(
                action = action,
                pipeline = pipeline,
                userId = action.data.getUserId(),
                gitProjectId = action.data.getGitProjectId().toLong(),
                projectCode = action.getProjectCode(),
                modelAndSetting = modelAndSetting,
                updateLastModifyUser = updateLastModifyUser
            )
            buildId = client.get(ServiceWebhookBuildResource::class).webhookTrigger(
                // #7700 此处传入userid 为权限人。同一为用ci开启人做权限校验
                userId = action.data.setting.enableUser,
                projectId = action.getProjectCode(),
                pipelineId = pipeline.pipelineId,
                params = WebhookTriggerParams(
                    params = modelParameters.webHookParams,
                    userParams = manualValues,
                    startValues = mutableMapOf(PIPELINE_NAME to pipeline.displayName),
                    triggerReviewers = action.forkMrNeedReviewers()
                ),
                channelCode = channelCode,
                startType = action.getStartType()
            ).data!!
            logger.info(
                "StreamYamlBaseBuild|startBuild|success|gitProjectId|${action.data.getGitProjectId()}|" +
                    "pipelineId|${pipeline.pipelineId}|gitBuildId|$gitBuildId|buildId|$buildId"
            )
        } catch (e: StreamTriggerException) {
            errorStartBuild(
                action = action,
                pipeline = pipeline,
                gitBuildId = gitBuildId,
                ignore = Throwable(
                    message = try {
                        // format在遇到不可解析的问题可能会报错
                        e.triggerReason.detail.format(e.reasonParams)
                    } catch (ignore: Throwable) {
                        e.triggerReason.detail
                    },
                    e
                ),
                yamlTransferData = yamlTransferData
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
        // #7983 全新第一次出发执行次数均为1
        return BuildId(buildId, 1)
    }

    private fun errorStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        gitBuildId: Long,
        ignore: Throwable,
        yamlTransferData: YamlTransferData?
    ) {
        logger.warn(
            "StreamYamlBaseBuild|errorStartBuild|${action.data.getGitProjectId()}|" +
                "${pipeline.pipelineId}|$gitBuildId",
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
    private fun afterStartBuild(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        buildId: String,
        gitBuildId: Long,
        yamlTransferData: YamlTransferData?
    ) {
        try {
            val event = gitRequestEventDao.getWithEvent(
                dslContext = dslContext, id = action.data.context.requestEventId!!
            ) ?: throw RuntimeException("can't find event ${action.data.context.requestEventId!!}")
            streamEventSaveService.saveUserMessage(
                userId = action.data.eventCommon.userId,
                projectCode = action.getProjectCode(),
                event = event,
                gitProjectId = action.data.getGitProjectId().toLong(),
                messageType = UserMessageType.ONLY_SUCCESS,
                isSave = true
            )

            if (action is StreamRepoTriggerAction) {
                gitRequestRepoEventDao.updateBuildId(
                    dslContext = dslContext, eventId = event.id!!, pipelineId = pipeline.pipelineId, buildId = buildId
                )
            }

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
                        homePage = streamGitConfig.streamUrl ?: throw ParamBlankException(
                            I18nUtil.getCodeLanMessage(
                                messageCode = STARTUP_CONFIG_MISSING,
                                params = arrayOf(" streamUrl"),
                                language = I18nUtil.getDefaultLocaleLanguage()
                            )
                        ),
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

            savePipelineBuildCommit(action = action, pipeline = pipeline, buildId = buildId)
        } catch (ignore: Exception) {
            logger.warn(
                "StreamYamlBaseBuild|afterStartBuild|${action.data.getGitProjectId()}|" +
                    "${pipeline.pipelineId}|$gitBuildId",
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
                )?.gitProjectId?.let { GitCommonUtils.getCiProjectId(it, streamGitConfig.getScmType()) }
                    ?: return@forEach

                remoteProjectIdMap[remoteProjectString] = TemplateAcrossInfoType.values().associateWith {
                    BuildTemplateAcrossInfo(
                        templateId = templateData.templateId,
                        templateType = it,
                        templateInstancesIds = mutableListOf(),
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

    fun savePipelineBuildCommit(
        action: BaseAction,
        pipeline: StreamTriggerPipeline,
        buildId: String
    ) {
        if (action !is GitBaseAction) {
            return
        }
        val projectId = action.getProjectCode()
        val pipelineId = pipeline.pipelineId
        try {
            var page = 1
            val pageSize = BUILD_COMMIT_PAGE_SIZE
            while (true) {
                val webhookCommitList = action.getWebhookCommitList(
                    page = page,
                    pageSize = pageSize
                )
                client.get(ServicePipelineBuildCommitResource::class).save(
                    commits = webhookCommitList.map {
                        PipelineBuildCommit(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            commitId = it.commitId,
                            authorName = it.authorName,
                            message = it.message,
                            repoType = it.repoType,
                            commitTime = it.commitTime,
                            url = action.data.setting.gitHttpUrl,
                            eventType = it.eventType,
                            mrId = it.mrId,
                            channel = ChannelCode.GIT.name,
                            action = it.action
                        )
                    }
                )
                if (webhookCommitList.size < pageSize) break
                page++
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildCommitFinishEvent(
                    source = "build_commits",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
            )
        } catch (ignore: Throwable) {
            logger.warn("StreamYamlBaseBuild|savePipelineBuildCommit|error", ignore)
        }
    }

    fun confirmProjectUseModelMd5Cache(projectId: String): Boolean {
        return redisOperation.isMember(STREAM_MODEL_MD5_CACHE_PROJECTS_KEY, projectId)
    }
}
