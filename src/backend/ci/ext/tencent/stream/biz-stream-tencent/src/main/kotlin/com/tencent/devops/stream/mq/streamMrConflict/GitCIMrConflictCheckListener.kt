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

package com.tencent.devops.stream.mq.streamMrConflict

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.stream.config.StreamStorageBean
import com.tencent.devops.stream.constant.MQ
import com.tencent.devops.stream.trigger.GitCITriggerService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIMrConflictCheckListener @Autowired
constructor(
    private val mergeConflictCheck: MergeConflictCheck,
    private val gitCITriggerService: GitCITriggerService,
    private val rabbitTemplate: RabbitTemplate,
    private val triggerExceptionService: TriggerExceptionService,
    private val streamStorageBean: StreamStorageBean
) {

    @RabbitListener(
        bindings = [(QueueBinding(
            key = [MQ.ROUTE_STREAM_MR_CONFLICT_CHECK_EVENT],
            value = Queue(value = MQ.QUEUE_STREAM_MR_CONFLICT_CHECK_EVENT, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_STREAM_MR_CONFLICT_CHECK_EVENT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenGitCIRequestTriggerEvent(checkEvent: GitCIMrConflictCheckEvent) {
        val start = LocalDateTime.now().timestampmilli()

        val (isFinish, isTrigger) = with(checkEvent) {
            mergeConflictCheck.checkMrConflictByListener(
                token = token,
                gitProjectConf = gitProjectConf,
                path2PipelineExists = path2PipelineExists,
                event = event,
                gitRequestEvent = gitRequestEvent,
                isEndCheck = retryTime == 1,
                notBuildRecordId = notBuildRecordId
            )
        }
        // 未检查完成，继续进入延时队列
        if (!isFinish && checkEvent.retryTime > 0) {
            logger.warn(
                "Retry to check gitci mr request conflict " +
                        "event [${checkEvent.gitRequestEvent}|${checkEvent.retryTime}]"
            )
            checkEvent.retryTime--
            GitCIMrConflictCheckDispatcher.dispatch(rabbitTemplate, checkEvent)
        } else {
            if (isTrigger) {
                triggerExceptionService.handle(
                    requestEvent = checkEvent.gitRequestEvent,
                    gitEvent = checkEvent.event,
                    basicSetting = checkEvent.gitProjectConf
                ) {
                    triggerExceptionService.handleErrorCode(request = checkEvent.gitRequestEvent) {
                        gitCITriggerService.matchAndTriggerPipeline(
                            gitRequestEvent = checkEvent.gitRequestEvent,
                            event = checkEvent.event,
                            path2PipelineExists = checkEvent.path2PipelineExists,
                            gitProjectConf = checkEvent.gitProjectConf
                        )
                    }
                }
            }
            streamStorageBean.conflictTime(LocalDateTime.now().timestampmilli() - start)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIMrConflictCheckListener::class.java)
    }
}
