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

package com.tencent.devops.dispatch.docker.client

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.dispatch.docker.client.context.BuildLessStartHandlerContext
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessStartPrepareHandler @Autowired constructor(
    private val bkTag: BkTag,
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val buildLessStartDispatchHandler: BuildLessStartDispatchHandler
) : Handler<BuildLessStartHandlerContext>() {
    private val logger = LoggerFactory.getLogger(BuildLessStartPrepareHandler::class.java)

    override fun handlerRequest(handlerContext: BuildLessStartHandlerContext) {
        with(handlerContext) {
            // 区分是否灰度环境
            handlerContext.grayEnv = bkTag.getFinalTag().contains("gray")

            // 设置日志打印关键字
            handlerContext.buildLogKey = "${event.pipelineId}|${event.buildId}|${event.vmSeqId}|$retryTime"
            logger.info("$buildLogKey start select buildLess.")

            // Check if the pipeline is running
            checkPipelineRunning(event)

            if (event.retryTime == 0) {
                buildLogPrinter.addLine(
                    buildId = event.buildId,
                    message = "Prepare BuildLess Job(#${event.vmSeqId})...",
                    tag = VMUtils.genStartVMTaskId(event.vmSeqId),
                    jobId = event.containerHashId,
                    executeCount = event.executeCount ?: 1
                )
            }

            buildLessStartDispatchHandler.handlerRequest(this)
        }
    }

    private fun checkPipelineRunning(event: PipelineBuildLessStartupDispatchEvent) {
        // 判断流水线当前container是否在运行中
        val statusResult = client.get(ServicePipelineTaskResource::class).getTaskStatus(
            projectId = event.projectId,
            buildId = event.buildId,
            taskId = VMUtils.genStartVMTaskId(event.containerId)
        )

        if (statusResult.isNotOk() || statusResult.data == null) {
            logger.warn(
                "The build event($event) fail to check if pipeline task is running " +
                    "because of ${statusResult.message}"
            )
            throw BuildFailureException(
                errorType = ErrorType.SYSTEM,
                errorCode = DispatchSdkErrorCode.PIPELINE_STATUS_ERROR,
                formatErrorMessage = "无法获取流水线JOB状态，构建停止",
                errorMessage = "无法获取流水线JOB状态，构建停止"
            )
        }

        if (!statusResult.data!!.isRunning()) {
            logger.warn("The build event($event) is not running")
            throw BuildFailureException(
                errorType = ErrorType.USER,
                errorCode = DispatchSdkErrorCode.PIPELINE_NOT_RUNNING,
                formatErrorMessage = "流水线JOB已经不再运行，构建停止",
                errorMessage = "流水线JOB已经不再运行，构建停止"
            )
        }
    }
}
