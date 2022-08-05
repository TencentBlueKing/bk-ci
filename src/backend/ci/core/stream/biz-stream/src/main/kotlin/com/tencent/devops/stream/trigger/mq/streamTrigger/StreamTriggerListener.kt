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

package com.tencent.devops.stream.trigger.mq.streamTrigger

import com.tencent.devops.stream.trigger.StreamYamlTrigger
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamTriggerListener @Autowired constructor(
    private val exceptionHandler: StreamTriggerExceptionHandler,
    private val streamYamlTrigger: StreamYamlTrigger,
    private val actionFactory: EventActionFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerListener::class.java)
    }

    fun listenStreamTriggerEvent(event: StreamTriggerEvent) {
        try {
            run(event)
        } catch (e: Throwable) {
            logger.error("BKSystemErrorMonitor|listenStreamTriggerEvent|error", e)
        }
    }

    private fun run(event: StreamTriggerEvent) {
        val action = try {
            val action = actionFactory.loadByData(
                eventStr = event.eventStr,
                actionCommonData = event.actionCommonData,
                actionContext = event.actionContext,
                actionSetting = event.actionSetting
            )
            if (action == null) {
                logger.warn("StreamTriggerListener|run|$event")
                return
            }
            action
        } catch (e: Throwable) {
            logger.warn("StreamTriggerListener|load|action|error", e)
            return
        }
        logger.info(
            "StreamTriggerListener|${action.data.context.requestEventId} " +
                "|listenStreamTriggerEvent|action|${action.format()}"
        )

        // 针对每个流水线处理异常
        exceptionHandler.handle(action = action) {
            streamYamlTrigger.checkAndTrigger(action = action, trigger = event.trigger)
        }
    }
}
