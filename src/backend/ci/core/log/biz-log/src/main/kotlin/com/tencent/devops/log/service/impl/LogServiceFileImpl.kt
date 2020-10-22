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

package com.tencent.devops.log.service.impl

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.LogBatchEvent
import com.tencent.devops.common.log.pojo.LogEvent
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.LogStatusEvent
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.common.log.utils.LogMQEventDispatcher
import com.tencent.devops.log.jmx.v2.LogBeanV2
import com.tencent.devops.log.lucene.LuceneService
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.util.Constants
import com.tencent.devops.log.util.LuceneIndexUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
class LogServiceFileImpl @Autowired constructor(
    private val luceneService: LuceneService,
    private val indexService: IndexService,
    private val logStatusService: LogStatusService,
    private val logTagService: LogTagService,
    private val defaultKeywords: List<String>,
    private val logBeanV2: LogBeanV2,
    private val logMQEventDispatcher: LogMQEventDispatcher
) : LogService {

    companion object {
        private val logger = LoggerFactory.getLogger(LogServiceFileImpl::class.java)
    }

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, Boolean/*Has create the index*/>()

    override fun pipelineFinish(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            logger.info("[$projectId|$pipelineId|$buildId] build finish")
            indexService.flushLineNum2DB(buildId)
        }
    }

    override fun addLogEvent(event: LogEvent) {
        startLog(event.buildId)
        val logMessage = addLineNo(event.buildId, event.logs)
        if (logMessage.isNotEmpty()) {
            logMQEventDispatcher.dispatch(LogBatchEvent(event.buildId, logMessage))
        }
    }

    override fun addBatchLogEvent(event: LogBatchEvent) {
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            val logMessages = event.logs
            val buf = mutableListOf<LogMessageWithLineNo>()
            logMessages.forEach {
                buf.add(it)
                if (buf.size == 200) {
                    doAddMultiLines(buf, event.buildId)
                    buf.clear()
                }
            }
            if (buf.isNotEmpty()) doAddMultiLines(buf, event.buildId)
            success = true
        } finally {
            val elapse = System.currentTimeMillis() - currentEpoch
            logBeanV2.execute(elapse, success)
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
                finish = finished
            )
        }
    }

    override fun queryInitLogs(
        buildId: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            if (keywordsStr == null || keywordsStr.isBlank()) {
                val result = if (isAnalysis) {
                    doQueryByKeywords(
                        buildId = buildId,
                        index = index,
                        start = 1,
                        keywords = defaultKeywords,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount
                    )
                } else {
                    doQueryInitLogs(
                        buildId = buildId,
                        index = index,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount
                    )
                }
                success = logStatusSuccess(result.status)
                return result
            }

            val keywords =
                Arrays.asList(*(keywordsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                    .stream()
                    .filter { k -> k.isNotBlank() }
                    .collect(Collectors.toList())

            val result = doQueryByKeywords(
                buildId = buildId,
                index = index,
                start = 1,
                keywords = keywords,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            logger.info("query init logs for build($buildId): size-${result.logs.size} size-${result.status}")
            success = true
            return result
        } finally {
            logBeanV2.query(System.currentTimeMillis() - currentEpoch, success)
        }
    }

    override fun queryMoreLogsBetweenLines(
        buildId: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val queryLogs = QueryLogs(buildId, getLogStatus(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ))

            try {
                val logs = luceneService.fetchLogs(
                    buildId = buildId,
                    tag = tag,
                    subTag = subTag,
                    jobId = jobId,
                    executeCount = executeCount,
                    end = end,
                    start = start,
                    size = null
                )
                if (!fromStart) {
                    logs.reverse()
                }
                queryLogs.logs.addAll(logs)
                success = true
            } catch (ex: Exception) {
                logger.error("Query more logs between lines failed, buildId: $buildId", ex)
            }
            return queryLogs
        } finally {
            logBeanV2.query(System.currentTimeMillis() - startEpoch, success)
        }
    }

    override fun queryLogsAfterLine(
        buildId: String,
        start: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            val result = doQueryLargeLogsAfterLine(
                buildId = buildId,
                index = index,
                start = start,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            success = logStatusSuccess(result.status)
            return result
        } finally {
            logBeanV2.query(System.currentTimeMillis() - startEpoch, success)
        }
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
        val fileStream = luceneService.fetchDocumentsStreaming(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val resultName = fileName ?: "$pipelineId-$buildId-log"
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $resultName.log")
            .header("Cache-Control", "no-cache")
            .build()
    }

    override fun getEndLogs(
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int
    ): EndPageQueryLogs {
        val queryLogs = EndPageQueryLogs(buildId)
        try {
            return doGetEndLogs(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )
        } catch (e: Exception) {
            logger.error("Query end logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL
        }
        return queryLogs
    }

    override fun queryInitLogsPage(
        buildId: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): PageQueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            val pageResult: QueryLogs
            val pageLog = if (keywordsStr == null || keywordsStr.isBlank()) {
                if (isAnalysis) {
                    pageResult = doQueryByKeywords(
                        buildId = buildId,
                        index = index,
                        start = 1,
                        keywords = defaultKeywords,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount
                    )
                    val logSize = pageResult.logs.size
                    Page(logSize.toLong(), 1, logSize, 1, pageResult.logs.filter { it.lineNo != -1L })
                } else {
                    pageResult = queryInitLogsPage(
                        buildId = buildId,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount,
                        page = page,
                        pageSize = pageSize
                    )
                    val logSize = luceneService.fetchLogsCount(
                        buildId = buildId,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount
                    )
                    val totalPage = Math.ceil((logSize + 0.0) / pageSize).toInt()
                    Page(
                        count = logSize.toLong(),
                        page = page,
                        pageSize = pageSize,
                        totalPages = totalPage,
                        records = pageResult.logs
                    )
                }
            } else {
                val keywords =
                    Arrays.asList(*(keywordsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                        .stream()
                        .filter { k -> k.isNotBlank() }
                        .collect(Collectors.toList())
                pageResult = doQueryByKeywords(
                    buildId = buildId,
                    index = index,
                    start = 1,
                    keywords = keywords,
                    tag = tag,
                    subTag = subTag,
                    jobId = jobId,
                    executeCount = executeCount
                )
                val logSize = pageResult.logs.size
                Page(
                    count = logSize.toLong(),
                    page = 1,
                    pageSize = logSize,
                    totalPages = 1,
                    records = pageResult.logs.filter { it.lineNo != -1L }
                )
            }
            success = logStatusSuccess(pageResult.status)
            return PageQueryLogs(
                buildId = pageResult.buildId,
                finished = pageResult.finished,
                logs = pageLog,
                timeUsed = pageResult.timeUsed,
                status = pageResult.status
            )
        } finally {
            logBeanV2.query(System.currentTimeMillis() - startEpoch, success)
        }
    }

    override fun reopenIndex(buildId: String): Boolean {
        return true
    }

    private fun logStatusSuccess(logStatus: LogStatus) = (logStatus == LogStatus.EMPTY || logStatus == LogStatus.SUCCEED)

    private fun queryInitLogsPage(
        buildId: String,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): QueryLogs {
        val queryLogs = QueryLogs(buildId, getLogStatus(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        ))

        try {
            val logs = luceneService.fetchAllLogsInPage(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                page = page,
                pageSize = pageSize
            )
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL
            queryLogs.finished = true
        }
        return queryLogs
    }

    private fun doGetEndLogs(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int
    ): EndPageQueryLogs {
        val beginTime = System.currentTimeMillis()
        val count = luceneService.fetchLogsCount(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val logs = luceneService.fetchLogs(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            start = (count - size).toLong()
        )
        return EndPageQueryLogs(
            buildId = buildId,
            startLineNo = logs.lastOrNull()?.lineNo ?: 0,
            endLineNo = logs.firstOrNull()?.lineNo ?: 0,
            logs = logs,
            timeUsed = System.currentTimeMillis() - beginTime
        )
    }

    private fun doQueryByKeywords(
        buildId: String,
        index: String,
        start: Long,
        keywords: List<String>,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val logStatus = getLogStatus(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        val initLogs = QueryLogs(buildId, logStatus)
        try {
            val size = luceneService.fetchLogsCount(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            if (size == 0) {
                return initLogs
            }

            val logs = getLogs(
                buildId = buildId,
                index = index,
                keywords = keywords,
                wholeQuery = false,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            initLogs.logs.addAll(logs)
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            initLogs.status = LogStatus.FAIL
        }
        return initLogs
    }

    private fun doQueryInitLogs(
        buildId: String,
        index: String,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val startTime = System.currentTimeMillis()
        logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryInitLogs")
        val logStatus = if (tag == null && jobId != null) getLogStatus(
            buildId = buildId,
            tag = jobId,
            subTag = null,
            jobId = null,
            executeCount = executeCount
        ) else getLogStatus(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )

        val subTags = if (tag.isNullOrBlank()) null else logTagService.getSubTags(buildId, tag!!)
        val queryLogs = QueryLogs(buildId = buildId, finished = logStatus, subTags = subTags)

        try {
            val size = luceneService.fetchLogsCount(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            val logs = luceneService.fetchInitLogs(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
            queryLogs.hasMore = size > logs.size
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL
            queryLogs.finished = true
            queryLogs.hasMore = false
        }
        return queryLogs
    }

    private fun doQueryLargeLogsAfterLine(
        buildId: String,
        index: String,
        start: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryLargeInitLogs")
        val logStatus = if (tag == null && jobId != null) {
            getLogStatus(
                buildId = buildId,
                tag = jobId,
                subTag = null,
                jobId = null,
                executeCount = executeCount
            )
        } else {
            getLogStatus(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        }

        val subTags = if (tag.isNullOrBlank()) null else logTagService.getSubTags(buildId, tag!!)
        val moreLogs = QueryLogs(buildId = buildId, finished = logStatus, subTags = subTags)

        try {
            val startTime = System.currentTimeMillis()
            val logs = luceneService.fetchLogs(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                start = start,
                size = Constants.MAX_LINES * Constants.SCROLL_MAX_TIMES
            )

            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            moreLogs.logs.addAll(logs)
            moreLogs.hasMore = moreLogs.logs.size >= Constants.MAX_LINES * Constants.SCROLL_MAX_TIMES
        } catch (e: Exception) {
            logger.error("Query after logs failed because of ${e.javaClass}. buildId: $buildId", e)
            moreLogs.status = LogStatus.FAIL
            moreLogs.finished = true
            moreLogs.hasMore = false
        }
        return moreLogs
    }

    private fun getLogStatus(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Boolean {
        return logStatusService.isFinish(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    private fun getLogs(
        buildId: String,
        index: String,
        keywords: List<String>,
        wholeQuery: Boolean,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): List<LogLine> {
        logger.warn("[$buildId|$index|$tag|$subTag|$jobId|$executeCount] luence cannot index with keywords params: " +
            "index: $index, keywords: $keywords, wholeQuery: $wholeQuery, tag: $tag, jobId: $jobId, executeCount: $executeCount")

        val startTime = System.currentTimeMillis()
        val logs = luceneService.fetchLogs(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        logger.info("getLogs time cost: ${System.currentTimeMillis() - startTime}")

        return logs
    }

    private fun doAddMultiLines(logMessages: List<LogMessageWithLineNo>, buildId: String): Int {
        val startTime = System.currentTimeMillis()
        val logDocuments = logMessages.map {
            LuceneIndexUtils.getDocumentObject(buildId, it)
        }
        val lines = luceneService.indexBatchLog(buildId, logDocuments)
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

        // TODO 限制每个构建的日志总量
//        if (lineNum >= Constants.MAX_LINES) {
//            logger.warn("Number of build's log lines is limited, buildId: $buildId")
//            return emptyList()
//        }

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

    private fun startLog(buildId: String, force: Boolean = false): Boolean {
        val index = indexService.getIndexName(buildId)
        indexCache.put(index, true)
        return true
    }
}