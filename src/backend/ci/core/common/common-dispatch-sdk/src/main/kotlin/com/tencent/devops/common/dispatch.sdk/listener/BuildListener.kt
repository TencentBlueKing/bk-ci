/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_FAILED_START_BUILD_MACHINE
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.dispatch.sdk.utils.DispatchLogRedisUtils
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
@Suppress("ALL")
interface BuildListener {

    fun getStartupQueue(): String

    fun getStartupDemoteQueue(): String

    fun getShutdownQueue(): String

    fun onStartup(dispatchMessage: DispatchMessage)

    fun onStartupDemote(dispatchMessage: DispatchMessage)

    fun onShutdown(event: PipelineAgentShutdownEvent)

    fun consumerFilter(dispatchType: DispatchType): Boolean

    @BkTimed
    fun handleStartup(event: PipelineAgentStartupEvent) {
        // 根据dispatchType筛选消息消费
        if (!consumerFilter(event.dispatchType)) {
            return
        }
        DispatcherContext.setEvent(event)
        val dispatchService = getDispatchService()

        var startTime = 0L
        var errorCode = 0
        var errorMessage = ""
        var errorType: ErrorType? = null

        try {
            logger.info("Start to handle the startup message -(${DispatcherContext.getEvent()})")

            startTime = System.currentTimeMillis()
            DispatchLogRedisUtils.setRedisExecuteCount(event.buildId, event.executeCount)

            // 校验流水线是否在运行中，且处在构建机未启动状态
            if (!dispatchService.checkRunning(event)) {
                if (event.retryTime > 1) {
                    // 重试的请求如果流水线已结束，主动把配额记录删除
                    getJobQuotaService().removeRunningJob(
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        buildId = event.buildId,
                        vmSeqId = event.vmSeqId,
                        executeCount = event.executeCount
                    )
                }

                return
            }

            // 校验构建资源配额是否超限，配额超限后会放进延迟队列
            val jobQuotaService = getJobQuotaService()
            if (!jobQuotaService.checkAndAddRunningJob(
                    startupEvent = event,
                    jobType = getVmType(),
                    demoteQueueRouteKeySuffix = getStartupDemoteQueue()
                )
            ) {
                return
            }

            onStartup(dispatchService.buildDispatchMessage(event))
        } catch (e: BuildFailureException) {
            dispatchService.logRed(
                buildId = event.buildId,
                containerHashId = event.containerHashId,
                vmSeqId = event.vmSeqId,
                message = "${I18nUtil.getCodeLanMessage(BK_FAILED_START_BUILD_MACHINE)}- ${e.message}",
                executeCount = event.executeCount,
                jobId = event.jobId
            )

            errorCode = e.errorCode
            errorMessage = e.formatErrorMessage
            errorType = e.errorType

            dispatchService.onFailure(event, e)
        } catch (t: Throwable) {
            logger.warn("Fail to handle the start up message - DispatchService($event)", t)
            dispatchService.logRed(
                buildId = event.buildId,
                containerHashId = event.containerHashId,
                vmSeqId = event.vmSeqId,
                message = "${I18nUtil.getCodeLanMessage(BK_FAILED_START_BUILD_MACHINE)} - ${t.message}",
                executeCount = event.executeCount,
                jobId = event.jobId
            )

            errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR
            errorMessage = "Fail to handle the start up message"
            errorType = ErrorType.SYSTEM

            dispatchService.onFailure(
                event = event,
                e = BuildFailureException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR,
                    formatErrorMessage = "Fail to handle the start up message",
                    errorMessage = "Fail to handle the start up message"
                )
            )
        } finally {
            DispatcherContext.removeEvent()

            // 上报monitoring，做SLA统计
            dispatchService.sendDispatchMonitoring(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                actionType = "start",
                retryTime = event.retryTime,
                routeKeySuffix = event.routeKeySuffix ?: "",
                startTime = startTime,
                stopTime = 0L,
                errorCode = errorCode,
                errorMessage = errorMessage,
                errorType = errorType
            )
        }
    }

    fun handleShutdownMessage(event: PipelineAgentShutdownEvent) {
        // 根据dispatchType筛选消息消费
        if (!consumerFilter(event.dispatchType)) {
            return
        }

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
                    routeKeySuffix = event.routeKeySuffix ?: "",
                    startTime = 0L,
                    stopTime = System.currentTimeMillis(),
                    errorCode = 0,
                    errorMessage = "",
                    errorType = null
                )

                dispatchService.shutdown(event)
            } catch (t: Throwable) {
                logger.warn("Fail to handle the shutdown message - ($event)", t)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to handle the shutdown message - ($event)", t)
        } finally {
            val jobQuotaService = getJobQuotaService()
            jobQuotaService.removeRunningJob(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                executeCount = event.executeCount
            )
            DispatchLogRedisUtils.removeRedisExecuteCount(event.buildId)
        }
    }

    fun getVmType(): JobQuotaVmType?

    fun log(
        buildLogPrinter: BuildLogPrinter,
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?,
        jobId: String?
    ) {
        buildLogPrinter.addLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId = containerHashId,
            executeCount = executeCount ?: 1,
            jobId = jobId,
            stepId = VMUtils.genStartVMTaskId(vmSeqId)
        )
    }

    fun logRed(
        buildLogPrinter: BuildLogPrinter,
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?,
        jobId: String?
    ) {
        buildLogPrinter.addRedLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId = containerHashId,
            executeCount = executeCount ?: 1,
            jobId = jobId,
            stepId = VMUtils.genStartVMTaskId(vmSeqId)
        )
    }

    fun retry(
        sleepTimeInMS: Int = 30000,
        retryTimes: Int = 3,
        pipelineEvent: IEvent? = null,
        errorMessage: String? = ""
    ): Boolean {
        val event = pipelineEvent ?: DispatcherContext.getEvent()
        if (event == null) {
            logger.warn("The event is empty")
            return false
        }
        logger.info("Retry the event($event) in $sleepTimeInMS ms")
        if (event.retryTime > retryTimes) {
            logger.warn("Fail to dispatch the agent start event with $retryTimes times - ($event)")
            onFailure(
                errorType = ErrorType.SYSTEM,
                errorCode = DispatchSdkErrorCode.RETRY_STARTUP_FAIL,
                formatErrorMessage = errorMessage ?: "Fail to start up the job after $retryTimes times",
                message = errorMessage ?: "Fail to start up the job after $retryTimes times"
            )
        }

        // 重试PipelineAgentStartupEvent消息时将上一次的构建配额记录移除
        if (event is PipelineAgentStartupEvent) {
            getJobQuotaService().removeRunningJob(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                executeCount = event.executeCount
            )
        }

        event.retryTime += 1
        event.delayMills = if (sleepTimeInMS <= 5000) {
            // 重试不能低于5秒
            logger.warn("The retry time is less than 5 seconds, use 5 as default")
            5000
        } else {
            sleepTimeInMS
        }
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

    private fun getClient() = SpringContextUtil.getBean(Client::class.java)

    companion object {
        private val logger = LoggerFactory.getLogger(BuildListener::class.java)
    }
}
