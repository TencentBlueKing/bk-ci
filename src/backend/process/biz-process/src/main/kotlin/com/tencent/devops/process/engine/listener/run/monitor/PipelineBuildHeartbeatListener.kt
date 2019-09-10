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

package com.tencent.devops.process.engine.listener.run.monitor

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线构建心跳事件
 *
 * @version 1.0
 */
@Component
class PipelineBuildHeartbeatListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate
) : BaseListener<PipelineContainerAgentHeartBeatEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineContainerAgentHeartBeatEvent) {
        with(event) {
            val lastUpdate = redisOperation.get(HeartBeatUtils.genHeartBeatKey(buildId, containerId))
                ?: return
            logger.info("[$buildId]|$source heart beat for container($containerId)")
            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return
            if (BuildStatus.isFinish(buildInfo.status)) {
                logger.info("[$buildId]|The build is ${buildInfo.status}")
                return
            }
            val elapse = System.currentTimeMillis() - lastUpdate.toLong()
            if (elapse > TIMEOUT_IN_MS) {
                logger.error("The build($buildId) is timeout for ${elapse}ms, terminate it")
                LogUtils.addRedLine(rabbitTemplate, buildId, "构建任务对应的Agent的心跳超时，请检查Agent的状态", "", 1)
                var stageId: String? = null
                var containerType = "vmBuild"
                val modelDetail = buildDetailService.get(buildId) ?: return
                run outer@{
                    modelDetail.model.stages.forEach { stage ->
                        stage.containers.forEach { c ->
                            if (c.id == containerId) {
                                stageId = stage.id!!
                                containerType = c.getClassType()
                                return@outer
                            }
                        }
                    }
                }

                if (stageId.isNullOrBlank()) {
                    logger.warn("[$buildId]|heartbeat timeout|can not find stage")
                    return
                }

                // 终止当前容器下的任务
                pipelineEventDispatcher.dispatch(
                    PipelineBuildContainerEvent(
                        "heartbeat_timeout",
                        projectId, pipelineId, userId, buildId,
                        stageId!!, containerId, containerType, ActionType.TERMINATE
                    )
                )
            } else {
                // 正常是继续循环检查当前消息
                pipelineEventDispatcher.dispatch(event)
            }
        }
        return
    }

    companion object {
        private const val TIMEOUT_IN_MS = 2 * 60 * 1000 // timeout in 2 minutes
    }
}
