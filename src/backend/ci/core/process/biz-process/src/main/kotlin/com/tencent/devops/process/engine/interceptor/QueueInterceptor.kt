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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_QUEUE_FULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_SUMMARY_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 队列拦截, 在外面业务逻辑中需要保证Summary数据的并发控制，否则可能会出现不准确的情况
 * @version 1.0
 */
@Component
class QueueInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter
) : PipelineInterceptor {

    override fun execute(task: InterceptData): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        val setting = pipelineRepositoryService.getSetting(pipelineId)
            ?: return Response(status = PIPELINE_SETTING_NOT_EXISTS.toInt(), message = "流水线设置不存在/Setting not found")
        val runLockType = setting.runLockType

        val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
        return if (buildSummaryRecord == null) {
            // Summary为空是不正常的，抛错
            Response(status = ERROR_PIPELINE_SUMMARY_NOT_FOUND.toInt(), message = "异常：流水线的基础构建数据Summary不存在，请联系管理员")
        } else if (runLockType == PipelineRunLockType.SINGLE || runLockType == PipelineRunLockType.SINGLE_LOCK) {
            val maxQueue = setting.maxQueueSize
            if (maxQueue == 0 && buildSummaryRecord.runningCount == 0 && buildSummaryRecord.queueCount == 0) {
                // 设置了最大排队数量限制为0，但此时没有构建正在执行
                Response(data = BuildStatus.RUNNING)
            } else if (maxQueue == 0 && buildSummaryRecord.runningCount > 0) {
                Response(status = ERROR_PIPELINE_QUEUE_FULL.toInt(), message = "流水线串行，排队数设置为0")
            } else if (buildSummaryRecord.queueCount >= maxQueue) {
                // 排队数量超过最大限制
                logger.info("[$pipelineId] MaxQueue=$maxQueue| currentQueue=${buildSummaryRecord.queueCount}")
                // 排队数量已满，将该流水线最靠前的排队记录，置为"取消构建"，取消人为本次新构建的触发人
                val buildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(projectId, pipelineId)
                if (buildInfo != null) {
                    buildLogPrinter.addRedLine(
                        buildId = buildInfo.buildId,
                        message = "$pipelineId] queue outSize,cancel first Queue build",
                        tag = "QueueInterceptor",
                        jobId = "",
                        executeCount = 1
                    )
                    logger.info("$pipelineId] queue outSize,shutdown first Queue build")
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildCancelEvent(
                            source = javaClass.simpleName,
                            projectId = buildInfo.projectId,
                            pipelineId = pipelineId,
                            userId = buildSummaryRecord.latestStartUser,
                            buildId = buildInfo.buildId,
                            status = BuildStatus.CANCELED
                        )
                    )
                }
                Response(data = BuildStatus.RUNNING)
            } else {
                // 满足条件
                Response(data = BuildStatus.RUNNING)
            }
        } else {
            val maxRunningQueue = setting.maxConRunningQueueSize
            if (maxRunningQueue <= (buildSummaryRecord.queueCount + buildSummaryRecord.runningCount)) {
                Response(status = ERROR_PIPELINE_QUEUE_FULL.toInt(), message = "流水线并行构建数量达到上限: $maxRunningQueue")
            } else {
                Response(data = BuildStatus.RUNNING)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QueueInterceptor::class.java)
    }
}
