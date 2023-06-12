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

package com.tencent.devops.log.service.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.log.constant.LogMessageCode
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import com.tencent.devops.log.jmx.LogStorageBean
import com.tencent.devops.log.lucene.LuceneClient
import com.tencent.devops.log.service.BuildLogPrintService
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.util.Constants
import com.tencent.devops.log.util.LuceneIndexUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.math.ceil

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions", "ReturnCount")
class LogServiceLuceneImpl constructor(
    private val indexMaxSize: Int,
    private val luceneClient: LuceneClient,
    private val indexService: IndexService,
    private val logStatusService: LogStatusService,
    private val logTagService: LogTagService,
    private val logStorageBean: LogStorageBean,
    private val buildLogPrintService: BuildLogPrintService
) : LogService {

    companion object {
        private val logger = LoggerFactory.getLogger(LogServiceLuceneImpl::class.java)
        private const val INDEX_CACHE_MAX_SIZE = 100000L
        private const val INDEX_CACHE_EXPIRE_MINUTES = 30L
        private const val INDEX_STORAGE_WARN_MILLIS = 1000
    }

    private val indexCache = Caffeine.newBuilder()
        .maximumSize(INDEX_CACHE_MAX_SIZE)
        .expireAfterAccess(INDEX_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String/*BuildId*/, Boolean/*Has created the index*/>()

    override fun addLogEvent(event: LogOriginEvent) {
        val logMessage = addLineNo(event.buildId, event.logs)
        if (logMessage.isNotEmpty()) {
            buildLogPrintService.dispatchEvent(LogStorageEvent(event.buildId, logMessage))
        }
    }

    override fun addBatchLogEvent(event: LogStorageEvent) {
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            prepareIndex(event.buildId)
            val logMessages = event.logs
            val buf = mutableListOf<LogMessageWithLineNo>()
            logMessages.forEach {
                buf.add(it)
                if (buf.size == Constants.BULK_BUFFER_SIZE) {
                    doAddMultiLines(buf, event.buildId)
                    buf.clear()
                }
            }
            if (buf.isNotEmpty()) doAddMultiLines(buf, event.buildId)
            success = true
        } finally {
            val elapse = System.currentTimeMillis() - currentEpoch
            logStorageBean.batchWrite(elapse, success)

            // #4265 当日志消息处理时间过长时打印消息内容
            if (elapse >= INDEX_STORAGE_WARN_MILLIS && event.logs.isNotEmpty()) logger.warn(
                "[${event.buildId}] addBatchLogEvent spent too much time($elapse) with tag=${event.logs.first().tag}"
            )
        }
    }

    override fun updateLogStatus(event: LogStatusEvent) {
        with(event) {
            logger.info("[$buildId|$tag|$subTag|$jobId|$executeCount|$finished] Start to update log status")
            logStatusService.finish(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                logStorageMode = logStorageMode,
                finish = finished
            )
        }
    }

    override fun queryInitLogs(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        return doQueryInitLogs(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun queryLogsBetweenLines(
        buildId: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        if (index.isNullOrBlank()) return queryLogs
        try {
            val logs = luceneClient.fetchLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                before = end,
                start = start,
                size = null
            )
            if (!fromStart) {
                logs.reverse()
            }
            queryLogs.logs.addAll(logs)
        } catch (ignore: Exception) {
            logger.error("Query more logs between lines failed, buildId: $buildId", ignore)
        }
        return queryLogs
    }

    override fun queryLogsAfterLine(
        buildId: String,
        start: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        return doQueryLogsAfterLine(
            buildId = buildId,
            start = start,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun queryLogsBeforeLine(
        buildId: String,
        end: Long,
        debug: Boolean,
        logType: LogType?,
        size: Int?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        return doQueryLogsBeforeLine(
            buildId = buildId,
            before = end,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            size = size ?: Constants.NORMAL_MAX_LINES
        )
    }

    override fun downloadLogs(
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        fileName: String?
    ): Response {
        val fileStream = luceneClient.fetchDocumentsStreaming(
            buildId = buildId,
            debug = false,
            logType = null,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val resultName = fileName ?: "$pipelineId-$buildId-log"
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = \"$resultName.log\"")
            .header("Cache-Control", "no-cache")
            .build()
    }

    override fun getEndLogsPage(
        pipelineId: String,
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int
    ): EndPageQueryLogs {
        val queryLogs = EndPageQueryLogs(buildId)
        try {
            val result = doGetEndLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )
            queryLogs.startLineNo = result.logs.lastOrNull()?.lineNo ?: 0
            queryLogs.endLineNo = result.logs.firstOrNull()?.lineNo ?: 0
            queryLogs.logs = result.logs
        } catch (ignore: Exception) {
            logger.error("Query end logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            queryLogs.status = LogStatus.FAIL.status
        }
        return queryLogs
    }

    override fun getBottomLogs(
        pipelineId: String,
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int?
    ): QueryLogs {
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        if (index.isNullOrBlank()) return queryLogs
        try {
            val result = doGetEndLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size ?: Constants.NORMAL_MAX_LINES
            )
            queryLogs.logs = result.logs
        } catch (ignore: Exception) {
            logger.error("Query bottom logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            queryLogs.status = LogStatus.FAIL.status
        }
        return queryLogs
    }

    override fun queryInitLogsPage(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): PageQueryLogs {
        val pageResult = doQueryInitLogsPage(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            page = page,
            pageSize = pageSize
        )
        val logSize = luceneClient.fetchLogsCount(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val totalPage = ceil((logSize + 0.0) / pageSize).toInt()
        val pageLog = Page(
            count = logSize.toLong(),
            page = page,
            pageSize = pageSize,
            totalPages = totalPage,
            records = pageResult.logs
        )
        return PageQueryLogs(
            buildId = pageResult.buildId,
            finished = pageResult.finished,
            logs = pageLog,
            timeUsed = pageResult.timeUsed,
            status = pageResult.status
        )
    }

    override fun reopenIndex(buildId: String): Boolean {
        return true
    }

    private fun doQueryInitLogsPage(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): QueryLogs {
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        if (index.isNullOrBlank()) return queryLogs
        try {
            val logs = luceneClient.fetchAllLogsInPage(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                page = page,
                pageSize = pageSize
            )
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
        } catch (ignore: Exception) {
            logger.error("Query init logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            queryLogs.status = LogStatus.FAIL.status
            queryLogs.finished = true
        }
        return queryLogs
    }

    private fun doGetEndLogs(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int
    ): QueryLogs {
        logger.info("[$buildId|$tag|$subTag|$jobId|$executeCount] doGetEndLogs")
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        if (index.isNullOrBlank()) return queryLogs
        val logSize = luceneClient.fetchLogsCount(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val logs = luceneClient.fetchLogs(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            start = (logSize - size).toLong()
        )
        queryLogs.logs = logs
        queryLogs.hasMore = logSize > queryLogs.logs.size
        return queryLogs
    }

    private fun doQueryInitLogs(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val startTime = System.currentTimeMillis()
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryInitLogs")
        if (index.isNullOrBlank()) return queryLogs
        try {
            val size = luceneClient.fetchLogsCount(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            val logs = luceneClient.fetchInitLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
            queryLogs.hasMore = size > logs.size
        } catch (ignore: Exception) {
            logger.error("Query init logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            queryLogs.status = LogStatus.FAIL.status
            queryLogs.finished = true
            queryLogs.hasMore = false
        }
        return queryLogs
    }

    private fun doQueryLogsAfterLine(
        buildId: String,
        start: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val (moreLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryLogsAfterLine")
        if (index.isNullOrBlank()) return moreLogs
        try {
            val startTime = System.currentTimeMillis()
            val logs = luceneClient.fetchLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                start = start,
                size = Constants.SCROLL_MAX_LINES * Constants.SCROLL_MAX_TIMES
            )

            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            moreLogs.logs.addAll(logs)
            moreLogs.hasMore = moreLogs.logs.size >= Constants.SCROLL_MAX_LINES * Constants.SCROLL_MAX_TIMES
        } catch (ignore: Exception) {
            logger.warn("Query after logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            moreLogs.status = LogStatus.FAIL.status
            moreLogs.finished = true
            moreLogs.hasMore = false
        }
        return moreLogs
    }

    private fun doQueryLogsBeforeLine(
        buildId: String,
        before: Long,
        size: Int,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val (queryLogs, index) = getQueryLogs(buildId, jobId, tag, subTag, executeCount)
        logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryLogsBeforeLine")
        if (index.isNullOrBlank()) return queryLogs

        try {
            val startTime = System.currentTimeMillis()
            val logSize = luceneClient.fetchLogsCount(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            val logs = luceneClient.fetchLogs(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                before = before,
                size = size
            )

            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            queryLogs.logs.addAll(logs)
            queryLogs.hasMore = queryLogs.logs.size >= logSize
        } catch (ignore: Exception) {
            logger.error("Query before logs failed because of ${ignore.javaClass}. buildId: $buildId", ignore)
            queryLogs.status = LogStatus.FAIL.status
            queryLogs.finished = true
            queryLogs.hasMore = false
        }
        return queryLogs
    }

    private fun getQueryLogs(
        buildId: String,
        jobId: String?,
        tag: String?,
        subTag: String?,
        executeCount: Int?
    ): Pair<QueryLogs, String?> {
        val logStatus = logStatusService.isFinish(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val indexName = indexService.getBuildIndexName(buildId)
        val subTags = tag?.let { logTagService.getSubTags(buildId, it) }
        val (status, msg) = if (indexService.getBuildIndexName(buildId) == null) {
            Pair(
                LogStatus.CLEAN,
                I18nUtil.getCodeLanMessage(LogMessageCode.LOG_INDEX_HAS_BEEN_CLEANED)
            )
        } else {
            Pair(LogStatus.SUCCEED, null)
        }
        return Pair(
            QueryLogs(
                buildId = buildId,
                finished = logStatus,
                status = status.status,
                subTags = subTags,
                message = msg
            ),
            indexName
        )
    }

    private fun doAddMultiLines(logMessages: List<LogMessageWithLineNo>, buildId: String): Int {
        val startTime = System.currentTimeMillis()
        val logDocuments = logMessages.map {
            LuceneIndexUtils.getDocumentObject(buildId, it)
        }
        val lines = luceneClient.indexBatchLog(buildId, logDocuments)
        val endTime = System.currentTimeMillis()
        logger.info("[$buildId] it takes ${(endTime - startTime)} ms to do add $lines lines in lucene file.")
        return lines
    }

    private fun addLineNo(buildId: String, logMessages: List<LogMessage>): List<LogMessageWithLineNo> {
        val lineNum = indexService.getAndAddLineNum(buildId, logMessages.size)
        if (lineNum == null) {
            logger.error("Got null logIndex from indexService, buildId: $buildId")
            return emptyList()
        }

        if (lineNum >= indexMaxSize) {
            logger.warn("Number of build's log lines is limited, buildId: $buildId")
            return emptyList()
        }

        var startLineNum: Long = lineNum
        return logMessages.map {
            val timestamp = if (it.timestamp == 0L) {
                System.currentTimeMillis()
            } else {
                it.timestamp
            }
            if (!it.subTag.isNullOrBlank()) {
                logTagService.saveSubTag(buildId, it.tag, it.subTag!!)
            }
            LogMessageWithLineNo(
                tag = it.tag,
                subTag = it.subTag,
                jobId = it.jobId,
                message = it.message,
                timestamp = timestamp,
                logType = it.logType,
                lineNo = startLineNum++,
                executeCount = it.executeCount
            )
        }
    }

    private fun prepareIndex(buildId: String): Boolean {
        val index = indexService.getIndexName(buildId)
        indexCache.put(index, true)
        return true
    }
}
