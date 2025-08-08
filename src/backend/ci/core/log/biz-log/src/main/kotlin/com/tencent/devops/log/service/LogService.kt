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

package com.tencent.devops.log.service

import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import jakarta.ws.rs.core.Response

@Suppress("LongParameterList", "TooManyFunctions")
interface LogService {

    fun addLogEvent(event: LogOriginEvent)

    fun addBatchLogEvent(event: LogStorageEvent)

    fun updateLogStatus(event: LogStatusEvent)

    fun queryInitLogs(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?,
        reverse: Boolean?
    ): QueryLogs

    fun queryLogsBetweenLines(
        buildId: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?
    ): QueryLogs

    fun queryLogsAfterLine(
        buildId: String,
        start: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?
    ): QueryLogs

    fun queryLogsBeforeLine(
        buildId: String,
        end: Long,
        debug: Boolean,
        logType: LogType?,
        size: Int?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?
    ): QueryLogs

    fun downloadLogs(
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        fileName: String?,
        jobId: String?,
        stepId: String?
    ): Response

    fun getEndLogsPage(
        pipelineId: String,
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        size: Int,
        jobId: String?,
        stepId: String?
    ): EndPageQueryLogs

    fun getBottomLogs(
        pipelineId: String,
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        size: Int?,
        jobId: String?,
        stepId: String?
    ): QueryLogs

    fun queryInitLogsPage(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int,
        jobId: String?,
        stepId: String?
    ): PageQueryLogs

    fun reopenIndex(buildId: String): Boolean
}
