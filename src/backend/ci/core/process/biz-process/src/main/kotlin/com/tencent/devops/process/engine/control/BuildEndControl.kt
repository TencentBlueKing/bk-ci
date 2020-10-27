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

import com.tencent.devops.common.api.pojo.ErrorCode.PLUGIN_DEFAULT_ERROR
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.utils.PIPELINE_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
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
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val buildLogPrinter: BuildLogPrinter
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
        val buildStatus = if (BuildStatus.isFinish(status) || status.name == BuildStatus.STAGE_SUCCESS.name) {
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

        fixTask(buildInfo, buildStatus)

        // 记录本流水线最后一次构建的状态
        pipelineRuntimeService.finishLatestRunningBuild(
            latestRunningBuild = LatestRunningBuild(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                userId = buildInfo.startUser, status = buildStatus, taskCount = buildInfo.taskCount,
                buildNum = buildInfo.buildNum
            ),
            currentBuildStatus = buildInfo.status,
            errorInfoList = buildInfo.errorInfoList
        )

        // 设置状态
        pipelineBuildDetailService.buildEnd(
            buildId = buildId,
            buildStatus = buildStatus,
            errorInfos = buildInfo.errorInfoList
        )

        // 广播结束事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishBroadCastEvent(
                source = "build_finish_$buildId", projectId = projectId, pipelineId = pipelineId,
                userId = userId, buildId = buildId, status = buildStatus.name,
                startTime = buildInfo.startTime, endTime = buildInfo.endTime, triggerType = buildInfo.trigger,
                errorInfoList = if (buildInfo.errorInfoList != null) JsonUtil.toJson(buildInfo.errorInfoList!!) else null
            ),
            PipelineBuildStatusBroadCastEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = ActionType.END
            )
        )

        // 记录日志
        buildLogPrinter.stopLog(buildId = buildId, tag = "", jobId = null)
    }

    private fun PipelineBuildFinishEvent.fixTask(buildInfo: BuildInfo, buildStatus: BuildStatus) {
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

                    // 如果是取消的构建，则会统一取消子流水线的构建
                    if (BuildStatus.isCancel(buildStatus)) {
                        terminateSubPipeline(buildInfo.buildId, it)
                    }
                }
            }

            if (it.errorType != null) {
                val infos = mutableListOf<ErrorInfo>()
                if (buildInfo.errorInfoList != null) infos.addAll(buildInfo.errorInfoList!!)
                infos.add(ErrorInfo(
                    taskId = it.taskId,
                    taskName = it.taskName,
                    atomCode = it.atomCode ?: it.taskParams["atomCode"] as String? ?: it.taskType,
                    errorType = it.errorType?.num ?: ErrorType.USER.num,
                    errorCode = it.errorCode ?: PLUGIN_DEFAULT_ERROR,
                    errorMsg = CommonUtils.interceptStringInLength(it.errorMsg, PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX) ?: ""
                ))
                // 做入库长度保护，假设超过上限则抛弃该错误信息
                if (JsonUtil.toJson(infos).toByteArray().size > PIPELINE_MESSAGE_STRING_LENGTH_MAX) {
                    infos.removeAt(infos.lastIndex)
                }
                buildInfo.errorInfoList = infos
            }
        }
    }

    private fun terminateSubPipeline(buildId: String, buildTask: PipelineBuildTask) {

        if (!buildTask.subBuildId.isNullOrBlank()) {
            val subBuildInfo = pipelineRuntimeService.getBuildInfo(buildTask.subBuildId!!)
            if (subBuildInfo != null && !BuildStatus.isFinish(subBuildInfo.status)) {
                try {
                    pipelineBuildService.buildManualShutdown(
                        userId = subBuildInfo.startUser,
                        projectId = subBuildInfo.projectId,
                        pipelineId = subBuildInfo.pipelineId,
                        buildId = subBuildInfo.buildId,
                        channelCode = subBuildInfo.channelCode,
                        checkPermission = false
                    )
                } catch (e: Throwable) {
                    logger.warn("[$buildId]|TerminateSubPipeline|subBuildId=${subBuildInfo.buildId}|e=$e")
                }
            }
        }
    }

    private fun PipelineBuildFinishEvent.popNextBuild() {

        // 获取下一个排队的
        val nextQueueBuildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(projectId = projectId, pipelineId = pipelineId)
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
