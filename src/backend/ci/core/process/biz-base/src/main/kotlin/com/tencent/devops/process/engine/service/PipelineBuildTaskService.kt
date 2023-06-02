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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.control.FastKillUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.utils.BuildUtils
import com.tencent.devops.process.util.TaskUtils
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildTaskService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    /**
     * 对结束的任务[buildTask], 根据状态[buildStatus]是否失败，以及[buildTask]配置：
     * 1. 需要失败重试，将[buildTask]的构建状态设置为RETRY
     */
    fun finishTask(
        buildTask: PipelineBuildTask,
        buildStatus: BuildStatus,
        actionType: ActionType,
        source: String,
        sendEventFlag: Boolean = true
    ) {
        val buildId = buildTask.buildId
        val projectId = buildTask.projectId
        val pipelineId = buildTask.pipelineId
        val taskId = buildTask.taskId
        val containerId = buildTask.containerId
        val stageId = buildTask.stageId
        if (buildStatus == BuildStatus.CANCELED) {
            // 删除redis中取消构建操作标识
            redisOperation.delete(BuildUtils.getCancelActionBuildKey(buildId))
            redisOperation.delete(TaskUtils.getCancelTaskIdRedisKey(buildId, containerId, false))
            // 当task任务是取消状态时，把taskId存入redis供心跳接口获取
            val cancelTaskKey = TaskUtils.getCancelTaskIdRedisKey(buildId, containerId)
            redisOperation.leftPush(cancelTaskKey, taskId)
            // 为取消任务设置最大超时时间，防止构建异常产生的脏数据
            redisOperation.expire(cancelTaskKey, TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS))
        }
        // 如果是取消的构建，则会统一取消子流水线的构建
        if (buildStatus.isPassiveStop() || buildStatus.isCancel()) {
            terminateSubPipeline(buildId, buildTask)
        }
        if (sendEventFlag) {
            // 失败的任务并且不是需要前置终止的情况才允许自动重试
            val errorCode = buildTask.errorCode ?: 0
            if (buildStatus.isFailure() && !actionType.isTerminate() && !FastKillUtils.isTerminateCode(errorCode)) {
                // 如果配置了失败重试，且重试次数上线未达上限，则将状态设置为重试，让其进入
                if (pipelineTaskService.isRetryWhenFail(buildTask.projectId, taskId, buildId)) {
                    logger.info("ENGINE|$buildId|$source|ATOM_FIN|$stageId|j($containerId)|t($taskId)|RetryFail")
                    pipelineTaskService.updateTaskStatus(
                        task = buildTask, userId = buildTask.starter, buildStatus = BuildStatus.RETRY
                    )
                } else {
                    // 如果配置了失败继续，则继续下去的行为是在ContainerControl处理，而非在Task
                    pipelineTaskService.createFailTaskVar(
                        buildId = buildId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = taskId
                    )
                }
            } else {
                // 清除该原子内的重试记录
                pipelineTaskService.removeRetryCache(buildId, taskId)
                // 清理插件错误信息（重试插件成功的情况下）
                pipelineTaskService.removeFailTaskVar(
                    buildId = buildId, projectId = projectId, pipelineId = pipelineId, taskId = taskId
                )
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(
                    source = "from_t($taskId)",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = buildTask.starter,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerHashId = buildTask.containerHashId,
                    containerType = buildTask.containerType,
                    actionType = actionType,
                    errorCode = errorCode,
                    errorTypeName = buildTask.errorType?.getI18n(I18nUtil.getDefaultLocaleLanguage()),
                    reason = buildTask.errorMsg
                )
            )
        }
    }

    private fun terminateSubPipeline(buildId: String, buildTask: PipelineBuildTask) {
        if (buildTask.subBuildId.isNullOrBlank()) {
            return
        }
        val subBuildInfo = pipelineRuntimeService.getBuildInfo(buildTask.subProjectId!!, buildTask.subBuildId!!)
        if (subBuildInfo?.status?.isFinish() == false) { // 子流水线状态为未构建结束的，开始下发退出命令
            try {
                val tasks = pipelineTaskService.getRunningTask(subBuildInfo.projectId, subBuildInfo.buildId)
                tasks.forEach { task ->
                    val taskId = task["taskId"] ?: ""
                    val containerId = task["containerId"] ?: ""
                    val executeCount = task["executeCount"] ?: 1
                    buildLogPrinter.addYellowLine(
                        buildId = buildId,
                        message = "Cancelled by pipeline[${buildTask.pipelineId}]，Operator:${buildTask.starter}",
                        tag = taskId.toString(),
                        jobId = containerId.toString(),
                        executeCount = executeCount as Int
                    )
                }
                if (tasks.isEmpty()) {
                    buildLogPrinter.addYellowLine(
                        buildId = buildId,
                        message = "cancelled by pipeline[${buildTask.pipelineId}]，Operator:${buildTask.starter}",
                        tag = "",
                        jobId = "",
                        executeCount = 1
                    )
                }
                pipelineRuntimeService.cancelBuild(
                    projectId = subBuildInfo.projectId,
                    pipelineId = subBuildInfo.pipelineId,
                    buildId = subBuildInfo.buildId,
                    userId = subBuildInfo.startUser,
                    executeCount = subBuildInfo.executeCount ?: 1,
                    buildStatus = BuildStatus.CANCELED
                )
            } catch (ignored: Exception) {
                logger.warn("ENGINE|$buildId|TerminateSubPipeline|subBuildId=${subBuildInfo.buildId}|e=$ignored")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildTask::class.java)
    }
}
