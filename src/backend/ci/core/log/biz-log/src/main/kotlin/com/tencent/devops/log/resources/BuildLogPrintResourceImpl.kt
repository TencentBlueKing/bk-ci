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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.print.BuildLogPrintResource
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.meta.Ansi
import com.tencent.devops.log.service.BuildLogPrintService
import com.tencent.devops.log.service.BuildLogQueryService
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogStatusService
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

/**
 *
 * Powered By Tencent
 */
@RestResource
class BuildLogPrintResourceImpl @Autowired constructor(
    private val buildLogPrintService: BuildLogPrintService,
    private val logStatusService: LogStatusService,
    private val indexService: IndexService,
    private val meterRegistry: MeterRegistry,
    private val buildLogQueryService: BuildLogQueryService
) : BuildLogPrintResource {

    @Value("\${spring.application.name:#{null}}")
    private val applicationName: String? = null

    override fun addLogLine(buildId: String, logMessage: LogMessage): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        buildLogPrintService.dispatchEvent(LogOriginEvent(buildId, listOf(logMessage)))
        return Result(true)
    }

    override fun addRedLogLine(buildId: String, logMessage: LogMessage): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        buildLogPrintService.dispatchEvent(
            LogOriginEvent(
                buildId = buildId,
                logs = listOf(
                    logMessage.copy(
                        message = Ansi().bold().fgRed().a(logMessage.message).reset().toString()
                    )
                )
            )
        )
        return Result(true)
    }

    override fun addYellowLogLine(buildId: String, logMessage: LogMessage): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        buildLogPrintService.dispatchEvent(
            LogOriginEvent(
                buildId = buildId,
                logs = listOf(
                    logMessage.copy(
                        message = Ansi().bold().fgYellow().a(logMessage.message).reset().toString()
                    )
                )
            )
        )
        return Result(true)
    }

    @Timed
    override fun addLogMultiLine(buildId: String, logMessages: List<LogMessage>): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        buildLogPrintService.dispatchEvent(LogOriginEvent(buildId, logMessages))
        recordMultiLogCount(logMessages.size)
        return Result(true)
    }

    override fun addLogStatus(
        buildId: String,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        logMode: String?,
        jobId: String?,
        stepId: String?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        // #7168 通过一次获取创建记录以及缓存
        val index = indexService.getIndexName(buildId)
        logger.info("Start to print log to index[$index]")
        buildLogPrintService.dispatchEvent(
            LogStatusEvent(
                buildId = buildId,
                finished = false,
                tag = tag,
                subTag = subTag,
                jobId = containerHashId,
                executeCount = executeCount,
                logStorageMode = LogStorageMode.parse(logMode),
                userJobId = jobId,
                stepId = stepId
            )
        )
        return Result(true)
    }

    override fun updateLogStatus(
        buildId: String,
        finished: Boolean,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        logMode: String?,
        jobId: String?,
        stepId: String?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        buildLogPrintService.dispatchEvent(
            LogStatusEvent(
                buildId = buildId,
                finished = finished,
                tag = tag,
                subTag = subTag,
                jobId = containerHashId,
                executeCount = executeCount,
                logStorageMode = LogStorageMode.parse(logMode),
                userJobId = jobId,
                stepId = stepId
            )
        )
        return Result(false)
    }

    override fun updateLogStorageMode(
        buildId: String,
        executeCount: Int,
        propertyList: List<TaskBuildLogProperty>
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            logger.warn("Invalid build ID[$buildId]")
            return Result(false)
        }
        logStatusService.updateStorageMode(
            buildId = buildId,
            executeCount = executeCount,
            propertyList = propertyList
        )
        return Result(true)
    }

    override fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val initLogs = buildLogQueryService.getInitLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            containerHashId = jobId,
            executeCount = executeCount,
            jobId = null,
            stepId = null,
            reverse = false
        )
        recordMultiLogCount(initLogs.data?.logs?.size ?: 0)
        return initLogs
    }

    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val afterLogs = buildLogQueryService.getAfterLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            start = start,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            containerHashId = jobId,
            executeCount = executeCount,
            jobId = null,
            stepId = null
        )
        recordMultiLogCount(afterLogs.data?.logs?.size ?: 0)

        return afterLogs
    }

    /**
     * 记录日志列表函数
     */
    private fun recordMultiLogCount(count: Number) {
        Counter
            .builder("multi_log_count")
            .tag("application", applicationName ?: "")
            .register(meterRegistry)
            .increment(count.toDouble())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLogPrintResourceImpl::class.java)
    }
}
