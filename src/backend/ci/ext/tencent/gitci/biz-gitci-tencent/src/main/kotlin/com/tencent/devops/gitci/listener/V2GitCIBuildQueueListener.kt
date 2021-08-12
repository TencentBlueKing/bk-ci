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

package com.tencent.devops.gitci.listener

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class V2GitCIBuildQueueListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(V2GitCIBuildQueueListener::class.java)
        private const val ymlVersion = "v2.0"

        // 接受广播的队列
        private const val QUEUE_PIPELINE_BUILD_QUEUE_GITCI = "q.engine.pipeline.build.queue.gitci"
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = QUEUE_PIPELINE_BUILD_QUEUE_GITCI, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineBuildQueueBroadCastEvent(buildQueueEvent: PipelineBuildQueueBroadCastEvent) {
        logger.info("listenPipelineBuildQueueBroadCastEvent: event: $buildQueueEvent")

        val projectId = buildQueueEvent.projectId
        if (!projectId.startsWith("git_")) {
            return
        }
        // 只监听定时触发
        if (buildQueueEvent.triggerType != StartType.TIME_TRIGGER.name) {
            return
        }

        // 定时任务其他actionType也会受到消息，所以buildId存在时不新增
        if (gitRequestEventBuildDao.isBuildExist(dslContext, buildQueueEvent.buildId)) {
            return
        }

        val records = gitRequestEventBuildDao.getLastEventByPipelineId(
            dslContext = dslContext,
            gitProjectId = projectId.removePrefix("git_").toLong(),
            pipelineId = buildQueueEvent.pipelineId
        )
        if (records.isEmpty()) {
            logger.error("can't find queueEvent: $buildQueueEvent pipeline in gitci")
            return
        }
        // 有定时任务过来时，复制当前流水线在工蜂CI的最后一条记录，部分数据做替换生成新的记录
        with(records.first()) {
            gitRequestEventBuildDao.saveWhole(
                dslContext = dslContext,
                eventId = eventId,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitProjectId,
                branch = branch,
                objectKind = objectKind,
                triggerUser = triggerUser,
                commitMsg = commitMessage,
                sourceGitProjectId = sourceGitProjectId,
                pipelineId = buildQueueEvent.pipelineId,
                buildId = buildQueueEvent.buildId,
                buildStatus = BuildStatus.RUNNING,
                version = ymlVersion
            )
        }
    }
}
