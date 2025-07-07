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

package com.tencent.devops.common.log.utils

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.log.api.print.ServiceLogPrintResource
import com.tencent.devops.log.meta.Ansi
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory

@Suppress("LongParameterList", "TooManyFunctions")
class BuildLogPrinter(
    private val client: Client,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {

    fun addLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogLine(
                    buildId = buildId,
                    logMessage = genLogMessage(
                        message = message,
                        tag = tag,
                        subTag = subTag,
                        containerHashId = containerHashId,
                        logType = LogType.LOG,
                        executeCount = executeCount,
                        jobId = jobId,
                        stepId = stepId
                    )
                )
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("[LOG]|LOG_SERVER_ERROR|causingCircuitBreakerName=${e.causingCircuitBreakerName}|$buildId|")
        } catch (ignore: Exception) {
            logger.error("[$buildId]|addLine error|message=$message", ignore)
        }
    }

    fun addLines(buildId: String, logMessages: List<LogMessage>) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogMultiLine(
                    buildId = buildId,
                    logMessages = logMessages
                )
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("[LOG]|LOG_SERVER_ERROR|causingCircuitBreakerName=${e.causingCircuitBreakerName}|$buildId|")
        } catch (ignore: Exception) {
            logger.error("[$buildId]|addLines error|logMessages=$logMessages", ignore)
        }
    }

    fun addFoldStartLine(
        buildId: String,
        groupName: String,
        tag: String,
        subTag: String? = null,
        containerHashId: String? = null,
        executeCount: Int,
        jobId: String?,
        stepId: String?
    ) = addLine(
        buildId = buildId,
        message = "##[group]$groupName",
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId,
        executeCount = executeCount,
        jobId = jobId,
        stepId = stepId
    )

    fun addFoldEndLine(
        buildId: String,
        groupName: String,
        tag: String,
        subTag: String? = null,
        containerHashId: String? = null,
        executeCount: Int,
        jobId: String?,
        stepId: String?
    ) = addLine(
        buildId = buildId,
        message = "##[endgroup]$groupName",
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId,
        executeCount = executeCount,
        jobId = jobId,
        stepId = stepId
    )

    fun addAIErrorLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) {
        addErrorLine(
            buildId = buildId,
            message = "$LOG_AI_FLAG$message",
            tag = tag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            subTag = subTag,
            jobId = jobId,
            stepId = stepId
        )
    }

    fun addErrorLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogLine(
                    buildId = buildId,
                    logMessage = genLogMessage(
                        message = "$LOG_ERROR_FLAG${message.replace("\n", "\n$LOG_ERROR_FLAG")}",
                        tag = tag,
                        subTag = subTag,
                        containerHashId = containerHashId,
                        logType = LogType.ERROR,
                        executeCount = executeCount,
                        jobId = jobId,
                        stepId = stepId
                    )
                )
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("[LOG]|LOG_SERVER_ERROR|causingCircuitBreakerName=${e.causingCircuitBreakerName}|$buildId|")
        } catch (ignore: Exception) {
            logger.error("[$buildId]|addErrorLine error|message=$message", ignore)
        }
    }

    fun addDebugLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogLine(
                    buildId = buildId,
                    logMessage = genLogMessage(
                        message = "$LOG_DEBUG_FLAG${message.replace("\n", "\n$LOG_DEBUG_FLAG")}",
                        tag = tag,
                        subTag = subTag,
                        containerHashId = containerHashId,
                        logType = LogType.DEBUG,
                        executeCount = executeCount,
                        jobId = jobId,
                        stepId = stepId
                    )
                )
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("[LOG]|LOG_SERVER_ERROR|causingCircuitBreakerName=${e.causingCircuitBreakerName}|$buildId|")
        } catch (ignore: Exception) {
            logger.error("[$buildId]|addDebugLine error|message=$message", ignore)
        }
    }

    fun addWarnLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogLine(
                    buildId = buildId,
                    logMessage = genLogMessage(
                        message = "$LOG_WARN_FLAG${message.replace("\n", "\n$LOG_WARN_FLAG")}",
                        tag = tag,
                        subTag = subTag,
                        containerHashId = containerHashId,
                        logType = LogType.WARN,
                        executeCount = executeCount,
                        jobId = jobId,
                        stepId = stepId
                    )
                )
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("[LOG]|LOG_SERVER_ERROR|causingCircuitBreakerName=${e.causingCircuitBreakerName}|$buildId|")
        } catch (ignore: Exception) {
            logger.error("[$buildId]|addWarnLine error|message=$message", ignore)
        }
    }

    fun addYellowLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String? = null,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) = addLine(
        buildId = buildId,
        message = Ansi().bold().fgYellow().a(message).reset().toString(),
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId,
        executeCount = executeCount,
        jobId = jobId,
        stepId = stepId
    )

    @Suppress("UNUSED")
    fun addGreenLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String?,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) = addLine(
        buildId = buildId,
        message = Ansi().bold().fgGreen().a(message).reset().toString(),
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId,
        executeCount = executeCount,
        jobId = jobId,
        stepId = stepId
    )

    fun addRedLine(
        buildId: String,
        message: String,
        tag: String,
        containerHashId: String?,
        executeCount: Int,
        subTag: String? = null,
        jobId: String?,
        stepId: String?
    ) = addLine(
        buildId = buildId,
        message = Ansi().bold().fgRed().a(message).reset().toString(),
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId,
        executeCount = executeCount,
        jobId = jobId,
        stepId = stepId
    )

    fun stopLog(
        buildId: String,
        tag: String? = null,
        containerHashId: String? = null,
        executeCount: Int? = null,
        subTag: String? = null,
        jobId: String? = null,
        stepId: String? = null
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().updateLogStatus(
                    buildId = buildId,
                    finished = true,
                    tag = tag,
                    subTag = subTag,
                    containerHashId = containerHashId,
                    executeCount = executeCount,
                    jobId = jobId,
                    stepId = stepId
                )
            }
        } catch (ignore: Exception) {
            logger.warn("[$buildId]|stopLog fail", ignore)
        }
    }

    fun startLog(
        buildId: String,
        tag: String?,
        containerHashId: String?,
        executeCount: Int? = null,
        subTag: String? = null,
        jobId: String? = null,
        stepId: String? = null
    ) {
        try {
            doWithCircuitBreaker {
                genLogPrintPrintResource().addLogStatus(
                    buildId = buildId,
                    tag = tag,
                    subTag = subTag,
                    containerHashId = containerHashId,
                    executeCount = executeCount,
                    jobId = jobId,
                    stepId = stepId
                )
            }
        } catch (ignore: Exception) {
            logger.warn("[$buildId]|stopLog fail", ignore)
        }
    }

    private fun genLogMessage(
        message: String,
        tag: String,
        subTag: String? = null,
        containerHashId: String? = null,
        logType: LogType,
        executeCount: Int,
        jobId: String?,
        stepId: String?
    ) = LogMessage(
        message = message,
        timestamp = System.currentTimeMillis(),
        tag = tag,
        subTag = subTag,
        containerHashId = containerHashId ?: "",
        logType = logType,
        executeCount = executeCount,
        jobId = jobId ?: "",
        stepId = stepId ?: ""
    )

    private fun genLogPrintPrintResource(): ServiceLogPrintResource {
        return client.get(ServiceLogPrintResource::class)
    }

    private fun <T> doWithCircuitBreaker(
        action: () -> T
    ): T {
        return circuitBreakerRegistry.let {
            val breaker = it.circuitBreaker(this.javaClass.name)
            breaker.executeCallable {
                action()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLogPrinter::class.java)

        private const val LOG_DEBUG_FLAG = "##[debug]"

        private const val LOG_ERROR_FLAG = "##[error]"

        private const val LOG_WARN_FLAG = "##[warning]"

        private const val LOG_AI_FLAG = "##[ai]"
    }
}
