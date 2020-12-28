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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BuildCancelControl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineMQEventDispatcher: PipelineEventDispatcher,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    @Autowired(required = false)
    private val measureService: MeasureService?
) {

    companion object {
        private const val expiredTimeInSeconds = 20L
        private const val mutexExpireSeconds: Long = 86400
        private const val mutexOpLockSeconds: Long = 60
        private val logger = LoggerFactory.getLogger(BuildCancelControl::class.java)
    }

    fun handle(event: PipelineBuildCancelEvent) {
        val watcher = Watcher(id = "BuildCancel|${event.traceId}|${event.buildId}|${event.status}")
        if (event.status == null) {
            logger.warn("[${event.buildId}]| illegal buildId in pipeline terminate event: $event")
            return
        }
        val redisLock = RedisLock(redisOperation, "process.pipeline.build.shutdown.${event.buildId}", expiredTimeInSeconds)
        try {
            watcher.start("lock")
            redisLock.lock()
            watcher.start("execute")
            execute(event.buildId, event, event.status, event.pipelineId)
        } catch (ignored: Exception) {
            logger.error("[${event.buildId}]|${event.pipelineId} build finish fail: $ignored", ignored)
        } finally {
            redisLock.unlock()
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    private fun execute(
        buildId: String,
        event: PipelineBuildCancelEvent,
        status: BuildStatus,
        pipelineId: String
    ): Boolean {

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return true
        // 已经结束的构建，不再受理，抛弃消息
        if (BuildStatus.isFinish(buildInfo.status)) {
            logger.info("[$buildId]|REPEAT_CANCEL_EVENT|event=$event| abandon!")
            return true
        }

        buildDetailService.buildCancel(buildId, status)

        val projectId = event.projectId

        logger.info("[$buildId]|CANCEL|status=${event.status}|pipelineId=$pipelineId|projectId=$projectId")

        val model = pipelineBuildDetailService.getBuildModel(buildId)
        if (model == null) {
            logger.warn("[$buildId] the model is null")
            return false
        }

        val retryCount = buildVariableService.getVariable(buildId, PIPELINE_RETRY_COUNT)
        val executeCount = if (retryCount.isNullOrEmpty()) {
            1
        } else {
            logger.info("build [$buildId] cancel retryCount[$retryCount]")
            retryCount!!.toInt() + 1
        }

        pipelineMQEventDispatcher.dispatch(
            PipelineAgentShutdownEvent(
                source = "shutdownAllVMTaskAtom",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = event.userId,
                buildId = buildId,
                buildResult = true,
                vmSeqId = null,
                executeCount = executeCount
            )
        )

        model.stages.forEach { stage ->
            stage.containers.forEach C@{ container ->

                unlockMutexGroup(container, buildId, event, pipelineId, projectId, stage)

                if (container is VMBuildContainer && container.dispatchType?.routeKeySuffix != null) {
                    val routeKeySuffix = container.dispatchType!!.routeKeySuffix!!.routeKeySuffix
                    logger.info("[$buildId] Adding the route key - ($routeKeySuffix)")
                    pipelineMQEventDispatcher.dispatch(
                        PipelineAgentShutdownEvent(
                            source = "shutdownAllVMTaskAtom",
                            projectId = projectId,
                            pipelineId = pipelineId,
                            userId = event.userId,
                            buildId = buildId,
                            buildResult = true,
                            vmSeqId = null,
                            routeKeySuffix = routeKeySuffix,
                            executeCount = executeCount
                        )
                    )
                }
            }
        }

        pipelineMQEventDispatcher.dispatch(
            PipelineBuildLessShutdownDispatchEvent(
                source = "shutdownAllBLVMTaskAtom",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = event.userId,
                buildId = buildId,
                buildResult = true,
                vmSeqId = null,
                executeCount = executeCount
            )
        )

        pipelineMQEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = "cancel_build",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = event.userId,
                buildId = buildId,
                status = status
            )
        )

        measureService?.postCancelData(projectId = projectId, pipelineId = pipelineId, buildId = buildId, userId = event.userId)

        return true
    }

    private fun unlockMutexGroup(
        container: Container,
        buildId: String,
        event: PipelineBuildCancelEvent,
        pipelineId: String,
        projectId: String,
        stage: Stage
    ) {

        if (container !is VMBuildContainer && container !is NormalContainer) {
            return
        }

        val mutexGroup = when (container) {
            is VMBuildContainer -> container.mutexGroup
            is NormalContainer -> container.mutexGroup
            else -> null
        } ?: return

        // 释放互斥锁
        // 需要替换mutex中的变量。
        val mutexGroupName = if (mutexGroup.mutexGroupName.isNullOrBlank()) {
            ""
        } else {
            val variables = buildVariableService.getAllVariable(buildId)
            EnvUtils.parseEnv(mutexGroup.mutexGroupName!!, variables)
        }
        val mutexEnable = mutexGroup.enable
        if (mutexGroupName.isNotBlank() && mutexEnable) {
            // 锁住containerController
            val redisMutexLock =
                RedisLock(redisOperation, "lock.build.$buildId.c_${container.containerId}", mutexOpLockSeconds)
            try {
                logger.info("[$buildId]|try to unlock|status=${event.status}|pipelineId=$pipelineId|projectId=$projectId")
                redisMutexLock.lock()
                val lockKey = "lock:container:mutex:$projectId:$mutexGroupName:lock"
                val queueKey = "lock:container:mutex:$projectId:$mutexGroupName:queue"
                val containerMutexId = "${buildId}_${container.id}"
                val containerMutexLock = RedisLockByValue(
                    redisOperation, lockKey, containerMutexId,
                    mutexExpireSeconds
                )
                containerMutexLock.unlock()
                redisOperation.hdelete(queueKey, containerMutexId)
                logger.info("[$buildId]|unlock mutex success|status=${event.status}|pipelineId=$pipelineId|projectId=$projectId")
                // 将container状态设置为终止
                if (!BuildStatus.isFinish(BuildStatus.valueOf(container.status ?: "RUNNING"))) {
                    pipelineRuntimeService.updateContainerStatus(
                        buildId = buildId,
                        stageId = stage.id ?: "",
                        containerId = container.id ?: "",
                        startTime = null,
                        endTime = LocalDateTime.now(),
                        buildStatus = BuildStatus.CANCELED
                    )
                }
                logger.info("[$buildId]|update status success|status=${event.status}|pipelineId=$pipelineId|projectId=$projectId")
            } finally {
                redisMutexLock.unlock()
            }
        }
    }
}
