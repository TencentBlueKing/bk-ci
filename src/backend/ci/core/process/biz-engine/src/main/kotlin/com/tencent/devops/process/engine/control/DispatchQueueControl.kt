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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.utils.PIPELINE_CON_RUNNING_CONTAINER_SIZE_MAX
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class DispatchQueueControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DispatchQueueControl::class.java)
        private fun getDispatchQueueKey(buildId: String, stageId: String) =
            "pipeline:build:$buildId:stage:$stageId:dispatch:queue"
    }

    internal fun flushDispatchQueue(buildId: String, stageId: String) {
        // 构建启动和结束时，分别清空一次队列
        LOG.info("ENGINE|$buildId|FLUSH_CONTAINER_QUEUE")
        redisOperation.delete(getDispatchQueueKey(buildId, stageId))
    }

    internal fun dequeueDispatch(container: PipelineBuildContainer) {
        val queueKey = getDispatchQueueKey(container.buildId, container.stageId)
        val result = redisOperation.zremove(queueKey, container.containerId)
        redisOperation.expire(queueKey, TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS))
        LOG.info(
            "ENGINE|${container.buildId}|CONTAINER_DEQUEUE" +
                "|${container.stageId}|${container.containerId}|containerHashId=" +
                "${container.containerHashId}|result=$result"
        )
    }

    /**
     * 构建机ID不在队列中则进入队列，保证在队列内后尝试出队列
     * 出队列时，如果为超过上限则返回true，无法出队列则返回false
     */
    fun tryToDispatch(container: PipelineBuildContainer): Boolean {
        val queueKey = getDispatchQueueKey(container.buildId, container.stageId)
        var rank = redisOperation.zrank(queueKey, container.containerId)
        val canDequeue: Boolean
        when {
            rank == null -> {
                buildLogPrinter.addYellowLine(
                    buildId = container.buildId,
                    message = "[QUEUE] Dispatch queue add container(${container.containerId})",
                    tag = VMUtils.genStartVMTaskId(container.containerId),
                    jobId = null,
                    executeCount = container.executeCount
                )
                redisOperation.zadd(queueKey, container.containerId, LocalDateTime.now().timestamp().toDouble())
                redisOperation.expire(queueKey, TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS))
                rank = redisOperation.zrank(queueKey, container.containerId) ?: return false
                canDequeue = rank < PIPELINE_CON_RUNNING_CONTAINER_SIZE_MAX
            }
            rank < PIPELINE_CON_RUNNING_CONTAINER_SIZE_MAX -> {
                canDequeue = true
            }
            else -> {
                canDequeue = false
            }
        }
        LOG.info("ENGINE|${container.buildId}|TRY_TO_DEQUEUE|container(${container.containerId} rank=$rank}")
        buildLogPrinter.addYellowLine(
            buildId = container.buildId,
            message = "[QUEUE] Rank of container(${container.containerId}) is: $rank, " +
                "if can dequeue: $canDequeue",
            tag = VMUtils.genStartVMTaskId(container.containerId),
            jobId = null,
            executeCount = container.executeCount
        )
        return canDequeue
    }
}
