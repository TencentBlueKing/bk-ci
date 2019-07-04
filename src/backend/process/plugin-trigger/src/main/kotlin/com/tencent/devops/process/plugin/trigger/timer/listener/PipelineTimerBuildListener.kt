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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.plugin.trigger.timer.listener

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.process.api.ServiceTimerBuildResource
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线原子任务执行事件
 *
 * @version 1.0
 */
@Component
class PipelineTimerBuildListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val serviceTimerBuildResource: ServiceTimerBuildResource,
    private val pipelineTimerService: PipelineTimerService
) : BaseListener<PipelineTimerBuildEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineTimerBuildEvent) {
        with(event) {
            try {

                val buildResult = serviceTimerBuildResource.timerTrigger(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = emptyMap(),
                    channelCode = channelCode
                )

                // 如果是不存在的流水线，则直接删除定时任务，相当于给异常创建失败的定时流水线做清理
                if (buildResult.data.isNullOrBlank()) {
                    pipelineTimerService.deleteTimer(pipelineId, userId)
                    logger.error("[$pipelineId]|pipeline not exist!${buildResult.message}")
                } else {
                    logger.info("[$pipelineId]|TimerTrigger start| buildId=${buildResult.data}")
                }
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
            } catch (ignored: Throwable) {
                logger.error("[$pipelineId]|TimerTrigger fail event=$event| error=${ignored.message}", ignored)
            }
        }
    }
}
