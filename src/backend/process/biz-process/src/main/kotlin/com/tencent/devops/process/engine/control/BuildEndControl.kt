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

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildEndControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun handle(event: PipelineBuildFinishEvent) {

        with(event) {
            val buildIdLock = BuildIdLock(redisOperation, buildId)
            try {
                buildIdLock.lock()
                finish()
            } catch (e: Exception) {
                logger.error("[$buildId]|BUILD_FINISH_ERR|$pipelineId build finish fail: $e", e)
            } finally {
                buildIdLock.unlock()
            }

            val buildStartLock = PipelineBuildStartLock(redisOperation, pipelineId)
            try {
                buildStartLock.lock()
                popNextBuild()
            } finally {
                buildStartLock.unlock()
            }
        }
    }

    private fun PipelineBuildFinishEvent.finish() {

        // 将状态设置正确
        val buildStatus = if (BuildStatus.isFinish(status)) {
            status
        } else {
            BuildStatus.SUCCEED
        }

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

        // 当前构建整体的状态，可能是运行中，也可能已经失败
        // 已经结束的构建，不再受理，抛弃消息
        if (buildInfo == null || BuildStatus.isFinish(buildInfo.status)) {
            logger.info("[$buildId]|BUILD_FINISH_REPEAT_FINISH_EVENT|STATUS=${buildInfo?.status}| abandon!")
            return
        }

        logger.info("[$pipelineId]|BUILD_FINISH| finish the build[$buildId] event ($status)")

        if (errorType != null)
            logger.info("[ERRORCODE] PipelineBuildFinishEvent.fixTask buildInfo with <$buildId>[$errorType][$errorCode][$errorMsg] ")

        fixTask(buildInfo)

        // 记录本流水线最后一次构建的状态
        pipelineRuntimeService.finishLatestRunningBuild(
            latestRunningBuild = LatestRunningBuild(
                pipelineId = pipelineId, buildId = buildId,
                userId = buildInfo.startUser, status = buildStatus, taskCount = buildInfo.taskCount,
                buildNum = buildInfo.buildNum
            ),
            currentBuildStatus = buildInfo.status,
            errorType = buildInfo.errorType,
            errorCode = buildInfo.errorCode,
            errorMsg = buildInfo.errorMsg
        )

        // 设置状态
        pipelineBuildDetailService.buildEnd(
            buildId = buildId,
            buildStatus = buildStatus,
            errorType = buildInfo.errorType,
            errorCode = buildInfo.errorCode,
            errorMsg = buildInfo.errorMsg
        )

        // 广播结束事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishBroadCastEvent(
                source = "build_finish_$buildId", projectId = projectId, pipelineId = pipelineId,
                userId = userId, buildId = buildId, status = buildStatus.name,
                startTime = buildInfo.startTime, endTime = buildInfo.endTime, triggerType = buildInfo.trigger,
                errorType = if (buildInfo.errorType == null) null else buildInfo.errorType!!.name,
                errorCode = buildInfo.errorCode, errorMsg = buildInfo.errorMsg
            )
        )
    }

    private fun PipelineBuildFinishEvent.fixTask(buildInfo: BuildInfo) {
        val allBuildTask = pipelineRuntimeService.getAllBuildTask(buildId)

        allBuildTask.forEach {
            // 将所有还在运行中的任务全部结束掉
            if (BuildStatus.isRunning(it.status)) {
                // 构建机直接结束
                if (it.containerType == VMBuildContainer.classType) {
                    pipelineRuntimeService.updateTaskStatus(
                        buildId = buildId, taskId = it.taskId,
                        userId = userId, buildStatus = BuildStatus.TERMINATE,
                        errorType = errorType, errorCode = errorCode, errorMsg = errorMsg
                    )
                } else {
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildAtomTaskEvent(
                            source = javaClass.simpleName,
                            projectId = projectId, pipelineId = pipelineId, userId = it.starter,
                            stageId = it.stageId, buildId = it.buildId, containerId = it.containerId,
                            containerType = it.containerType, taskId = it.taskId,
                            taskParam = it.taskParams, actionType = ActionType.TERMINATE
                        )
                    )
                }
            }
            // 优先保存SYSTEM错误，若无SYSTEM错误，将BuildData中错误信息更新为编排顺序的最后一个Element错误
            if (it.errorType != null && buildInfo.errorType != ErrorType.SYSTEM) {
                buildInfo.errorType = it.errorType
                buildInfo.errorCode = it.errorCode
                buildInfo.errorMsg = it.errorMsg
            }
        }
        with(buildInfo) {
            logger.info("[ERRORCODE] PipelineBuildFinishEvent.fixTask buildInfo with <$buildId>[$errorType][$errorCode][$errorMsg] ")
        }
    }

    private fun PipelineBuildFinishEvent.popNextBuild() {

        // 获取下一个排队的
        val nextQueueBuildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(pipelineId)
        if (nextQueueBuildInfo == null) {
            logger.info("[$buildId]|FETCH_QUEUE|$pipelineId no queue build!")
            return
        }

        logger.info("[$buildId]|FETCH_QUEUE|next build: ${nextQueueBuildInfo.buildId} ${nextQueueBuildInfo.status}")
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "build_finish_$buildId",
                projectId = nextQueueBuildInfo.projectId,
                pipelineId = nextQueueBuildInfo.pipelineId,
                userId = nextQueueBuildInfo.startUser,
                buildId = nextQueueBuildInfo.buildId,
                taskId = nextQueueBuildInfo.firstTaskId,
                status = nextQueueBuildInfo.status,
                actionType = ActionType.START
            )
        )
    }
}
