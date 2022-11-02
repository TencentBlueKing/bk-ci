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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamScheduleAction
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleEvent
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamRevisionInfo
import com.tencent.devops.stream.trigger.git.pojo.toStreamGitProjectInfoWithProject
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScheduleTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val eventActionFactory: EventActionFactory,
    private val streamYamlTrigger: StreamYamlTrigger,
    private val streamYamlBuild: StreamYamlBuild,
    private val exHandler: StreamTriggerExceptionHandler,
    private val streamTimerService: StreamTimerService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleTriggerService::class.java)
    }

    fun triggerBuild(
        streamTimerEvent: StreamTimerBuildEvent,
        buildBranch: String,
        buildCommitInfo: StreamRevisionInfo
    ): BuildId? {
        val streamTriggerSetting = streamBasicSettingDao.getSetting(
            dslContext = dslContext,
            gitProjectId = GitCommonUtils.getGitProjectId(streamTimerEvent.projectId),
            hasLastInfo = false
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
                enableMrComment = it.enableMrComment,
                homepage = it.homepage,
                triggerReviewSetting = it.triggerReviewSetting
            )
        }
        if (streamTriggerSetting == null || !streamTriggerSetting.enableCi) {
            logger.warn(
                "ScheduleTriggerService|triggerBuild" +
                    "|not enable ci no trigger schedule|project|${streamTimerEvent.projectId}"
            )
            return null
        }

        val action = eventActionFactory.loadScheduleAction(
            setting = streamTriggerSetting,
            event = StreamScheduleEvent(
                userId = streamTriggerSetting.enableUser,
                gitProjectId = streamTimerEvent.gitProjectId.toString(),
                projectCode = streamTimerEvent.projectId,
                branch = buildBranch,
                commitId = buildCommitInfo.revision,
                commitMsg = buildCommitInfo.updatedMessage,
                commitAuthor = buildCommitInfo.authorName
            )
        )
        val event = action.event()
        val gitRequestEvent = GitRequestEventHandle.createScheduleTriggerEvent(
            event = event,
            eventStr = JsonUtil.toJson(event)
        )
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        action.data.context.requestEventId = id

        val existsPipeline = gitPipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = streamTimerEvent.gitProjectId,
            pipelineId = streamTimerEvent.pipelineId
        )
        // 流水线不存在时删除定时触发任务
        if (existsPipeline == null) {
            streamTimerService.deleteTimer(streamTimerEvent.pipelineId, streamTimerEvent.userId)
            return null
        }

        // 流水线未启用在定时什么都不管，不触发不提醒
        if (!existsPipeline.enabled) {
            return null
        }

        val buildPipeline = StreamTriggerPipeline(
            gitProjectId = existsPipeline.gitProjectId.toString(),
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            // 兼容定时触发，触发人取流水线最近修改人
            lastModifier = client.get(ServicePipelineResource::class).getPipelineInfo(
                projectId = GitCommonUtils.getCiProjectId(existsPipeline.gitProjectId, streamGitConfig.getScmType()),
                pipelineId = existsPipeline.pipelineId,
                channelCode = ChannelCode.GIT
            ).data?.lastModifyUser
        )
        action.data.context.pipeline = buildPipeline

        action.data.context.originYaml = streamTimerEvent.originYaml

        return handleTrigger(
            action = action,
            originYaml = streamTimerEvent.originYaml
        )
    }

    fun handleTrigger(
        action: StreamScheduleAction,
        originYaml: String
    ): BuildId? {
        logger.info(
            "ScheduleTriggerService|handleTrigger" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )
        return exHandler.handle(action) {
            trigger(action, originYaml)
        }
    }

    @BkTimed
    private fun trigger(
        action: StreamScheduleAction,
        originYaml: String
    ): BuildId? {
        // 如果当前文件没有内容直接不触发
        if (originYaml.isBlank()) {
            throw StreamTriggerException(
                action = action,
                triggerReason = TriggerReason.CI_YAML_CONTENT_NULL,
                commitCheck = CommitCheck(
                    block = false,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }

        val yamlReplaceResult = streamYamlTrigger.prepareCIBuildYaml(action)!!
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        val normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml)
        action.data.context.parsedYaml = parsedYaml
        action.data.context.normalizedYaml = normalizedYaml

        val event = action.event()
        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = action.data.context.requestEventId!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = normalizedYaml,
            gitProjectId = event.gitProjectId.toLong(),
            branch = event.branch,
            objectKind = action.metaData.streamObjectKind.value,
            commitMsg = action.data.eventCommon.commit.commitMsg,
            triggerUser = event.userId,
            sourceGitProjectId = action.data.eventCommon.sourceGitProjectId?.toLong(),
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        val gitProjectInfo = action.api.getGitProjectInfo(
            action.getGitCred(),
            action.getGitProjectIdOrName(),
            ApiRequestRetryInfo(true)
        )!!.toStreamGitProjectInfoWithProject()
        streamBasicSettingService.updateProjectInfo(action.data.getUserId(), gitProjectInfo)

        return streamYamlBuild.gitStartBuild(
            action = action,
            triggerResult = TriggerResult(
                trigger = TriggerBody(true),
                triggerOn = null,
                timeTrigger = false,
                deleteTrigger = false
            ),
            startParams = emptyMap(),
            yaml = yamlReplaceResult.normalYaml,
            gitBuildId = gitBuildId,
            onlySavePipeline = false,
            yamlTransferData = yamlReplaceResult.yamlTransferData,
            manualInputs = null
        )
    }
}
