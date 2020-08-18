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

    fun addLine(buildId: String, message: String, tag: String, jobId: String? = null, executeCount: Int) {
        logMQEventDispatcher.dispatch(genLogEvent(buildId, message, tag, jobId, LogType.LOG, executeCount))
    }

    fun addLines(buildId: String, logMessages: List<LogMessage>) {
        logMQEventDispatcher.dispatch(LogEvent(buildId, logMessages))
    }

    fun addFoldStartLine(
        buildId: String,
        groupName: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(buildId, "##[group]$groupName", tag, jobId, LogType.LOG, executeCount))
    }

    fun addFoldEndLine(
        buildId: String,
        groupName: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(buildId, "##[endgroup]$groupName", tag, jobId, LogType.LOG, executeCount))
    }

    fun addRangeStartLine(
        buildId: String,
        rangeName: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(buildId, "[START] $rangeName", tag, jobId, LogType.START, executeCount))
    }

    fun addRangeEndLine(
        buildId: String,
        rangeName: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int
    ) {
        logMQEventDispatcher.dispatch(genLogEvent(buildId, "[END] $rangeName", tag, jobId, LogType.END, executeCount))
    }

    fun addYellowLine(
        buildId: String,
        message: String,
        tag: String,
        jobId: String? = null,
        executeCount: Int
    ) = addLine(buildId, Ansi().bold().fgYellow().a(message).reset().toString(), tag, jobId, executeCount)

    fun addRedLine(
        buildId: String,
        message: String,
        tag: String,
        jobId: String?,
        executeCount: Int
    ) = addLine(buildId, Ansi().bold().fgRed().a(message).reset().toString(), tag, jobId, executeCount)

    fun updateLogStatus(
        buildId: String,
        finished: Boolean,
        tag: String,
        jobId: String? = null,
        executeCount: Int?
    ) {
        logMQEventDispatcher.dispatch(LogStatusEvent(buildId, finished, tag, jobId
            ?: "", executeCount))
    }

    fun stopLog(buildId: String, tag: String, jobId: String?, executeCount: Int? = null) {
        updateLogStatus(buildId, true, tag, jobId, executeCount)
    }

    private fun genLogEvent(
        buildId: String,
        message: String,
        tag: String,
        jobId: String? = null,
        logType: LogType,
        executeCount: Int
    ): LogEvent {
        val logs = listOf(LogMessage(message, System.currentTimeMillis(), tag, jobId ?: "", logType, executeCount))
        return LogEvent(buildId, logs)
    }
}
