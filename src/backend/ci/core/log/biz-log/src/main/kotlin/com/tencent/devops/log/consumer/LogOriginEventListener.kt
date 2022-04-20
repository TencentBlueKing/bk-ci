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

package com.tencent.devops.log.consumer

import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.service.BuildLogPrintService
import com.tencent.devops.log.service.LogService
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component(StreamBinding.BINDING_LOG_ORIGIN_EVENT)
class LogOriginEventListener @Autowired constructor(
    private val logService: LogService,
    private val buildLogPrintService: BuildLogPrintService
) : Consumer<Message<LogOriginEvent>> {

    companion object {
        private val logger = LoggerFactory.getLogger(LogOriginEventListener::class.java)
    }

    override fun accept(message: Message<LogOriginEvent>) {
        logEvent(message.payload)
    }

    fun logEvent(event: LogOriginEvent) {
        var result = false
        try {
            logService.addLogEvent(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the log event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            if (!result && event.retryTime >= 0) {
                logger.warn("Retry to add the log event [${event.buildId}|${event.retryTime}]")
                with(event) {
                    buildLogPrintService.dispatchEvent(
                        LogOriginEvent(
                            buildId = buildId,
                            logs = logs,
                            retryTime = retryTime - 1,
                            delayMills = getNextDelayMills(retryTime)
                        )
                    )
                }
            }
        }
    }
}
