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

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.trigger.v2.StreamYamlBuild
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScheduleTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCIBasicSettingService: StreamBasicSettingService,
    private val yamlBuildV2: StreamYamlBuild,
    private val streamTimerService: StreamTimerService,
    private val triggerExceptionService: TriggerExceptionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleTriggerService::class.java)
    }

    fun triggerBuild(
        streamTimerEvent: StreamTimerBuildEvent,
        buildBranch: String,
        buildCommitInfo: RevisionInfo
    ): BuildId? {
        val gitRequestEvent = GitRequestEventHandle.createScheduleTriggerEvent(
            streamTimerEvent,
            buildBranch,
            buildCommitInfo.revision,
            buildCommitInfo.updatedMessage,
            buildCommitInfo.authorName
        )
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

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

        val buildPipeline = GitProjectPipeline(
            gitProjectId = existsPipeline.gitProjectId,
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            latestBuildInfo = null,
            latestBuildBranch = buildBranch
        )

        return handleTrigger(
            gitRequestEvent = gitRequestEvent,
            originYaml = streamTimerEvent.originYaml,
            buildPipeline = buildPipeline
        )
    }

    fun handleTrigger(
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline
    ): BuildId? {
        return triggerExceptionService.handle(gitRequestEvent, null, null) {
            trigger(gitRequestEvent, originYaml, buildPipeline)
        }
    }

    private fun trigger(
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline
    ): BuildId? {
        // 如果当前文件没有内容直接不触发
        if (originYaml.isBlank()) {
            logger.warn(
                "Matcher is false,gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            TriggerException.triggerError(
                request = gitRequestEvent,
                event = null,
                pipeline = buildPipeline,
                reason = TriggerReason.CI_YAML_CONTENT_NULL,
                yamls = Yamls(
                    originYaml = originYaml,
                    parsedYaml = null,
                    normalYaml = null
                ),
                commitCheck = CommitCheck(
                    block = false,
                    state = GitCICommitCheckState.FAILURE
                )
            )
        }

        val yamlReplaceResult = yamlTriggerFactory.requestTriggerV2.prepareCIBuildYaml(
            gitRequestEvent = gitRequestEvent,
            isMr = false,
            originYaml = originYaml,
            filePath = buildPipeline.filePath,
            pipelineId = buildPipeline.pipelineId,
            pipelineName = buildPipeline.displayName,
            event = null,
            changeSet = null,
            forkGitProjectId = null
        )!!
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = gitRequestEvent.id!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml),
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            objectKind = gitRequestEvent.objectKind,
            commitMsg = gitRequestEvent.commitMsg,
            triggerUser = gitRequestEvent.userId,
            sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )
        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        gitCIBasicSettingService.refreshSetting(gitRequestEvent.userId, gitRequestEvent.gitProjectId)
        return yamlBuildV2.gitStartBuild(
            pipeline = buildPipeline,
            event = gitRequestEvent,
            yaml = yamlReplaceResult.normalYaml,
            parsedYaml = parsedYaml,
            originYaml = originYaml,
            normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml),
            gitBuildId = gitBuildId,
            isTimeTrigger = false,
            onlySavePipeline = false,
            yamlTransferData = yamlReplaceResult.yamlTransferData
        )
    }
}
