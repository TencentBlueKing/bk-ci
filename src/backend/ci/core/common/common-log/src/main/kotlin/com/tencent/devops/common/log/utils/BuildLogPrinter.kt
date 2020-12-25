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

package com.tencent.devops.common.log.utils

import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.LogEvent
import com.tencent.devops.common.log.pojo.LogStatusEvent
import com.tencent.devops.common.log.pojo.enums.LogType

class BuildLogPrinter(
    private val logMQEventDispatcher: LogMQEventDispatcher
) {

    fun addLine(
        buildId: String,
        message: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int,
        subTag: String? = null
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(
            buildId = buildId,
            message = message,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        ))
    }

    fun addLines(buildId: String, logMessages: List<LogMessage>) {
        logMQEventDispatcher.dispatch(LogEvent(buildId, logMessages))
    }

    fun addFoldStartLine(
        buildId: String,
        groupName: String,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(
            buildId = buildId,
            message = "##[group]$groupName",
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        ))
    }

    fun addFoldEndLine(
        buildId: String,
        groupName: String,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(
            buildId = buildId,
            message = "##[endgroup]$groupName",
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        ))
    }

    fun addRangeStartLine(
        buildId: String,
        rangeName: String,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(
            buildId = buildId,
            message = "[START] $rangeName",
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.START,
            executeCount = executeCount
        ))
    }

    fun addRangeEndLine(
        buildId: String,
        rangeName: String,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(
            buildId = buildId,
            message = "[END] $rangeName",
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.END,
            executeCount = executeCount
        ))
    }

    fun addYellowLine(
        buildId: String,
        message: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int,
        subTag: String? = null
    ) = addLine(
        buildId = buildId,
        message = Ansi().bold().fgYellow().a(message).reset().toString(),
        tag = tag,
        subTag = subTag,
        jobId = jobId,
        executeCount = executeCount
    )

    fun addRedLine(
        buildId: String,
        message: String,
        tag: String,
        jobId: String?,
        executeCount: Int,
        subTag: String? = null
    ) = addLine(
        buildId = buildId,
        message = Ansi().bold().fgRed().a(message).reset().toString(),
        tag = tag,
        subTag = subTag,
        jobId = jobId,
        executeCount = executeCount
    )

    fun updateLogStatus(
        buildId: String,
        finished: Boolean,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ) {
        logMQEventDispatcher.dispatch(LogStatusEvent(
            buildId = buildId,
            finished = finished,
            tag = tag,
            subTag = subTag,
            jobId = jobId ?: "",
            executeCount = executeCount
        ))
    }

    fun stopLog(
        buildId: String,
        tag: String,
        jobId: String?,
        executeCount: Int? = null,
        subTag: String? = null
    ) {
        updateLogStatus(
            buildId = buildId,
            finished = true,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    private fun genLogEvent(
        buildId: String,
        message: String,
        tag: String,
        subTag: String? = null,
        jobId: String? = null,
        logType: LogType,
        executeCount: Int
    ): LogEvent {
        val logs = listOf(LogMessage(
            message = message,
            timestamp = System.currentTimeMillis(),
            tag = tag,
            subTag = subTag,
            jobId = jobId ?: "",
            logType = logType,
            executeCount = executeCount
        ))
        return LogEvent(buildId, logs)
    }
}
