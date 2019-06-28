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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
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
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BuildEndControl::class.java)!!
        private const val EXPIRED_SECONDS: Long = 20L
    }

    fun handle(event: PipelineBuildFinishEvent) {

        val pipelineId = event.pipelineId
        val projectId = event.projectId
        val buildId = event.buildId
        // 将状态设置正确
        val status = if (BuildStatus.isFinish(event.status)) {
            event.status
        } else {
            BuildStatus.SUCCEED
        }

        val redisLock = RedisLock(redisOperation, "process.pipeline.build.shutdown.$buildId", EXPIRED_SECONDS)
        try {
            redisLock.lock()
            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

            // 当前构建整体的状态，可能是运行中，也可能已经失败
            // 已经结束的构建，不再受理，抛弃消息
            if (buildInfo == null || BuildStatus.isFinish(buildInfo.status)) {
                logger.info("[$buildId]|REPEAT_FINISH_EVENT|event=$event| abandon!")
                return
            }

            logger.info("[$pipelineId]| finish the build[$buildId] event (${event.status})")

            dealRunningTask(buildId, event, projectId, pipelineId)

            // 设置
            doneBuild(buildInfo, status)

            // 获取下一个排队的
            nextBuild(pipelineId, projectId)
        } catch (ignored: Exception) {
            logger.error("[$buildId]|$pipelineId build finish fail: $ignored", ignored)
        } finally {
            redisLock.unlock()
        }
        return
    }

    private fun nextBuild(pipelineId: String, projectId: String) {
        val nextQueueBuildInfo = pipelineRuntimeService.getNextQueueBuildInfo(pipelineId)
        if (nextQueueBuildInfo != null) {
            pipelineEventDispatcher.dispatch(
                PipelineBuildStartEvent(
                    source = javaClass.simpleName,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = nextQueueBuildInfo.startUser,
                    buildId = nextQueueBuildInfo.buildId,
                    taskId = nextQueueBuildInfo.firstTaskId,
                    status = nextQueueBuildInfo.status,
                    actionType = ActionType.START
                )
            )
        }
    }

    private fun dealRunningTask(
        buildId: String,
        event: PipelineBuildFinishEvent,
        projectId: String,
        pipelineId: String
    ) {
        val allBuildTask = pipelineRuntimeService.getAllBuildTask(buildId)

        allBuildTask.forEach {
            // 将所有还在运行中的任务全部结束掉
            if (BuildStatus.isRunning(it.status)) {
                // 构建机直接结束
                if (it.containerType == VMBuildContainer.classType) {
                    pipelineRuntimeService.updateTaskStatus(
                        buildId = buildId, taskId = it.taskId,
                        userId = event.userId, buildStatus = BuildStatus.TERMINATE
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
        }
    }

    private fun doneBuild(buildInfo: BuildInfo, status: BuildStatus) {

        logger.info("[${buildInfo.buildId}] The build(${buildInfo.buildId}) shutdown with status($status)")

        // 记录本流水线最后一次构建的状态
        pipelineRuntimeService.finishLatestRunningBuild(
            latestRunningBuild = LatestRunningBuild(
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                buildId = buildInfo.buildId,
                userId = buildInfo.startUser,
                status = status,
                taskCount = buildInfo.taskCount,
                buildNum = buildInfo.buildNum
            ),
            currentBuildStatus = buildInfo.status
        )

        if (buildInfo.trigger == StartType.PIPELINE.name) {
            checkPipelineCall(buildInfo, status) // 通知父流水线状态
        }

        val model = pipelineRepositoryService.getModel(buildInfo.pipelineId)

        setBuildNo(buildInfo.pipelineId, model!!, status)

        // 设置状态
        pipelineBuildDetailService.buildEnd(buildInfo.buildId, status)

        // 广播结束事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishBroadCastEvent(
                source = "build_finish_${buildInfo.buildId}",
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                userId = buildInfo.startUser,
                buildId = buildInfo.buildId,
                status = status.name
            )
        )
    }

    private fun setBuildNo(pipelineId: String, model: Model, buildStatus: BuildStatus) {

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        val buildNo = triggerContainer.buildNo ?: return

        val buildNoLock = RedisLock(
            redisOperation, "process.pipeline.buildno.update.$pipelineId",
            EXPIRED_SECONDS
        )

        try {

            buildNoLock.lock()

            val buildSummary = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
            if (buildSummary == null || buildSummary.buildNo == null) {
                logger.warn("The pipeline[$pipelineId] don't has the build no")
                return
            }

            val buildNoInt = calculate(buildNo.buildNoType, buildSummary.buildNo, buildStatus)

            if (buildSummary.buildNo != buildNoInt) {
                pipelineRuntimeService.updateBuildNo(pipelineId, buildNoInt)
            }
        } finally {
            buildNoLock.unlock()
        }
    }

    private fun calculate(buildNoType: BuildNoType, currentBuildNo: Int, buildStatus: BuildStatus): Int {
        return when (buildNoType) {
            BuildNoType.CONSISTENT -> currentBuildNo
            BuildNoType.SUCCESS_BUILD_INCREMENT -> {
                if (BuildStatus.isSuccess(buildStatus)) {
                    currentBuildNo + 1
                } else {
                    currentBuildNo
                }
            }
            BuildNoType.EVERY_BUILD_INCREMENT -> currentBuildNo + 1
        }
    }

    private fun checkPipelineCall(buildInfo: BuildInfo, buildStatus: BuildStatus) {

        val parentTaskId = buildInfo.parentTaskId ?: return

        val parentBuildTask =
            pipelineRuntimeService.getBuildTask(buildInfo.parentBuildId!!, buildInfo.parentTaskId!!)
        if (parentBuildTask == null) {
            logger.error("The parent build(${buildInfo.parentBuildId}) task(${buildInfo.parentTaskId}) not exist ")
            return
        }

        logger.info("[${buildInfo.buildId}]|buildId=${parentBuildTask.buildId}|task=$parentTaskId|status=$buildStatus")

        pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                source = "sub_pipeline_build_${buildInfo.buildId}", // 来源
                projectId = parentBuildTask.projectId,
                pipelineId = parentBuildTask.pipelineId,
                userId = parentBuildTask.starter,
                buildId = parentBuildTask.buildId,
                stageId = parentBuildTask.stageId,
                containerId = parentBuildTask.containerId,
                containerType = parentBuildTask.containerType,
                taskId = parentBuildTask.taskId,
                taskParam = parentBuildTask.taskParams,
                actionType = ActionType.REFRESH
            )
        )
    }
}
