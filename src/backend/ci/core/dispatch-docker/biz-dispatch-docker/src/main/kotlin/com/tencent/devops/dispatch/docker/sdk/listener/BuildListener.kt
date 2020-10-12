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

package com.tencent.devops.dispatch.docker.sdk.listener

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.docker.sdk.BuildFailureException
import com.tencent.devops.dispatch.docker.sdk.DispatchSdkErrorCode
import com.tencent.devops.dispatch.docker.sdk.jmx.BuildBean
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.docker.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.docker.sdk.service.DispatchService
import com.tencent.devops.dispatch.docker.sdk.service.JobQuotaService
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
interface BuildListener {

    fun getStartupQueue(): String

    fun getShutdownQueue(): String

    fun onPipelineStartup(event: PipelineBuildStartBroadCastEvent) {
        logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] The pipeline start up")
    }

    fun onPipelineShutdown(event: PipelineBuildFinishBroadCastEvent) {
        logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] The pipeline shutdown")
    }

    fun onStartup(dispatchMessage: DispatchMessage)

    fun onShutdown(event: PipelineAgentShutdownEvent)

    fun handleStartMessage(event: PipelineAgentStartupEvent) {
        DispatcherContext.setEvent(event)
        var success = false
        val dispatchService = getDispatchService()
        val jobQuotaService = getJobQuotaService()
        try {
            logger.info("Start to handle the startup message -(${DispatcherContext.getEvent()})")

            val dispatcherType = event.dispatchType
            dispatchService.log(
                event.buildId,
                event.containerHashId,
                event.vmSeqId,
                "Start docker ${dispatcherType.buildType()} for the build",
                event.executeCount
            )

            var startTime = 0L
            var errorCode = 0
            var errorMessage = ""

            try {
                startTime = System.currentTimeMillis()

                dispatchService.isRunning(event)

                if (!jobQuotaService.checkJobQuota(event, getVmType())) {
                    logger.warn("Job quota excess, return.")
                    throw BuildFailureException(ErrorType.USER, DispatchSdkErrorCode.JOB_QUOTA_EXCESS, "JOB配额超限", "JOB配额超限")
                }

                val dispatchMessage = dispatchService.build(event, getStartupQueue())
                onStartup(dispatchMessage)
                // 到这里说明JOB已经启动成功(但是不代表Agent启动成功)，开始累加使用额度
                jobQuotaService.addRunningJob(event.projectId, getVmType(), event.buildId, event.vmSeqId)
                success = true
            } catch (e: BuildFailureException) {
                dispatchService.logRed(event.buildId, event.containerHashId, event.vmSeqId, "启动构建机失败 - ${e.message}", event.executeCount)

                errorCode = e.errorCode
                errorMessage = e.formatErrorMessage

                onFailure(dispatchService, event, e)
            } catch (t: Throwable) {
                logger.warn("Fail to handle the start up message - DispatchService($event)", t)
                dispatchService.logRed(event.buildId, event.containerHashId, event.vmSeqId, "启动构建机失败 - ${t.message}", event.executeCount)

                errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR
                errorMessage = "Fail to handle the start up message"

                onFailure(dispatchService, event, BuildFailureException(ErrorType.SYSTEM, DispatchSdkErrorCode.SDK_SYSTEM_ERROR, "Fail to handle the start up message", "Fail to handle the start up message"))
            } finally {
                // 上报monitoring
                dispatchService.sendDispatchMonitoring(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    actionType = "start",
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix!!,
                    startTime = startTime,
                    stopTime = 0L,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                )
            }
        } catch (t: Throwable) {
            logger.warn("Fail to handle the start up message -($event)", t)
        } finally {
            getBuildBean().start(success)
            DispatcherContext.removeEvent()
        }
    }

    fun handleShutdownMessage(event: PipelineAgentShutdownEvent) {
        var success = false
        try {
            logger.info("Start to handle the shutdown message ($event)")
            try {
                onShutdown(event)
                val dispatchService = getDispatchService()
                // 上报monitoring
                dispatchService.sendDispatchMonitoring(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId ?: "",
                    actionType = "stop",
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix!!,
                    startTime = 0L,
                    stopTime = System.currentTimeMillis(),
                    errorCode = 0,
                    errorMessage = ""
                )

                dispatchService.shutdown(event, getStartupQueue())
                success = true
            } catch (t: Throwable) {
                logger.warn("Fail to handle the shutdown message - ($event)", t)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to handle the shutdown message - ($event)", t)
        } finally {
            val jobQuotaService = getJobQuotaService()
            jobQuotaService.removeRunningJob(event.projectId, event.buildId, event.vmSeqId)
            getBuildBean().shutdown(success)
        }
    }

    fun getVmType(): JobQuotaVmType?

    fun log(buildLogPrinter: BuildLogPrinter, buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
        )
    }

    fun logRed(buildLogPrinter: BuildLogPrinter, buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addRedLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
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
            onFailure(ErrorType.SYSTEM, DispatchSdkErrorCode.RETRY_STARTUP_FAIL, "Fail to start up the job after $retryTimes times", "Fail to start up the job after $retryTimes times")
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

    fun onAlert(users: Set<String>, alertTitle: String, message: String) {
        try {
            if (users.isEmpty()) {
                return
            }
            val emailMessage = EmailNotifyMessage().apply {
                addAllReceivers(users)
                format = EnumEmailFormat.HTML
                body = message
                title = alertTitle
                sender = "DevOps"
            }
            logger.info("Start to send the email message($message) with title($alertTitle) to users($users)")
            val result = getClient().get(ServiceNotifyResource::class).sendEmailNotify(emailMessage)
            logger.info("Get the notify result - ($result)")
        } catch (t: Throwable) {
            logger.warn("Fail to send the alert email - ($users|$alertTitle|$message)", t)
        }
    }

    fun parseMessageTemplate(content: String, data: Map<String, String>): String {
        if (content.isBlank()) {
            return content
        }
        val pattern = Pattern.compile("#\\{([^}]+)}")
        val newValue = StringBuffer(content.length)
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val key = matcher.group(1)
            val variable = data[key] ?: ""
            matcher.appendReplacement(newValue, variable)
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }

    private fun getDispatchService(): DispatchService {
        return SpringContextUtil.getBean(DispatchService::class.java)
    }

    private fun getJobQuotaService(): JobQuotaService {
        return SpringContextUtil.getBean(JobQuotaService::class.java)
    }

    private fun getBuildBean() = SpringContextUtil.getBean(BuildBean::class.java)

    private fun getClient() = SpringContextUtil.getBean(Client::class.java)

    private fun onFailure(dispatchService: DispatchService, event: PipelineAgentStartupEvent, e: BuildFailureException) {
        dispatchService.onContainerFailure(event, e)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildListener::class.java)
    }
}