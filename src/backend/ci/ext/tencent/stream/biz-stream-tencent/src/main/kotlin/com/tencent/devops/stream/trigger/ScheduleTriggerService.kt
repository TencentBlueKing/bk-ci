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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
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
import com.tencent.devops.stream.trigger.v2.YamlBuildV2
import com.tencent.devops.stream.v2.service.GitCIBasicSettingService
import com.tencent.devops.stream.v2.service.OauthService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class ScheduleTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val oauthService: OauthService,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCIBasicSettingService: GitCIBasicSettingService,
    private val yamlBuildV2: YamlBuildV2,
    private val triggerExceptionService: TriggerExceptionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleTriggerService::class.java)
    }

    fun triggerBuild(streamTimerEvent: StreamTimerBuildEvent, buildBranch: String): Boolean {

        //todo: 这里是否需要传入最新的定时触发id，还是复用之前的id，还是不管id
        val gitRequestEvent = GitRequestEventHandle.createScheduleTriggerEvent(streamTimerEvent, buildBranch)
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        val existsPipeline =
            gitPipelineResourceDao.getPipelineById(
                dslContext,
                streamTimerEvent.gitProjectId,
                streamTimerEvent.pipelineId
            ) ?: throw OperationException("stream pipeline: ${streamTimerEvent.pipelineId} is not exist")
        // 如果该流水线已保存过，则继续使用
        val buildPipeline = GitProjectPipeline(
            gitProjectId = existsPipeline.gitProjectId,
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            latestBuildInfo = null
        )

        // todo:未启用则对应的定时触发删除还是不触发还是报错
        // 流水线未启用在定时处直接报错
        if (!buildPipeline.enabled) {
            throw CustomException(
                status = Response.Status.METHOD_NOT_ALLOWED,
                message = "${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})"
            )
        }

        handleTrigger(
            userId = streamTimerEvent.userId,
            gitRequestEvent = gitRequestEvent,
            originYaml =  streamTimerEvent.originYaml,
            buildPipeline = buildPipeline
        )
        return true
    }

    fun handleTrigger(
        userId: String,
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline
    ) {
        triggerExceptionService.handle(gitRequestEvent, null, null) {
            trigger(userId, gitRequestEvent, originYaml, buildPipeline)
        }
    }

    private fun trigger(
        userId: String,
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline
    ) {
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

        val token = oauthService.getAndCheckOauthToken(userId)
        val objects = yamlTriggerFactory.requestTriggerV2.prepareCIBuildYaml(
            gitToken = token,
            forkGitToken = null,
            gitRequestEvent = gitRequestEvent,
            isMr = false,
            originYaml = originYaml,
            filePath = buildPipeline.filePath,
            pipelineId = buildPipeline.pipelineId,
            pipelineName = buildPipeline.displayName
        )!!
        val parsedYaml = YamlCommonUtils.toYamlNotNull(objects.preYaml)
        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = gitRequestEvent.id!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = YamlUtil.toYaml(objects.normalYaml),
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            objectKind = gitRequestEvent.objectKind,
            commitMsg = null,
            triggerUser = gitRequestEvent.userId,
            sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )
        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        gitCIBasicSettingService.refreshSetting(gitRequestEvent.gitProjectId)
        yamlBuildV2.gitStartBuild(
            pipeline = buildPipeline,
            event = gitRequestEvent,
            yaml = objects.normalYaml,
            parsedYaml = parsedYaml,
            originYaml = originYaml,
            normalizedYaml = YamlUtil.toYaml(objects.normalYaml),
            gitBuildId = gitBuildId,
            isTimeTrigger = false
        )
    }
}
