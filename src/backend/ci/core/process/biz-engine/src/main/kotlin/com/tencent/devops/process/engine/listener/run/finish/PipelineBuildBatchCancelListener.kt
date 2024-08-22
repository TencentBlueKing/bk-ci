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

package com.tencent.devops.process.engine.listener.run.finish

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.control.BuildCancelControl
import com.tencent.devops.process.engine.pojo.event.PipelineBuildBatchCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildBatchFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线构建取消/终止事件
 *
 * @version 1.0
 */
@Component
class PipelineBuildBatchCancelListener @Autowired(required = false) constructor(
    private val buildCancelControl: BuildCancelControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildBatchCancelEvent>(pipelineEventDispatcher) {

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineBuildBatchCancelListener::class.java)
    }

    override fun run(event: PipelineBuildBatchCancelEvent) {
        val watcher = Watcher(id = "ENGINE|BuildBatchCancel|${event.traceId}|${event.buildIds}|${event.status}")
        try {
            watcher.start("execute")
            val buildIds = mutableSetOf<String>()
            event.buildIds.forEach { buildId ->
                val singleEvent = PipelineBuildCancelEvent(
                    source = event.source,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = buildId,
                    status = event.status
                )
                if (buildCancelControl.handle(singleEvent, true)) {
                    buildIds.add(buildId)
                }
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildBatchFinishEvent(
                    source = BS_CANCEL_BUILD_SOURCE,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildIds = buildIds.toList(),
                    status = event.status
                )
            )
        } catch (ignored: Exception) {
            LOG.error("ENGINE|${event.buildIds}|{${event.source}}|build finish fail: $ignored", ignored)
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }
}
