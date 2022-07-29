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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_QUEUE_FULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_SUMMARY_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.service.PipelineRedisService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 队列拦截, 在外面业务逻辑中需要保证Summary数据的并发控制，否则可能会出现不准确的情况
 * @version 1.0
 */
@Component
@Suppress("ALL")
class QueueInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineTaskService: PipelineTaskService,
    private val redisOperation: RedisOperation,
    private val pipelineRedisService: PipelineRedisService
) : PipelineInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(QueueInterceptor::class.java)
    }

    override fun execute(task: InterceptData): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        val setting = task.setting
            ?: return Response(
                status = PIPELINE_SETTING_NOT_EXISTS.toInt(),
                message = "流水线设置不存在/Setting not found"
            )
        val runLockType = setting.runLockType
        val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId)
        return when {
            buildSummaryRecord == null ->
                // Summary为空是不正常的，抛错
                Response(
                    status = ERROR_PIPELINE_SUMMARY_NOT_FOUND.toInt(),
                    message = "异常：流水线的基础构建数据Summary不存在，请联系管理员"
                )
            runLockType == PipelineRunLockType.SINGLE || runLockType == PipelineRunLockType.SINGLE_LOCK ->
                checkRunLockWithSingleType(
                    task = task,
                    latestBuildId = buildSummaryRecord.latestBuildId,
                    latestStartUser = buildSummaryRecord.latestStartUser,
                    runningCount = buildSummaryRecord.runningCount,
                    queueCount = buildSummaryRecord.queueCount
                )
            runLockType == PipelineRunLockType.GROUP_LOCK ->
                checkRunLockWithGroupType(
                    task = task,
                    latestBuildId = buildSummaryRecord.latestBuildId,
                    latestStartUser = buildSummaryRecord.latestStartUser,
                    runningCount = buildSummaryRecord.runningCount,
                    queueCount = buildSummaryRecord.queueCount
                )
            setting.maxConRunningQueueSize!! <= (buildSummaryRecord.queueCount + buildSummaryRecord.runningCount) ->
                Response(
                    status = ERROR_PIPELINE_QUEUE_FULL.toInt(),
                    message = "并行上限/Max parallel: ${setting.maxConRunningQueueSize}"
                )
            else -> Response(data = BuildStatus.RUNNING)
        }
    }

    private fun checkRunLockWithSingleType(
        task: InterceptData,
        latestBuildId: String?,
        latestStartUser: String?,
        runningCount: Int,
        queueCount: Int
    ): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        val setting = task.setting
            ?: return Response(
                status = PIPELINE_SETTING_NOT_EXISTS.toInt(),
                message = "流水线设置不存在/Setting not found"
            )
        return when {
            // 如果最后一次构建被标记为refresh,则即便是串行也放行。因refresh的buildId都会被取消掉
            latestBuildId == null || pipelineRedisService.getBuildRestartValue(latestBuildId) != null ->
                Response(data = BuildStatus.RUNNING)
            // 设置了最大排队数量限制为0，但此时没有构建正在执行
            setting.maxQueueSize == 0 && runningCount == 0 && queueCount == 0 ->
                Response(data = BuildStatus.RUNNING)
            setting.maxQueueSize == 0 && (runningCount > 0 || queueCount > 0) ->
                Response(
                    status = ERROR_PIPELINE_QUEUE_FULL.toInt(),
                    message = "流水线串行，排队数设置为0"
                )
            queueCount >= setting.maxQueueSize -> {
                // 排队数量超过最大限制,排队数量已满，将该流水线最靠前的排队记录，置为"取消构建"
                val buildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildStatus = BuildStatus.UNEXEC
                )
                if (buildInfo != null) {
                    buildLogPrinter.addRedLine(
                        buildId = buildInfo.buildId,
                        message = "$pipelineId] queue outSize,cancel first Queue build",
                        tag = "QueueInterceptor",
                        jobId = "",
                        executeCount = 1
                    )
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildCancelEvent(
                            source = javaClass.simpleName,
                            projectId = buildInfo.projectId,
                            pipelineId = pipelineId,
                            userId = latestStartUser ?: task.pipelineInfo.creator,
                            buildId = buildInfo.buildId,
                            status = BuildStatus.CANCELED
                        )
                    )
                }
                Response(data = BuildStatus.RUNNING)
            }
            // 满足条件
            else ->
                Response(data = BuildStatus.RUNNING)
        }
    }

    private fun checkRunLockWithGroupType(
        task: InterceptData,
        latestBuildId: String?,
        latestStartUser: String?,
        runningCount: Int,
        queueCount: Int
    ): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val setting = task.setting
            ?: return Response(
                status = PIPELINE_SETTING_NOT_EXISTS.toInt(),
                message = "流水线设置不存在/Setting not found"
            )
        val concurrencyGroup = setting.concurrencyGroup
        return when {
            !concurrencyGroup.isNullOrBlank() -> {
                if (setting.concurrencyCancelInProgress) {
                    // cancel-in-progress: true时， 若有相同 group 的流水线正在执行，则取消正在执行的流水线，新来的触发开始执行
                    val status = listOf(BuildStatus.RUNNING, BuildStatus.QUEUE)
                    pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
                        projectId = projectId,
                        concurrencyGroup = concurrencyGroup,
                        status = status
                    ).forEach { (pipelineId, buildId) ->
                        cancelBuildPipeline(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            userId = latestStartUser ?: task.pipelineInfo.creator,
                            groupName = concurrencyGroup
                        )
                    }
                    Response(data = BuildStatus.RUNNING)
                } else {
                    // cancel-in-progress: false时，保持原有single逻辑
                    checkRunLockWithSingleType(
                        task = task,
                        latestBuildId = latestBuildId,
                        latestStartUser = latestStartUser,
                        runningCount = runningCount,
                        queueCount = queueCount
                    )
                }
            }
            // 满足条件
            else ->
                Response(data = BuildStatus.RUNNING)
        }
    }

    private fun cancelBuildPipeline(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        groupName: String
    ) {
        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        try {
            redisLock.lock()
            val tasks = pipelineTaskService.getRunningTask(projectId, buildId)
            tasks.forEach { task ->
                val taskId = task["taskId"]?.toString() ?: ""
                logger.info("build($buildId) shutdown by $userId, taskId: $taskId, status: ${task["status"] ?: ""}")
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "[concurrency]Canceling since a higher priority waiting request for " +
                            "group($groupName) exists",
                    tag = taskId,
                    jobId = task["containerId"]?.toString() ?: "",
                    executeCount = task["executeCount"] as? Int ?: 1
                )
            }
            if (tasks.isEmpty()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "[concurrency]Canceling all since a higher priority waiting request for " +
                            "group($groupName) exists",
                    tag = "QueueInterceptor",
                    jobId = "",
                    executeCount = 1
                )
            }
            try {
                pipelineRuntimeService.cancelBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = userId,
                    buildStatus = BuildStatus.CANCELED
                )
                logger.info("Cancel the pipeline($pipelineId) of instance($buildId) by the user($userId)")
            } catch (t: Throwable) {
                logger.warn("Fail to shutdown the build($buildId) of pipeline($pipelineId)", t)
            }
        } finally {
            redisLock.unlock()
        }
    }
}
