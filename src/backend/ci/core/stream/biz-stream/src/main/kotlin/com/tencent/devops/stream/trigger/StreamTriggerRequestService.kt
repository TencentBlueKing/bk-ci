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
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.RepoTrigger
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerDispatch
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.pojo.CheckType
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
    private val streamTriggerCache: StreamTriggerCache,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val exHandler: StreamTriggerExceptionHandler,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val streamTriggerRequestRepoService: StreamTriggerRequestRepoService,
    private val streamSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao
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
        val action = actionFactory.load(eventObject).also {
            if (it == null) {
                logger.warn("event not support: $event")
                return false
            }
        }!!

        // 获取前端展示相关的requestEvent
        val requestEvent = action.buildRequestEvent(event) ?: return false

        val eventCommon = action.data.eventCommon

        val repoTriggerPipelineList = repoTriggerEventService.getTargetPipelines(
            eventCommon.gitProjectName
        ).also {
            if (it.isNotEmpty()) {
                action.data.context.repoTrigger = RepoTrigger("", it)
            }
        }

        // 跨项目触发的逻辑不需要当前项目也可以使用
        if (repoTriggerPipelineList.isNotEmpty()) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.init(requestEventId)

            if (action.skipStream()) {
                return true
            }

            try {
                streamTriggerRequestRepoService.repoTriggerBuild(
                    triggerPipelineList = repoTriggerPipelineList,
                    action = action
                )
            } catch (ignore: Throwable) {
                logger.error("Fail to start repo trigger (${action.data.eventCommon.gitProjectName})", ignore)
            }
        }

        // 没开启stream的就不存event事件信息
        val gitCIBasicSetting = streamSettingDao.getSetting(dslContext, eventCommon.gitProjectId.toLong())

        if (null == gitCIBasicSetting || !gitCIBasicSetting.enableCi) {
            logger.info(
                "git ci is not enabled , but it has repo trigger , git project id: ${action.data.getGitProjectId()}"
            )
            return null
        }

        action.data.setting = StreamTriggerSetting(gitCIBasicSetting)

        if (action.data.context.requestEventId == null) {
            val requestEventId = gitRequestEventDao.saveGitRequest(dslContext, requestEvent)
            action.init(requestEventId)
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
        if (action.data.context.repoTrigger != null) {
            action.checkAndDeletePipeline(path2PipelineExists)
        }

        action.data.context.defaultBranch = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = action.data.getGitProjectId(),
            action = action,
            getProjectInfo = action.api::getGitProjectInfo
        ).defaultBranch

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

        yamlPathList.forEach { (filePath, checkType) ->
            // 如果该流水线已保存过，则继续使用
            // 对于来自fork库的mr新建的流水线，当前库不维护其状态
            val buildPipeline = path2PipelineExists[filePath] ?: StreamTriggerPipeline(
                gitProjectId = action.data.eventCommon.gitProjectId,
                displayName = filePath,
                pipelineId = "", // 留空用于是否创建判断
                filePath = filePath,
                enabled = true,
                creator = action.data.eventCommon.userId
            )
            // 远程仓库触发不需要新建流水线
            if (action.data.context.repoTrigger != null && buildPipeline.pipelineId.isBlank()) {
                return@forEach
            }

            action.data.context.pipeline = buildPipeline

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

                checkAndTrigger(buildPipeline = buildPipeline, action = action)
            }
        }
        return true
    }

    private fun checkAndTrigger(
        buildPipeline: StreamTriggerPipeline,
        action: BaseAction
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

        val originYaml = action.getYamlContent(filePath)
        action.data.context.originYaml = originYaml

        // 如果当前文件没有内容直接不触发
        if (originYaml.isBlank()) {
            throw StreamTriggerException(
                action,
                TriggerReason.CI_YAML_CONTENT_NULL,
                commitCheck = CommitCheck(
                    block = action.metaData.isStreamMr(),
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }

        yamlSchemaCheck.check(action = action, templateType = null, isCiFile = true)

        // 进入触发流程
        trigger(action)
    }

    protected fun trigger(action: BaseAction) = StreamTriggerDispatch.dispatch(rabbitTemplate, StreamTriggerEvent(action))
}
