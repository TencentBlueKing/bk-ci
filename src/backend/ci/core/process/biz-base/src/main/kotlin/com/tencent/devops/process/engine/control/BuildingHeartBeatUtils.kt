/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BuildingHeartBeatUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    companion object {
        private const val REDIS_EXPIRED_MIN = 30L // redis expired time in 30 minutes
        private val logger = LoggerFactory.getLogger(BuildingHeartBeatUtils::class.java)
    }

    fun addHeartBeat(buildId: String, vmSeqId: String, time: Long, retry: Int = 3, executeCount: Int? = null) {
        try {
            redisOperation.set(
                key = HeartBeatUtils.genHeartBeatKey(buildId = buildId, vmSeqId = vmSeqId, executeCount = executeCount),
                value = time.toString(),
                expiredInSecond = TimeUnit.MINUTES.toSeconds(REDIS_EXPIRED_MIN)
            )
        } catch (ignored: Throwable) {
            if (retry > 0) {
                logger.warn("[$buildId]|Fail to set heart beat variable(Job#$vmSeqId -> $time)", ignored)
                addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = time, retry = retry - 1)
            } else {
                throw ignored
            }
        }
    }

    fun dispatchHeartbeatEvent(buildInfo: BuildInfo, containerId: String) {
        val ctr = pipelineContainerService.getContainer(
            projectId = buildInfo.projectId,
            buildId = buildInfo.buildId,
            stageId = null,
            containerId = containerId
        ) ?: return
        pipelineEventDispatcher.dispatch(
            PipelineContainerAgentHeartBeatEvent(
                source = "buildVMStarted",
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                userId = buildInfo.startUser,
                buildId = buildInfo.buildId,
                containerId = containerId,
                executeCount = ctr.executeCount
            )
        )
    }

    fun dropHeartbeat(buildId: String, vmSeqId: String, executeCount: Int? = null) {
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId, executeCount))
        // 兼容旧版agent心跳接口没有传executeCount的逻辑
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId))
    }
}
