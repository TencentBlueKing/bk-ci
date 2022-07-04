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
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.config.StreamGitConfig
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
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerDispatch
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.MrYamlInfo
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
    private val yamlSchemaCheck: YamlSchemaCheck,
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

    fun externalCodeGitBuild(eventType: String?, event: String): Boolean? {
        logger.info("Trigger code git build($event, $eventType)")
        val eventObject = try {
            objectMapper.readValue<GitEvent>(event)
        } catch (ignore: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${ignore.message}")
            return false
        }

        // 处理不需要项目信息的，或不同软件源的预处理逻辑

        return start(eventObject, event)
    }

    fun start(eventObject: GitEvent, event: String): Boolean? {
        // 加载不同源的action
        val action = actionFactory.load(eventObject)
        if (action == null) {
            logger.warn("request event not support: $event")
            return false
        }

        // 获取前端展示相关的requestEvent
        val requestEvent = action.buildRequestEvent(event) ?: return false

        val eventCommon = action.data.eventCommon

        val repoTriggerPipelineList = repoTriggerEventService.getTargetPipelines(
            eventCommon.gitProjectName
        )

        // 跨项目触发的逻辑不需要当前项目也可以使用
        if (repoTriggerPipelineList.isNotEmpty()) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.data.context.requestEventId = requestEventId

            if (action.skipStream()) {
                return true
            }

            try {
                // 为了不影响主逻辑对action进行深拷贝
                streamTriggerRequestRepoService.repoTriggerBuild(
                    triggerPipelineList = repoTriggerPipelineList,
                    eventStr = event,
                    actionCommonData = objectMapper.writeValueAsString(action.data.eventCommon),
                    actionContext = objectMapper.writeValueAsString(action.data.context)
                )
            } catch (ignore: Throwable) {
                logger.error("Fail to start repo trigger (${action.data.eventCommon.gitProjectName})", ignore)
            }
        }

        // 没开启stream的就不存event事件信息
        if (!action.data.isSettingInitialized) {
            val gitCIBasicSetting = streamSettingDao.getSetting(dslContext, eventCommon.gitProjectId.toLong())

            if (null == gitCIBasicSetting || !gitCIBasicSetting.enableCi) {
                logger.info(
                    "git ci is not enabled , but it has repo trigger , git project id: ${action.data.getGitProjectId()}"
                )
                return null
            }

            action.data.setting = StreamTriggerSetting(gitCIBasicSetting)
        }

        if (action.data.context.requestEventId == null) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.data.context.requestEventId = requestEventId
        }

        if (action.skipStream()) {
            return true
        }

        return exHandler.handle(action) { checkRequest(action) }
    }

    private fun checkRequest(
        action: BaseAction
    ): Boolean {
        logger.info("|${action.data.context.requestEventId}|checkRequest|action|${action.format()}")

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
        logger.info("|${action.data.context.requestEventId}|matchAndTriggerPipeline|action|${action.format()}")

        // 判断本次mr/push提交是否需要删除流水线, fork不用
        // 远程触发不存在删除流水线的情况
        if (action.data.context.repoTrigger == null) {
            action.checkAndDeletePipeline(path2PipelineExists)
        }

        action.data.context.defaultBranch = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = action.data.getGitProjectId(),
            action = action,
            getProjectInfo = action.api::getGitProjectInfo
        )!!.defaultBranch

        // 获取yaml文件列表，同时会拿到Mr的changeSet
        val yamlPathList = action.getYamlPathList()

        logger.info(
            "matchAndTriggerPipeline in gitProjectId:${action.data.eventCommon.gitProjectId}," +
                "yamlPathList: $yamlPathList, path2PipelineExists: $path2PipelineExists, "
        )

        // 如果没有Yaml文件则直接不触发
        if (yamlPathList.isEmpty()) {
            logger.warn("event: ${action.data.context.requestEventId} cannot found ci yaml from git")
            throw StreamTriggerException(action, TriggerReason.CI_YAML_NOT_FOUND)
        }

        // 获取缓存的触发器, 使用空文本来区分是有缓存但是触发器没内容的情况
        val confirmProjectUseTriggerCache = triggerMatcher.confirmProjectUseTriggerCache(action.getProjectCode())
        val triggers = if (!confirmProjectUseTriggerCache) {
            emptyMap()
        } else {
            streamPipelineTriggerDao.getTriggers(
                dslContext = dslContext,
                projectId = action.getProjectCode(),
                pipelineId = null,
                branch = null,
                ciFileBlobIds = yamlPathList.asSequence().filter { !it.blobId.isNullOrBlank() }.map { it.blobId!! }
                    .toSet()
            ).associate { it.pipelineId to it.trigger }
        }

        yamlPathList.forEach { (filePath, checkType, ref, blobId) ->
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
            if (action.data.context.repoTrigger != null && buildPipeline.pipelineId.isBlank()) {
                return@forEach
            }

            action.data.context.pipeline = buildPipeline

            // 新增流水线，需要校验的不使用缓存的触发器
            val trigger = if (!confirmProjectUseTriggerCache ||
                buildPipeline.pipelineId.isBlank() || checkType != CheckType.NO_NEED_CHECK
            ) {
                null
            } else {
                triggers[buildPipeline.pipelineId]
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

                checkAndTrigger(buildPipeline = buildPipeline, action = action, trigger = trigger)
            }
        }
        // 流水线启动后，发送解锁webhook锁请求
        action.sendUnlockWebhook()
        return true
    }

    private fun checkAndTrigger(
        buildPipeline: StreamTriggerPipeline,
        action: BaseAction,
        trigger: String?
    ) {
        logger.info("|${action.data.context.requestEventId}|checkAndTrigger|action|${action.format()}")

        val filePath = buildPipeline.filePath
        // 流水线未启用则跳过
        if (!buildPipeline.enabled) {
            logger.warn(
                "Pipeline $filePath is not enabled, gitProjectId: ${action.data.eventCommon.gitProjectId}, " +
                    "eventId: ${action.data.context.requestEventId}"
            )
            throw StreamTriggerException(action, TriggerReason.PIPELINE_DISABLE)
        }

        // 使用触发缓存
        val triggerResult = if (trigger == null) {
            null
        } else {
            triggerMatcher.isMatch(action, trigger)
        }

        // 这里判断，各类注册事件如果修改blobId肯定不同，相同的肯定注册过了，所以只要不触发git就直接跳过
        if (triggerResult != null && !triggerResult.trigger) {
            logger.info(
                "${buildPipeline.pipelineId}| use trigger cache" +
                    "Matcher is false, return, gitProjectId: ${action.data.getGitProjectId()}, " +
                    "eventId: ${action.data.context.requestEventId}"
            )
            throw StreamTriggerException(action, TriggerReason.TRIGGER_NOT_MATCH)
        }

        val yamlContent = action.getYamlContent(filePath)
        action.data.context.originYaml = yamlContent.content

        // 如果当前文件没有内容直接不触发
        if (yamlContent.content.isBlank()) {
            throw StreamTriggerException(
                action,
                TriggerReason.CI_YAML_CONTENT_NULL,
                commitCheck = CommitCheck(
                    block = action.metaData.isStreamMr(),
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }

        // 因为mr获取文件后分支信息可能不同，这里单独重新更新触发器缓存信息
        if (action is StreamMrAction) {
            yamlContent as MrYamlInfo
            if (yamlContent.ref.isNotBlank() && !yamlContent.blobId.isNullOrBlank())
                action.data.context.triggerCache = TriggerCache(
                    pipelineFileBranch = yamlContent.ref,
                    blobId = yamlContent.blobId
                )
        }

        yamlSchemaCheck.check(action = action, templateType = null, isCiFile = true)

        // 进入触发流程
        trigger(action, triggerResult)
    }

    protected fun trigger(action: BaseAction, triggerResult: TriggerResult?) = when (streamGitConfig.getScmType()) {
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
                triggerResult = triggerResult
            )
        )
        else -> TODO("对接其他Git平台时需要补充")
    }
}
