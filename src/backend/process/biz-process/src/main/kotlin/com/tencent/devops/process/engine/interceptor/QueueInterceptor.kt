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

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_QUEUE_FULL
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.service.PipelineSettingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 队列拦截
 * @version 1.0
 */
@Component
class QueueInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineSettingService: PipelineSettingService
) : PipelineInterceptor {

    override fun execute(task: InterceptData): Response<BuildStatus> {
        val pipelineId = task.pipelineInfo.pipelineId
        val setting = pipelineSettingService.getSetting(pipelineId)
        val runLockType = setting?.runLockType ?: return Response(BuildStatus.RUNNING)
        return if (runLockType == PipelineRunLockType.SINGLE.ordinal ||
            runLockType == PipelineRunLockType.SINGLE_LOCK.ordinal
        ) {
            val maxQueue = setting.maxQueueSize ?: 10
            val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
            if (buildSummaryRecord == null) {
                // Summary为空，如新创建的pipeline
                Response(BuildStatus.RUNNING)
            } else if (maxQueue == 0 && buildSummaryRecord.runningCount == 0 && buildSummaryRecord.queueCount == 0) {
                // 设置了最大排队数量限制为0，但此时没有构建正在执行
                Response(BuildStatus.RUNNING)
            } else if (buildSummaryRecord.queueCount >= maxQueue) {
                // 排队数量超过最大限制
                logger.info("[$pipelineId] MaxQueue=$maxQueue| currentQueue=${buildSummaryRecord.queueCount}")
                Response(ERROR_PIPELINE_QUEUE_FULL, "流水线队列满")
            } else {
                // 满足条件
                Response(BuildStatus.RUNNING)
            }
        } else {
            Response(BuildStatus.RUNNING)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineInterceptor::class.java)
    }
}
