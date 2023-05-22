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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.CI_START_USER_NO_CURRENT_PROJECT_EXECUTE_PERMISSIONS
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.dao.StreamPipelineTriggerDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.TriggerCache
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerDispatch
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import java.util.UUID
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamTriggerRequestService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val rabbitTemplate: RabbitTemplate,
    private val actionFactory: EventActionFactory,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache,
    private val exHandler: StreamTriggerExceptionHandler,
    private val triggerMatcher: TriggerMatcher,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val streamTriggerRequestRepoService: StreamTriggerRequestRepoService,
    private val streamSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val streamPipelineTriggerDao: StreamPipelineTriggerDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerRequestService::class.java)
    }

    private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun externalCodeGitBuild(eventType: String?, webHookType: String, event: String): Boolean? {
        logger.info("StreamTriggerRequestService|externalCodeGitBuild|event|$event|type|$eventType|$webHookType")
        when (ScmType.valueOf(webHookType)) {
            ScmType.CODE_GIT -> {
                val eventObject = try {
                    objectMapper.readValue<GitEvent>(event)
                } catch (ignore: Exception) {
                    logger.warn(
                        "StreamTriggerRequestService|externalCodeGitBuild" +
                            "|Fail to parse the git web hook commit event|errMsg|${ignore.message}"
                    )
                    return false
                }
                // 处理不需要项目信息的，或不同软件源的预处理逻辑

                return start(eventObject, event, ScmType.CODE_GIT)
            }
            ScmType.GITHUB -> {
                val eventObject: GithubEvent = when (eventType) {
                    GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(event)
                    GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(event)
                    else -> {
                        logger.info("Github event($eventType) is ignored")
                        return true
                    }
                }
                return start(eventObject, event, ScmType.GITHUB)
            }
            // 对接其他平台时扩充
            else -> {}
        }
        return false
    }

    fun start(eventObject: CodeWebhookEvent, event: String, scmType: ScmType): Boolean? {
        // 加载不同源的action
        val action = actionFactory.load(eventObject)
        if (action == null) {
            logger.warn("StreamTriggerRequestService|start|request event not support|$event")
            return false
        }
        val eventCommon = action.data.eventCommon

        // 初始化setting
        if (!action.data.isSettingInitialized) {
            val gitCIBasicSetting = streamSettingDao.getSetting(dslContext, eventCommon.gitProjectId.toLong())
            if (null != gitCIBasicSetting) {
                action.data.setting = StreamTriggerSetting(gitCIBasicSetting)
            }
        }
        action.initCacheData()
        // 获取前端展示相关的requestEvent
        val requestEvent = action.buildRequestEvent(event) ?: return false

        val repoTriggerPipelineList = repoTriggerEventService.getTargetPipelines(
            eventCommon.gitProjectName
        )

        // 跨项目触发的逻辑不需要当前项目也可以使用
        if (repoTriggerPipelineList.isNotEmpty()) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.data.context.requestEventId = requestEventId

            try {
                if (action.skipStream()) {
                    return true
                }
                // 为了不影响主逻辑对action进行深拷贝
                val bizId = MDC.get(TraceTag.BIZID)
                val newId = UUID.randomUUID().toString()
                logger.info("stream start repo trigger|old bizId:$bizId| new bizId:$newId")
                executors.submit {
                    // 新线程biz id会断，需要重新注入
                    MDC.put(TraceTag.BIZID, newId)
                    logger.info("stream start repo trigger|old bizId:$bizId| new bizId:${MDC.get(TraceTag.BIZID)}")
                    streamTriggerRequestRepoService.repoTriggerBuild(
                        triggerPipelineList = repoTriggerPipelineList,
                        eventStr = event,
                        actionCommonData = objectMapper.writeValueAsString(action.data.eventCommon),
                        actionContext = objectMapper.writeValueAsString(action.data.context)
                    )
                }
            } catch (ignore: Throwable) {
                logger.warn("StreamTriggerRequestService|start|${action.data.eventCommon.gitProjectName}|error", ignore)
            }
        }

        // 上方已尝试初始化setting，在这还未初始化setting的说明没有开启过ci
        if (!action.data.isSettingInitialized || !action.data.setting.enableCi) {
            logger.info(
                "StreamTriggerRequestService|start" +
                    "|git ci is not enabled , but it has repo trigger|project_id|${action.data.getGitProjectId()}"
            )
            return null
        }

        if (action.data.context.requestEventId == null) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.data.context.requestEventId = requestEventId
        }

        return exHandler.handle(action) {
            if (action.skipStream()) {
                return@handle true
            }
            checkRequest(action)
        }
    }

    private fun checkRequest(
        action: BaseAction
    ): Boolean {
        logger.info(
            "StreamTriggerRequestService|checkRequest" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        CheckStreamSetting.checkGitProjectConf(action)

        val path2PipelineExists = gitPipelineResourceDao.getAllByGitProjectId(
            dslContext, action.data.getGitProjectId().toLong()
        ).associate {
            it.filePath to StreamTriggerPipeline(
                gitProjectId = it.gitProjectId.toString(),
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator
            )
        }

        val projectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = action.data.getGitProjectId(),
            action = action,
            getProjectInfo = action.api::getGitProjectInfo
        ) ?: throw StreamTriggerException(
            action = action,
            triggerReason = TriggerReason.PIPELINE_PREPARE_ERROR,
            reasonParams = listOf(
                I18nUtil.getCodeLanMessage(
                messageCode = CI_START_USER_NO_CURRENT_PROJECT_EXECUTE_PERMISSIONS,
                params = arrayOf(action.data.setting.enableUser)
                )
            )
        )

        action.data.context.defaultBranch = projectInfo.defaultBranch
        action.data.context.repoCreatedTime = projectInfo.repoCreatedTime
        action.data.context.repoCreatorId = projectInfo.repoCreatorId
        action.parseStreamTriggerContext()
        // 校验mr请求是否产生冲突
        if (!action.checkMrConflict(path2PipelineExists = path2PipelineExists)) {
            return false
        }

        return matchAndTriggerPipeline(action, path2PipelineExists)
    }

    @Suppress("ALL")
    fun matchAndTriggerPipeline(
        action: BaseAction,
        path2PipelineExists: Map<String, StreamTriggerPipeline>
    ): Boolean {
        logger.info(
            "StreamTriggerRequestService|matchAndTriggerPipeline" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )
        // 判断本次mr/push提交是否需要删除流水线, fork不用
        // 远程触发不存在删除流水线的情况
        if (action.data.context.repoTrigger == null) {
            action.checkAndDeletePipeline(path2PipelineExists)
        }

        // 获取yaml文件列表，同时会拿到Mr的changeSet
        val yamlPathList = action.getYamlPathList()

        logger.info(
            "StreamTriggerRequestService|matchAndTriggerPipeline" +
                "|gitProjectId|${action.data.eventCommon.gitProjectId}|" +
                "yamlPathList|$yamlPathList|path2PipelineExists|$path2PipelineExists"
        )

        // 如果没有Yaml文件则直接不触发
        if (yamlPathList.isEmpty()) {
            logger.warn(
                "StreamTriggerRequestService|matchAndTriggerPipeline" +
                    "|event|${action.data.context.requestEventId}|cannot found ci yaml from git"
            )
            throw StreamTriggerException(action, TriggerReason.CI_YAML_NOT_FOUND)
        }

        val confirmProjectUseTriggerCache = triggerMatcher.confirmProjectUseTriggerCache(action.getProjectCode())

        // 获取缓存的触发器, 使用空文本来区分是有缓存但是触发器没内容的情况
        // 使用blobId 20个一组来取，减少内存占用
        val blobIdMap = mutableMapOf<Int, MutableList<String>>()
        val yamlMap = mutableMapOf<Int, MutableList<YamlPathListEntry>>()
        var index = 1
        yamlPathList.forEach { yamlPath ->
            if (blobIdMap[index]?.size == 20) {
                index++
            }
            if (!yamlPath.blobId.isNullOrBlank()) {
                blobIdMap[index] = blobIdMap[index]?.also { it.add(yamlPath.blobId) } ?: mutableListOf(yamlPath.blobId)
            }
            yamlMap[index] = yamlMap[index]?.also { it.add(yamlPath) } ?: mutableListOf(yamlPath)
        }

        val bizId = MDC.get(TraceTag.BIZID)

        yamlMap.forEach { (i, yamlList) ->
            val triggers = if (!confirmProjectUseTriggerCache) {
                null
            } else {
                streamPipelineTriggerDao.getTriggers(
                    dslContext = dslContext,
                    projectId = action.getProjectCode(),
                    pipelineId = null,
                    branch = null,
                    ciFileBlobIds = blobIdMap[i]?.toSet() ?: emptySet()
                ).associate { it.pipelineId to it.trigger }
            }

            yamlList.forEach yamlEach@{ (filePath, checkType, ref, blobId) ->
                // 保存触发器缓存信息
                if (!ref.isNullOrBlank() && !blobId.isNullOrBlank()) {
                    action.data.context.triggerCache = TriggerCache(pipelineFileBranch = ref, blobId = blobId)
                }
                // 如果该流水线已保存过，则继续使用
                // 对于来自fork库的mr新建的流水线，当前库不维护其状态
                val buildPipeline = path2PipelineExists[filePath] ?: StreamTriggerPipeline(
                    gitProjectId = action.data.eventCommon.gitProjectId,
                    displayName = filePath,
                    pipelineId = "", // 留空用于是否创建判断
                    filePath = filePath,
                    enabled = true,
                    creator = action.data.getUserId()
                )
                // 远程仓库触发时，主库不需要新建流水线
                if (action.checkRepoHookTrigger() && buildPipeline.pipelineId.isBlank()) {
                    return@yamlEach
                }

                action.data.context.pipeline = buildPipeline

                // 新增流水线，需要校验的不使用缓存的触发器
                val trigger = if (!confirmProjectUseTriggerCache ||
                    buildPipeline.pipelineId.isBlank() || checkType != CheckType.NO_NEED_CHECK
                ) {
                    null
                } else {
                    triggers?.get(buildPipeline.pipelineId)
                }

                // 针对每个流水线处理异常
                exHandler.handle(action) {
                    // 目前只针对mr情况下源分支有目标分支没有且变更列表没有
                    if (checkType == CheckType.NO_TRIGGER) {
                        throw StreamTriggerException(
                            action = action,
                            triggerReason = TriggerReason.MR_BRANCH_FILE_ERROR,
                            reasonParams = listOf(filePath)
                        )
                    }
                    val newId = UUID.randomUUID().toString()
                    logger.info("stream start local trigger $filePath|old bizId:$bizId| new bizId:$newId")
                    MDC.put(TraceTag.BIZID, newId)
                    logger.info(
                        "stream start local trigger $filePath|old bizId:$bizId|" +
                            " new bizId:${MDC.get(TraceTag.BIZID)}"
                    )
                    trigger(action = action, trigger = trigger)
                    MDC.put(TraceTag.BIZID, bizId)
                }
            }
        }

        // 流水线启动后，发送解锁webhook锁请求
        action.sendUnlockWebhook()
        return true
    }

    @Suppress("ProtectedMemberInFinalClass")
    protected fun trigger(
        action: BaseAction,
        trigger: String?
    ) = when (streamGitConfig.getScmType()) {
        ScmType.CODE_GIT -> StreamTriggerDispatch.dispatch(
            rabbitTemplate = rabbitTemplate,
            event = StreamTriggerEvent(
                eventStr = if (action.metaData.streamObjectKind == StreamObjectKind.REVIEW) {
                    objectMapper.writeValueAsString(
                        (action.data.event as GitReviewEvent).copy(
                            objectKind = GitReviewEvent.classType
                        )
                    )
                } else {
                    objectMapper.writeValueAsString(action.data.event as GitEvent)
                },
                actionCommonData = action.data.eventCommon,
                actionContext = action.data.context,
                actionSetting = action.data.setting,
                trigger = trigger
            )
        )
        ScmType.GITHUB -> StreamTriggerDispatch.dispatch(
            rabbitTemplate = rabbitTemplate,
            event = StreamTriggerEvent(
                eventStr = objectMapper.writeValueAsString(action.data.event as GithubEvent),
                actionCommonData = action.data.eventCommon,
                actionContext = action.data.context,
                actionSetting = action.data.setting,
                trigger = trigger
            )
        )
        else -> TODO("对接其他Git平台时需要补充")
    }
}
