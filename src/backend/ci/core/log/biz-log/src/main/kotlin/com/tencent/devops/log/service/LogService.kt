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

package com.tencent.devops.log.service

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.LogBatchEvent
import com.tencent.devops.common.log.pojo.LogEvent
import com.tencent.devops.common.log.pojo.LogStatusEvent
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

interface LogService {

    companion object {
        private val logger = LoggerFactory.getLogger(LogService::class.java)
    }

    fun pipelineFinish(event: PipelineBuildFinishBroadCastEvent)

    fun addLogEvent(event: LogEvent)

    fun addBatchLogEvent(event: LogBatchEvent)

    fun updateLogStatus(event: LogStatusEvent)

    fun queryInitLogs(
        buildId: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs

    fun queryMoreLogsBetweenLines(
        buildId: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs

    fun queryLogsAfterLine(
        buildId: String,
        start: Long,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs

    fun downloadLogs(
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        fileName: String?
    ): Response

    fun getEndLogs(
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int
    ): EndPageQueryLogs

    fun queryInitLogsPage(
        buildId: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): PageQueryLogs

    fun reopenIndex(buildId: String): Boolean
}