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

package com.tencent.devops.stream.mq.streamTrigger

import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.trigger.v2.StreamYamlTrigger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamTriggerListener @Autowired constructor(
    private val triggerExceptionService: TriggerExceptionService,
    private val streamYamlTrigger: StreamYamlTrigger
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerListener::class.java)
    }

    fun listenStreamTriggerEvent(event: StreamTriggerEvent) {
        try {
            val traceId = MDC.get(TraceTag.BIZID)
            if (traceId.isNullOrEmpty()) {
                if (!event.traceId.isNullOrEmpty()) {
                    MDC.put(TraceTag.BIZID, event.traceId)
                } else {
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                }
            }
            run(event)
        } finally {
            MDC.remove(TraceTag.BIZID)
        }
    }

    private fun run(event: StreamTriggerEvent) {
        val startTime = System.currentTimeMillis()
        // 针对每个流水线处理异常
        triggerExceptionService.handle(
            requestEvent = event.gitRequestEventForHandle,
            gitEvent = event.event,
            basicSetting = event.gitCIBasicSetting
        ) {
            // ErrorCode都是系统错误，在最外面统一处理,都要发送无锁的commitCheck
            triggerExceptionService.handleErrorCode(
                request = event.gitRequestEventForHandle,
                event = event.event,
                pipeline = event.gitProjectPipeline,
                action = {
                    streamYamlTrigger.triggerBuild(
                        StreamTriggerContext(
                            gitEvent = event.event,
                            gitRequestEventForHandle = event.gitRequestEventForHandle,
                            streamSetting = event.gitCIBasicSetting,
                            pipeline = event.gitProjectPipeline,
                            originYaml = event.originYaml!!,
                            mrChangeSet = event.changeSet
                        )
                    )
                },
                commitCheck = CommitCheck(
                    block = false,
                    state = GitCICommitCheckState.FAILURE
                )
            )
        }
        logger.info(
            "stream pipeline: ${event.gitProjectPipeline.pipelineId} " +
                "from trigger to build time：${System.currentTimeMillis() - startTime}"
        )
    }
}
