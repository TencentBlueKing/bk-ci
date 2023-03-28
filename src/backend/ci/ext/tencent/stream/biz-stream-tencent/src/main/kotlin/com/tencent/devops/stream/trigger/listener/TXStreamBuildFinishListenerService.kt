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

package com.tencent.devops.stream.trigger.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.I18NConstant.BK_NEED_SUPPLEMEN
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.BK_NEED_SUPPLEMEN
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamCIInfo
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishData
import com.tencent.devops.stream.trigger.actions.data.context.getBuildStatus
import com.tencent.devops.stream.trigger.listener.components.SendCommitCheck
import com.tencent.devops.stream.trigger.listener.components.TXSendNotify
import com.tencent.devops.stream.util.StreamTriggerMessageUtils
import com.tencent.devops.stream.v1.components.V1SendCommitCheck
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

// 因为要兼容v1只能像素级复制core代码，看未来可以下掉吗
@Suppress("ALL")
@Primary
@Service
class TXStreamBuildFinishListenerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val actionFactory: EventActionFactory,
    private val sendCommitCheck: SendCommitCheck,
    private val sendNotify: TXSendNotify,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val v1SendCommitCheck: V1SendCommitCheck,
    private val streamGitConfig: StreamGitConfig
) : StreamBuildFinishListenerService(
    dslContext = dslContext,
    objectMapper = objectMapper,
    actionFactory = actionFactory,
    sendCommitCheck = sendCommitCheck,
    sendNotify = sendNotify,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    gitRequestEventDao = gitRequestEventDao,
    streamBasicSettingDao = streamBasicSettingDao,
    gitPipelineResourceDao = gitPipelineResourceDao,
    streamGitConfig = streamGitConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXStreamBuildFinishListenerService::class.java)
    }

    override fun doFinish(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        val buildEvent = gitRequestEventBuildDao.getByBuildId(dslContext, buildFinishEvent.buildId) ?: return

        if (buildEvent.version.isNullOrBlank() || buildEvent.version != "v2.0") {
            v1SendCommitCheck.sendCommitCheckV1(
                buildId = buildFinishEvent.buildId,
                userId = buildFinishEvent.userId,
                streamBuildId = buildEvent.id,
                requestEventId = buildEvent.eventId,
                pipelineId = buildEvent.pipelineId,
                buildStatus = buildFinishEvent.status
            )
            return
        }

        val requestEvent = gitRequestEventDao.getWithEvent(dslContext, buildEvent.eventId) ?: return
        val pipelineId = buildEvent.pipelineId

        logger.info("streamBuildFinish , pipelineId : $pipelineId, buildFinishEvent: $buildFinishEvent")

        // 更新流水线执行状态
        gitRequestEventBuildDao.updateBuildStatusById(
            dslContext = dslContext,
            id = buildEvent.id,
            buildStatus = BuildStatus.valueOf(buildFinishEvent.status)
        )

        val pipeline = gitPipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = listOf(pipelineId)
        ).getOrNull(0)?.let {
            StreamTriggerPipeline(
                gitProjectId = it.gitProjectId.toString(),
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator
            )
        } ?: throw OperationException("stream pipeline not exist")

        // 改为利用pipeline信息反查projectId 保证流水线和项目是绑定的
        val setting = streamBasicSettingDao.getSetting(dslContext, pipeline.gitProjectId.toLong())?.let {
            StreamTriggerSetting(it)
        } ?: throw OperationException("stream all projectCode not exist")

        // 加载action，并填充上下文，手动和定时触发需要自己的事件
        val action = when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> when (requestEvent.objectKind) {
                StreamObjectKind.MANUAL.value -> actionFactory.loadManualAction(
                    setting = setting,
                    event = objectMapper.readValue(requestEvent.event)
                )
                StreamObjectKind.SCHEDULE.value -> actionFactory.loadScheduleAction(
                    setting = setting,
                    event = objectMapper.readValue(requestEvent.event)
                )
                StreamObjectKind.OPENAPI.value -> {
                    // openApi可以手工触发也可以模拟事件触发,所以event有两种结构
                    try {
                        actionFactory.loadManualAction(
                            setting = setting,
                            event = objectMapper.readValue(requestEvent.event)
                        )
                    } catch (ignore: Exception) {
                        actionFactory.load(objectMapper.readValue<GitEvent>(requestEvent.event))
                    }
                }
                else -> actionFactory.load(objectMapper.readValue<GitEvent>(requestEvent.event))
            } ?: throw OperationException("stream not support action ${requestEvent.event}")
            else -> TODO(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_NEED_SUPPLEMEN,
                    language = I18nUtil.getLanguage()
                )
            )
        }

        action.data.setting = setting
        action.data.context.pipeline = pipeline
        action.data.context.finishData = BuildFinishData(
            streamBuildId = buildEvent.id,
            eventId = buildEvent.eventId,
            version = buildEvent.version,
            normalizedYaml = buildEvent.normalizedYaml,
            projectId = buildFinishEvent.projectId,
            pipelineId = buildFinishEvent.pipelineId,
            userId = buildFinishEvent.userId,
            buildId = buildFinishEvent.buildId,
            status = buildFinishEvent.status,
            startTime = buildFinishEvent.startTime,
            stageId = null
        )
        action.data.context.requestEventId = requestEvent.id

        // 推送结束构建消息
        sendCommitCheck.sendCommitCheck(action)

        // 更新最后一次执行状态
        streamBasicSettingDao.updateSettingLastCiInfo(
            dslContext,
            action.data.getGitProjectId().toLong(),
            StreamCIInfo(
                enableCI = setting.enableCi,
                lastBuildMessage = StreamTriggerMessageUtils.getEventMessageTitle(
                    requestEvent
                ),
                lastBuildStatus = action.data.context.finishData!!.getBuildStatus(),
                lastBuildPipelineId = buildFinishEvent.pipelineId,
                lastBuildId = buildFinishEvent.buildId
            )
        )

        // 发送通知
        sendNotify.sendNotify(action)
    }
}
