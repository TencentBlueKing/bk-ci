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

package com.tencent.devops.stream.listener

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.stream.constant.MQ as StreamMQ
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.v1.components.SendCommitCheck as V1SendCommitCheck
import com.tencent.devops.stream.listener.components.SendCommitCheck
import com.tencent.devops.stream.listener.notify.SendNotify
import com.tencent.devops.stream.pojo.v2.project.CIInfo
import com.tencent.devops.stream.utils.StreamTriggerMessageUtils
import com.tencent.devops.stream.v2.service.StreamPipelineService

@Service
class StreamBuildFinishListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamPipelineService: StreamPipelineService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val sendCommitCheck: SendCommitCheck,
    private val sendNotify: SendNotify,
    private val triggerMessageUtil: StreamTriggerMessageUtils,
    private val v1SendCommitCheck: V1SendCommitCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamBuildFinishListener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = StreamMQ.QUEUE_PIPELINE_BUILD_FINISH_STREAM, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineBuildFinishBroadCastEvent(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        try {
            val buildEvent = gitRequestEventBuildDao.getByBuildId(dslContext, buildFinishEvent.buildId)?.let {
                StreamBuildEvent(
                    id = it.id,
                    eventId = it.eventId,
                    pipelineId = it.pipelineId,
                    version = it.version,
                    normalizedYaml = it.normalizedYaml
                )
            } ?: return

            if (!buildEvent.isV2()) {
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

            val pipeline = streamPipelineService.getPipelineById(pipelineId)
                ?: throw OperationException("git ci pipeline not exist")
            // 改为利用pipeline信息反查projectId 保证流水线和项目是绑定的
            val v2GitSetting = streamBasicSettingDao.getSetting(dslContext, pipeline.gitProjectId)
                ?: throw OperationException("git ci all projectCode not exist")

            val newBuildEvent = BuildEvent(
                projectId = buildFinishEvent.projectId,
                pipelineId = buildFinishEvent.pipelineId,
                userId = buildFinishEvent.userId,
                buildId = buildFinishEvent.buildId,
                status = buildFinishEvent.status,
                startTime = buildFinishEvent.startTime
            )

            // 检查yml版本，根据yml版本选择不同的实现，构造上下文对象
            val context = StreamBuildListenerContextV2(
                buildEvent = newBuildEvent,
                requestEvent = requestEvent,
                streamBuildEvent = buildEvent,
                pipeline = pipeline,
                streamSetting = v2GitSetting
            )

            // 推送结束构建消息
            sendCommitCheck.sendCommitCheck(context)

            // 更新最后一次执行状态
            streamBasicSettingDao.updateSettingLastCiInfo(
                dslContext = dslContext,
                gitProjectId = v2GitSetting.gitProjectId,
                ciInfo = CIInfo(
                    enableCI = v2GitSetting.enableCi,
                    lastBuildMessage = triggerMessageUtil.getEventMessageTitle(
                        requestEvent
                    ),
                    lastBuildStatus = context.getBuildStatus(),
                    lastBuildPipelineId = buildFinishEvent.pipelineId,
                    lastBuildId = buildFinishEvent.buildId
                )
            )

            // 发送通知，去掉v1
            sendNotify.sendNotify(context)
        } catch (e: Throwable) {
            logger.error("Fail to listenPipelineBuildFinishBroadCastEvent(${buildFinishEvent.buildId})", e)
        }
    }
}
