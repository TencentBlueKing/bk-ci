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

import com.devops.process.yaml.v2.enums.StreamObjectKind
import com.devops.process.yaml.v2.utils.YamlCommonUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamManualAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamOpenApiAction
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandlerUtil
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBuilder
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList")
class ManualTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val actionFactory: EventActionFactory,
    private val streamGitConfig: StreamGitConfig,
    private val streamEventService: StreamEventService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamYamlTrigger: StreamYamlTrigger,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamYamlBuild: StreamYamlBuild
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ManualTriggerService::class.java)
    }

    fun triggerBuild(userId: String, pipelineId: String, triggerBuildReq: TriggerBuildReq): TriggerBuildResult {
        logger.info("Trigger build, userId: $userId, pipeline: $pipelineId, triggerBuildReq: $triggerBuildReq")

        val streamTriggerSetting = streamBasicSettingDao.getSettingByProjectCode(
            dslContext = dslContext,
            projectCode = triggerBuildReq.projectId
        )?.let {
            StreamTriggerSetting(
                enableCi = it.enableCi,
                buildPushedBranches = it.buildPushedBranches,
                buildPushedPullRequest = it.buildPushedPullRequest,
                enableUser = it.enableUserId,
                gitHttpUrl = it.gitHttpUrl,
                projectCode = it.projectCode,
                enableCommitCheck = it.enableCommitCheck,
                enableMrBlock = it.enableMrBlock,
                name = it.name,
                enableMrComment = it.enableMrComment
            )
        } ?: throw CustomException(Response.Status.FORBIDDEN, message = TriggerReason.CI_DISABLED.detail)

        // open api 通过提供事件原文模拟事件触发
        val action = if (!triggerBuildReq.payload.isNullOrEmpty()) {
            loadOpenApiAction(streamTriggerSetting, triggerBuildReq)
        } else {
            loadManualAction(streamTriggerSetting, userId, triggerBuildReq)
        }

        val buildPipeline = gitPipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = action.data.getGitProjectId().toLong(),
            pipelineId = pipelineId
        )?.let {
            StreamTriggerPipeline(
                gitProjectId = it.gitProjectId.toString(),
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator
            )
        } ?: throw OperationException("stream pipeline: $pipelineId is not exist")

        // 流水线未启用在手动触发处直接报错
        if (!buildPipeline.enabled) {
            throw CustomException(
                status = Response.Status.METHOD_NOT_ALLOWED,
                message = "${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})"
            )
        }

        action.data.context.pipeline = buildPipeline

        val originYaml = triggerBuildReq.yaml
        // 如果当前文件没有内容直接不触发
        if (originYaml.isNullOrBlank()) {
            logger.warn("Matcher is false, event: ${action.data.context.requestEventId} yaml is null")
            streamEventService.saveBuildNotBuildEvent(
                action = action,
                reason = TriggerReason.CI_YAML_CONTENT_NULL.name,
                reasonDetail = TriggerReason.CI_YAML_CONTENT_NULL.detail,
                sendCommitCheck = action.needSendCommitCheck(),
                commitCheckBlock = false,
                version = "v2.0"
            )
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.CI_YAML_CONTENT_NULL.name +
                    "(${TriggerReason.CI_YAML_CONTENT_NULL.detail.format("")})"
            )
        }

        action.data.context.originYaml = originYaml

        val result = handleTrigger(
            action = action,
            originYaml = originYaml,
            triggerBuildReq = triggerBuildReq
        ) ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = TriggerReason.PIPELINE_RUN_ERROR.name +
                "(${TriggerReason.PIPELINE_RUN_ERROR.detail})"
        )
        return TriggerBuildResult(
            projectId = action.data.eventCommon.gitProjectId.toLong(),
            branch = triggerBuildReq.branch,
            customCommitMsg = triggerBuildReq.customCommitMsg,
            description = triggerBuildReq.description,
            commitId = triggerBuildReq.commitId,
            buildId = result.id,
            buildUrl = StreamPipelineUtils.genStreamV2BuildUrl(
                homePage = streamGitConfig.streamUrl ?: throw ParamBlankException("启动配置缺少 streamUrl"),
                gitProjectId = buildPipeline.gitProjectId,
                pipelineId = pipelineId,
                buildId = result.id
            )
        )
    }

    private fun loadManualAction(
        streamTriggerSetting: StreamTriggerSetting,
        userId: String,
        triggerBuildReq: TriggerBuildReq
    ): StreamManualAction {
        val action = actionFactory.loadManualAction(
            setting = streamTriggerSetting,
            event = StreamManualEvent(
                userId = userId,
                gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId).toString(),
                triggerBuildReq = triggerBuildReq
            )
        )
        val request = action.buildRequestEvent("") ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = "event invalid"
        )
        val id = gitRequestEventDao.saveGitRequest(dslContext, request)
        action.data.context.requestEventId = id

        return action
    }

    private fun loadOpenApiAction(
        streamTriggerSetting: StreamTriggerSetting,
        triggerBuildReq: TriggerBuildReq
    ): StreamOpenApiAction {

        val event = mockWebhookTrigger(triggerBuildReq)
        val action = StreamOpenApiAction(
            actionFactory.load(event) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "can not load action"
            )
        )

        // 仅支持当前仓库下的 event
        if (action.data.getGitProjectId() != GitCommonUtils.getGitProjectId(triggerBuildReq.projectId).toString()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "Only events in the current repository [${triggerBuildReq.projectId}] are supported"
            )
        }

        val request = action.buildRequestEvent(triggerBuildReq.payload!!) ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = "event invalid"
        )
        val id = gitRequestEventDao.saveGitRequest(dslContext, request)
        action.data.context.requestEventId = id

        action.data.setting = streamTriggerSetting

        return action
    }

    private fun mockWebhookTrigger(triggerBuildReq: TriggerBuildReq): CodeWebhookEvent {
        // 这里使用eventType做判断，防止有些事件无法直接通过mapper得到，例如stream 的review
        if (triggerBuildReq.eventType.isNullOrBlank()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "eventType can't be empty"
            )
        }

        return when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> try {
                objectMapper.readValue<GitEvent>(triggerBuildReq.payload!!)
            } catch (ignore: Exception) {
                logger.warn("Fail to parse the git web hook commit event, errMsg: ${ignore.message}")
                throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = "Fail to parse the git web hook commit event, errMsg: ${ignore.message}"
                )
            }
            else -> throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "event not support"
            )
        }
    }

    fun handleTrigger(
        action: BaseAction,
        originYaml: String,
        triggerBuildReq: TriggerBuildReq
    ): BuildId? {
        logger.info("|${action.data.context.requestEventId}|handleTrigger|action|${action.format()}")

        var buildId: BuildId? = null
        StreamTriggerExceptionHandlerUtil.handleManualTrigger {
            buildId = trigger(action, originYaml, triggerBuildReq)
        }
        return buildId
    }

    private fun trigger(
        action: BaseAction,
        originYaml: String,
        triggerBuildReq: TriggerBuildReq
    ): BuildId? {
        val yamlReplaceResult = streamYamlTrigger.prepareCIBuildYaml(action)!!
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        val normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml)
        action.data.context.parsedYaml = parsedYaml
        action.data.context.normalizedYaml = normalizedYaml

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        val gitProjectInfo = action.api.getGitProjectInfo(
            action.getGitCred(),
            action.data.getGitProjectId(),
            ApiRequestRetryInfo(true)
        )!!
        streamBasicSettingService.updateProjectInfo(action.data.eventCommon.userId, gitProjectInfo)
        action.data.setting = action.data.setting.copy(gitHttpUrl = gitProjectInfo.gitHttpUrl)

        var params = emptyMap<String, String>()
        // open api 模拟真实git触发
        if (action.metaData.streamObjectKind == StreamObjectKind.OPENAPI) {
            params = (action as StreamOpenApiAction).getStartParams(
                triggerOn = TriggerBuilder.buildManualTriggerOn(action.metaData.streamObjectKind),
                scmType = streamGitConfig.getScmType()
            )
        }

        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = action.data.context.requestEventId!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = normalizedYaml,
            gitProjectId = action.data.getGitProjectId().toLong(),
            branch = action.data.eventCommon.branch,
            objectKind = action.metaData.streamObjectKind.value,
            commitMsg = triggerBuildReq.customCommitMsg,
            triggerUser = action.data.eventCommon.userId,
            sourceGitProjectId = action.data.eventCommon.sourceGitProjectId?.toLong(),
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )
        return streamYamlBuild.gitStartBuild(
            action = action,
            TriggerResult(
                trigger = true,
                startParams = params,
                timeTrigger = false,
                deleteTrigger = false
            ),
            yaml = yamlReplaceResult.normalYaml,
            gitBuildId = gitBuildId,
            onlySavePipeline = false,
            yamlTransferData = yamlReplaceResult.yamlTransferData
        )
    }
}
