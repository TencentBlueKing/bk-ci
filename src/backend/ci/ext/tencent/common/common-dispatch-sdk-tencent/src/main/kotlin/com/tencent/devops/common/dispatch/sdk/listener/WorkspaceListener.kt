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

package com.tencent.devops.common.dispatch.sdk.listener

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.dispatch.sdk.utils.DispatchLogRedisUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component@Suppress("ALL")
interface WorkspaceListener {

    fun onWorkspaceCreate(event: WorkspaceCreateEvent)

    fun onWorkspaceOperate(event: WorkspaceOperateEvent)

    @BkTimed
    fun handleWorkspaceCreate(event: WorkspaceCreateEvent) {
        try {
            logger.info("Start to handle the startup message -(${DispatcherContext.getEvent()})")
            onWorkspaceCreate(event)
        } catch (e: BuildFailureException) {
            //onFailure(dispatchService, event, e)
        } catch (t: Throwable) {
            // TODO
        } finally {
            DispatcherContext.removeEvent()
        }
    }

    fun handleWorkspaceOperateMessage(event: WorkspaceOperateEvent) {
        try {
            logger.info("Start to handle the shutdown message ($event)")
            try {
                onWorkspaceOperate(event)
            } catch (t: Throwable) {
                logger.warn("Fail to handle the shutdown message - ($event)", t)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to handle the shutdown message - ($event)", t)
        }
    }

    fun getVmType(): JobQuotaVmType?

    fun log(
        buildLogPrinter: BuildLogPrinter,
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?
    ) {
        buildLogPrinter.addLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = containerHashId,
            executeCount = executeCount ?: 1
        )
    }

    fun logRed(
        buildLogPrinter: BuildLogPrinter,
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?
    ) {
        buildLogPrinter.addRedLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = containerHashId,
            executeCount = executeCount ?: 1
        )
    }

    fun retry(sleepTimeInMS: Int = 30000, retryTimes: Int = 3): Boolean {
        val event = DispatcherContext.getEvent()
        if (event == null) {
            logger.warn("The event is empty")
            return false
        }
        logger.info("Retry the event($event) in $sleepTimeInMS ms")
        if (event.retryTime > retryTimes) {
            logger.warn("Fail to dispatch the agent start event with $retryTimes times - ($event)")
            onFailure(errorType = ErrorType.SYSTEM,
                errorCode = DispatchSdkErrorCode.RETRY_STARTUP_FAIL,
                formatErrorMessage = "Fail to start up the job after $retryTimes times",
                message = "Fail to start up the job after $retryTimes times")
        }
        val sleepTime = if (sleepTimeInMS <= 5000) {
            // 重试不能低于5秒
            logger.warn("The retry time is less than 5 seconds, use 5 as default")
            5000
        } else {
            sleepTimeInMS
        }
        event.retryTime += 1
        event.delayMills = sleepTime
        getDispatchService().redispatch(event)
        return true
    }

    fun onFailure(errorType: ErrorType, errorCode: Int, formatErrorMessage: String, message: String) {
        throw BuildFailureException(errorType, errorCode, formatErrorMessage, message)
    }

    private fun getDispatchService(): DispatchService {
        return SpringContextUtil.getBean(DispatchService::class.java)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceListener::class.java)
    }
}
