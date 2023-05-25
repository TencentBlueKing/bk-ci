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

package com.tencent.devops.buildless.service

import com.tencent.devops.buildless.ContainerPoolExecutor
import com.tencent.devops.buildless.client.DispatchClient
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.utils.ContainerStatus
import com.tencent.devops.buildless.utils.RedisUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.springframework.web.context.request.async.DeferredResult
import java.util.concurrent.Future

/**
 * 无构建环境Task服务
 */
@Service
class BuildLessTaskService(
    private val redisUtils: RedisUtils,
    private val dispatchClient: DispatchClient,
    private val containerPoolExecutor: ContainerPoolExecutor
) {

    @Async
    fun claimBuildLessTask(containerId: String): Future<BuildLessTask?> {
        synchronized(containerId.intern()) {
            var loopCount = 0
            while (loopCount < 100) {
                // 校验当前容器状态是否正常
                val buildLessPoolInfo = containerPoolExecutor.getContainerStatus(containerId)
                if (buildLessPoolInfo != null && buildLessPoolInfo.status == ContainerStatus.BUSY) {
                    return AsyncResult(buildLessPoolInfo.buildLessTask)
                }

                val buildLessTask = redisUtils.popBuildLessReadyTask()
                if (buildLessTask != null) {
                    try {
                        logger.info("****> container: $containerId claim buildLessTask: $buildLessTask")
                        dispatchClient.updateContainerId(
                            buildLessTask = buildLessTask,
                            containerId = containerId
                        )

                        logger.info("****> claim task buildLessPoolKey hset $containerId ${ContainerStatus.BUSY.name}.")
                        redisUtils.setBuildLessPoolContainer(containerId, ContainerStatus.BUSY, buildLessTask)

                        return AsyncResult(buildLessTask)
                    } catch (e: Exception) {
                        // 异常时任务重新回队列
                        logger.info("****> container: $containerId claim buildLessTask: $buildLessTask get error, " +
                                        "retry.", e)
                        redisUtils.leftPushBuildLessReadyTask(buildLessTask)

                        continue
                    }
                }

                loopCount++
                Thread.sleep(200)
            }

            return AsyncResult(null)
        }
    }

    fun claimBuildLessTaskDeferred(
        containerId: String,
        deferredResult: DeferredResult<BuildLessTask?>
    ) {
        synchronized(containerId.intern()) {
            var loopCount = 0
            while (loopCount < 100) {
                // 校验当前容器状态是否正常
                val buildLessPoolInfo = containerPoolExecutor.getContainerStatus(containerId)
                if (buildLessPoolInfo != null && buildLessPoolInfo.status == ContainerStatus.BUSY) {
                    deferredResult.setResult(buildLessPoolInfo.buildLessTask)
                    return
                }

                val buildLessTask = redisUtils.popBuildLessReadyTask()
                if (buildLessTask != null) {
                    logger.info("****> container: $containerId claim buildLessTask: $buildLessTask")
                    dispatchClient.updateContainerId(
                        buildLessTask = buildLessTask,
                        containerId = containerId
                    )

                    logger.info("****> claim task buildLessPoolKey hset $containerId ${ContainerStatus.BUSY.name}.")
                    redisUtils.setBuildLessPoolContainer(containerId, ContainerStatus.BUSY, buildLessTask)

                    deferredResult.setResult(buildLessTask)
                    return
                }

                loopCount++
                Thread.sleep(200)
            }

            deferredResult.setResult(null)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLessTaskService::class.java)
    }
}
