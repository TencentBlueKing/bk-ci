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
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.LogBatchEvent
import com.tencent.devops.common.log.pojo.LogEvent
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.LogStatusEvent
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.jmx.CreateIndexBean
import com.tencent.devops.log.jmx.LogStorageBean
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.util.Constants
import com.tencent.devops.log.util.ESIndexUtils
import com.tencent.devops.log.service.BuildLogPrintService
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import kotlin.math.ceil

@Suppress("ALL")
class LogServiceESImpl constructor(
    private val logClient: LogClient,
    private val indexService: IndexService,
    private val logStatusService: LogStatusService,
    private val logTagService: LogTagService,
    private val createIndexBean: CreateIndexBean,
    private val logStorageBean: LogStorageBean,
    private val redisOperation: RedisOperation,
    private val buildLogPrintService: BuildLogPrintService
) : LogService {

    companion object {
        private val logger = LoggerFactory.getLogger(LogServiceESImpl::class.java)
    }

    private val indexCache = Caffeine.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, Boolean/*Has create the index*/>()

    override fun pipelineFinish(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            indexService.flushLineNum2DB(buildId)
        }
    }

    override fun addLogEvent(event: LogEvent) {
        val logMessage = addLineNo(event.buildId, event.logs)
        if (logMessage.isNotEmpty()) {
            buildLogPrintService.dispatchEvent(LogBatchEvent(event.buildId, logMessage))
        }
    }

    override fun addBatchLogEvent(event: LogBatchEvent) {
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            prepareIndex(event.buildId)
            val logMessages = event.logs
            val buf = mutableListOf<LogMessageWithLineNo>()
            logMessages.forEach {
                buf.add(it)
                if (buf.size == Constants.BULK_BUFFER_SIZE) {
                    if (doAddMultiLines(buf, event.buildId) == 0) {
                        throw Exception(
                            "None of lines is inserted successfully to ES [${event.buildId}|${event.retryTime}]"
                        )
                    } else {
                        buf.clear()
                    }
                }
            }
            if (buf.isNotEmpty()) {
                if (doAddMultiLines(buf, event.buildId) == 0) {
                    throw Exception(
                        "None of lines is inserted successfully to ES [${event.buildId}|${event.retryTime}]"
                    )
                }
            }
            success = true
        } finally {
            val elapse = System.currentTimeMillis() - currentEpoch
            logStorageBean.batchWrite(elapse, success)

            // #4265 当日志消息处理时间过长时打印消息内容
            if (elapse >= 1000 && event.logs.isNotEmpty()) logger.warn(
                "[${event.buildId}] addBatchLogEvent spent too much time($elapse) with tag=${event.logs.first().tag}"
            )
        }
    }

    override fun updateLogStatus(event: LogStatusEvent) {
        with(event) {
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
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            val result = doQueryInitLogs(
                buildId = buildId,
                index = index,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            success = logStatusSuccess(result.status)
            return result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - currentEpoch, success)
        }
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
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)

            val queryLogs = QueryLogs(
                buildId, getLogStatus(
                    buildId = buildId,
                    tag = tag,
                    subTag = subTag,
                    jobId = jobId,
                    executeCount = executeCount
                )
            )

            try {
                val query = getQuery(
                    buildId = buildId,
                    debug = debug,
                    logType = logType,
                    tag = tag,
                    subTag = subTag,
                    jobId = jobId,
                    executeCount = executeCount
                ).must(QueryBuilders.rangeQuery("lineNo").gte(start).lte(end))

                val sortOrder = if (fromStart) SortOrder.ASC else SortOrder.DESC
                val searchRequest = SearchRequest(index)
                    .source(
                        SearchSourceBuilder()
                            .query(query)
                            .highlighter(
                                HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                                    .field("message").fragmentSize(100000)
                            )
                            .docValueField("lineNo")
                            .docValueField("timestamp")
                            .size(num)
                            .sort("timestamp", sortOrder)
                            .sort("lineNo", sortOrder)
                            .timeout(TimeValue.timeValueSeconds(60))
                    )

                val searchResponse = try {
                    logClient.hashClient(buildId).restClient.search(searchRequest, RequestOptions.DEFAULT)
                } catch (e: IOException) {
                    logClient.hashClient(buildId).restClient.search(searchRequest, genLargeSearchOptions())
                }

                searchResponse.hits.forEach { searchHitFields ->
                    val sourceMap = searchHitFields.sourceAsMap
                    val logLine = LogLine(
                        lineNo = sourceMap["lineNo"].toString().toLong(),
                        timestamp = sourceMap["timestamp"].toString().toLong(),
                        message = sourceMap["message"].toString(),
                        priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                        tag = sourceMap["tag"].toString(),
                        subTag = sourceMap["subTag"].toString(),
                        jobId = sourceMap["jobId"].toString()
                    )
                    queryLogs.logs.add(logLine)
                }
                if (!fromStart) {
                    queryLogs.logs.reverse()
                }
                if (queryLogs.logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
                success = true
            } catch (e: ElasticsearchStatusException) {
                e.status()
                val exString = e.toString()
                if (exString.contains("index_closed_exception")) {
                    logger.error("[$buildId] Can't search because of index_closed_exception", e)
                    queryLogs.status = LogStatus.CLOSED.status
                }
            } catch (e: Exception) {
                logger.error("Query more logs between lines failed because of ${e.javaClass}. buildId: $buildId", e)
            }
            return queryLogs
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
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
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            val result = doQueryLogsAfterLine(
                buildId = buildId,
                index = index,
                start = start,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            success = logStatusSuccess(result.status)
            return result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
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
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.getIndexName(buildId)
            val result = doQueryLogsBeforeLine(
                buildId = buildId,
                index = index,
                end = end,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size ?: Constants.NORMAL_MAX_LINES
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            return result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
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

        val index = indexService.getIndexName(buildId)
        val query = getQuery(
            buildId = buildId,
            debug = false,
            logType = null,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )

        val scrollClient = logClient.hashClient(buildId)
        val searchRequest = SearchRequest(index)
            .source(
                SearchSourceBuilder()
                    .query(query)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(Constants.SCROLL_MAX_LINES)
                    .sort("timestamp", SortOrder.ASC)
                    .sort("lineNo", SortOrder.ASC)
            ).scroll(TimeValue(1000 * 64))

        var scrollResp = try {
            scrollClient.restClient.search(searchRequest, RequestOptions.DEFAULT)
        } catch (e: IOException) {
            scrollClient.restClient.search(searchRequest, genLargeSearchOptions())
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        // 一边读一边流式下载
        val fileStream = StreamingOutput { output ->
            do {
                val sb = StringBuilder()
                scrollResp.hits.hits.forEach { searchHit ->
                    val sourceMap = searchHit.sourceAsMap

                    val logLine = LogLine(
                        sourceMap["lineNo"].toString().toLong(),
                        sourceMap["timestamp"].toString().toLong(),
                        sourceMap["message"].toString().removePrefix("\u001b[31m").removePrefix("\u001b[1m").replace(
                            "\u001B[m",
                            ""
                        ).removeSuffix("\u001b[m"),
                        Constants.DEFAULT_PRIORITY_NOT_DELETED
                    )
                    val dateTime = sdf.format(Date(logLine.timestamp))
                    val str = "$dateTime : ${logLine.message}" + System.lineSeparator()
                    sb.append(str)
                }
                output.write(sb.toString().toByteArray())
                output.flush()
                scrollResp = scrollClient.restClient.scroll(
                    SearchScrollRequest(scrollResp.scrollId).scroll(TimeValue(1000 * 64)),
                    RequestOptions.DEFAULT
                )
            } while (scrollResp.hits.hits.isNotEmpty())
        }

        val resultName = fileName ?: "$pipelineId-$buildId-log"
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $resultName.log")
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
        val startEpoch = System.currentTimeMillis()
        val queryLogs = EndPageQueryLogs(buildId)
        var success = false
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
            success = logStatusSuccess(result.status)
            queryLogs.startLineNo = result.logs.lastOrNull()?.lineNo ?: 0
            queryLogs.endLineNo = result.logs.firstOrNull()?.lineNo ?: 0
            queryLogs.logs = result.logs
            queryLogs.timeUsed = System.currentTimeMillis() - startEpoch
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
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
        val startEpoch = System.currentTimeMillis()
        var success = false
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
            success = logStatusSuccess(result.status)
            result.timeUsed = System.currentTimeMillis() - startEpoch
            return result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
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
        val startEpoch = System.currentTimeMillis()
        var success = false
        var queryLogs = QueryLogs(
            buildId, getLogStatus(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
        var logSize = 0L
        try {
            val index = indexService.getIndexName(buildId)
            queryLogs = doQueryInitLogsPage(
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
            logSize = getLogSize(
                index = index,
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            success = logStatusSuccess(queryLogs.status)
            if (queryLogs.logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
        } catch (e: ElasticsearchStatusException) {
            val exString = e.toString()
            if (exString.contains("index_closed_exception")) {
                logger.error("[$buildId] Can't search because of index_closed_exception", e)
                queryLogs.status = LogStatus.CLOSED.status
            }
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL.status
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }

        return PageQueryLogs(
            buildId = queryLogs.buildId,
            finished = queryLogs.finished,
            logs = Page(
                count = logSize,
                page = page,
                pageSize = pageSize,
                totalPages = ceil((logSize + 0.0) / pageSize).toInt(),
                records = queryLogs.logs
            ),
            timeUsed = queryLogs.timeUsed,
            status = queryLogs.status
        )
    }

    override fun reopenIndex(buildId: String): Boolean {
        logger.info("Reopen Index - $buildId")
        val index = indexService.getIndexName(buildId)
        return openIndex(buildId, index)
    }

    private fun logStatusSuccess(logStatus: Int) =
        (LogStatus.parse(logStatus) == LogStatus.EMPTY || LogStatus.parse(logStatus) == LogStatus.SUCCEED)

    private fun openIndex(buildId: String, index: String): Boolean {
        logger.info("[$buildId|$index] Start to open the index")
        return logClient.hashClient(buildId).restClient.indices()
            .open(OpenIndexRequest(index), RequestOptions.DEFAULT)
            .isAcknowledged
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
        val queryLogs = QueryLogs(
            buildId, getLogStatus(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
        val index = indexService.getIndexName(buildId)

        val boolQuery = QueryBuilders.boolQuery()
        if (page != -1 && pageSize != -1) {
            val endLineNo = pageSize * page
            val beginLineNo = endLineNo - pageSize + 1
            boolQuery.must(QueryBuilders.rangeQuery("lineNo").gte(beginLineNo).lte(endLineNo))
        }

        val query = getQuery(
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        ).must(boolQuery)

        val scrollClient = logClient.hashClient(buildId)
        val searchRequest = SearchRequest(index)
            .source(
                SearchSourceBuilder()
                    .query(query)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(pageSize)
                    .sort("timestamp", SortOrder.ASC)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60))
            )
            .scroll(TimeValue(1000 * 64))

        var searchResponse = try {
            logClient.hashClient(buildId).restClient.search(searchRequest, RequestOptions.DEFAULT)
        } catch (e: IOException) {
            logClient.hashClient(buildId).restClient.search(searchRequest, genLargeSearchOptions())
        }
        do {
            searchResponse.hits.hits.forEach { searchHit ->
                val sourceMap = searchHit.sourceAsMap
                val type = sourceMap["logType"].toString()
                val logLine = LogLine(
                    sourceMap["lineNo"].toString().toLong(),
                    sourceMap["timestamp"].toString().toLong(),
                    if (type == LogType.LOG.name) sourceMap["message"].toString() else "",
                    Constants.DEFAULT_PRIORITY_NOT_DELETED
                )
                queryLogs.logs.add(logLine)
            }
            searchResponse = scrollClient.restClient.scroll(
                SearchScrollRequest(searchResponse.scrollId).scroll(TimeValue(1000 * 64)),
                RequestOptions.DEFAULT
            )
        } while (searchResponse.hits.hits.isNotEmpty())

        if (queryLogs.logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
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
        val queryLogs = QueryLogs(buildId, logStatus)
        try {
            val index = indexService.getIndexName(buildId)
            val logSize = getLogSize(
                index = index,
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            if (logSize == 0L) return queryLogs
            val start = if (logSize > size.toLong()) logSize - size.toLong() else 0L
            val query = getQuery(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ).must(QueryBuilders.rangeQuery("lineNo").gte(start))
            val searchRequest = SearchRequest(index)
                .source(
                    SearchSourceBuilder()
                        .query(query)
                        .docValueField("lineNo")
                        .docValueField("timestamp")
                        .size(size)
                        .sort("timestamp", SortOrder.ASC)
                        .sort("lineNo", SortOrder.ASC)
                        .timeout(TimeValue.timeValueSeconds(60))
                )
                .scroll(TimeValue(1000 * 32))

            val searchResponse = try {
                logClient.hashClient(buildId).restClient.search(searchRequest, RequestOptions.DEFAULT)
            } catch (ex: IOException) {
                logClient.hashClient(buildId).restClient.search(searchRequest, genLargeSearchOptions())
            }

            val logs = mutableListOf<LogLine>()
            searchResponse.hits.hits.forEach { searchHit ->
                val sourceMap = searchHit.sourceAsMap
                val logLine = LogLine(
                    lineNo = sourceMap["lineNo"].toString().toLong(),
                    timestamp = sourceMap["timestamp"].toString().toLong(),
                    message = sourceMap["message"].toString(),
                    priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    tag = sourceMap["tag"].toString(),
                    subTag = sourceMap["subTag"].toString(),
                    jobId = sourceMap["jobId"].toString(),
                    executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                logs.add(logLine)
            }
            queryLogs.logs = logs
            queryLogs.hasMore = logSize > queryLogs.logs.size
            return queryLogs
        } catch (e: ElasticsearchStatusException) {
            val exString = e.toString()
            if (exString.contains("index_closed_exception")) {
                logger.error("[$buildId] Can't search because of index_closed_exception", e)
                queryLogs.status = LogStatus.CLOSED.status
            }
        } catch (e: Exception) {
            logger.error("Query end logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL.status
        }
        return queryLogs
    }

    private fun doQueryInitLogs(
        buildId: String,
        index: String,
        debug: Boolean,
        logType: LogType?,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs {
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
            val logSize = getLogSize(
                index = index,
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            if (logSize == 0L) return queryLogs

            val startTime = System.currentTimeMillis()
            val boolQueryBuilder = getQuery(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryInitLogs get the query builder: $boolQueryBuilder")

            val searchRequest = SearchRequest(index)
                .source(
                    SearchSourceBuilder()
                        .query(boolQueryBuilder)
                        .docValueField("lineNo")
                        .docValueField("timestamp")
                        .size(Constants.NORMAL_MAX_LINES)
                        .sort("timestamp", SortOrder.ASC)
                        .sort("lineNo", SortOrder.ASC)
                        .timeout(TimeValue.timeValueSeconds(60))
                )

            val searchResponse = try {
                logClient.hashClient(buildId).restClient.search(searchRequest, RequestOptions.DEFAULT)
            } catch (e: IOException) {
                logClient.hashClient(buildId).restClient.search(searchRequest, genLargeSearchOptions())
            }

            searchResponse.hits.forEach { searchHitFields ->
                val sourceMap = searchHitFields.sourceAsMap
                val ln = sourceMap["lineNo"].toString().toLong()
                val t = sourceMap["tag"]?.toString() ?: ""
                val logLine = LogLine(
                    lineNo = ln,
                    timestamp = sourceMap["timestamp"].toString().toLong(),
                    message = sourceMap["message"].toString(),
                    priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    tag = t,
                    subTag = sourceMap["subTag"]?.toString() ?: "",
                    jobId = sourceMap["jobId"]?.toString() ?: "",
                    executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                queryLogs.logs.add(logLine)
            }
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            if (queryLogs.logs.isEmpty()) queryLogs.status = LogStatus.EMPTY.status
            queryLogs.hasMore = logSize > queryLogs.logs.size
        } catch (e: ElasticsearchStatusException) {
            val exString = e.toString()
            if (exString.contains("index_closed_exception")) {
                logger.error("[$buildId] Can't search because of index_closed_exception", e)
                queryLogs.status = LogStatus.CLOSED.status
            }
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL.status
        }
        return queryLogs
    }

    private fun doQueryLogsAfterLine(
        buildId: String,
        index: String,
        start: Long,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
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
        val queryLogs = QueryLogs(buildId = buildId, finished = logStatus, subTags = subTags)

        try {
            val startTime = System.currentTimeMillis()
            val logSize = getLogSize(
                index = index,
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                start = start
            )
            if (logSize == 0L) return queryLogs
            val boolQueryBuilder = getQuery(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ).must(QueryBuilders.rangeQuery("lineNo").gte(start))

            logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryLogsAfterLine get the query builder: $boolQueryBuilder")
            val searchRequest = SearchRequest(index)
                .source(
                    SearchSourceBuilder()
                        .query(boolQueryBuilder)
                        .docValueField("lineNo")
                        .docValueField("timestamp")
                        .size(Constants.SCROLL_MAX_LINES)
                        .sort("timestamp", SortOrder.ASC)
                        .sort("lineNo", SortOrder.ASC)
                )
                .scroll(TimeValue(1000 * 64))
            val scrollClient = logClient.hashClient(buildId)

            // 初始化请求
            val searchResponse = try {
                scrollClient.restClient.search(searchRequest, RequestOptions.DEFAULT)
            } catch (e: IOException) {
                scrollClient.restClient.search(searchRequest, genLargeSearchOptions())
            }

            var scrollId = searchResponse.scrollId
            var hits = searchResponse.hits

            // 开始滚动
            var times = 1
            do {
                hits.forEach { searchHitFields ->
                    val sourceMap = searchHitFields.sourceAsMap
                    val ln = sourceMap["lineNo"].toString().toLong()
                    val t = sourceMap["tag"]?.toString() ?: ""
                    val logLine = LogLine(
                        lineNo = ln,
                        timestamp = sourceMap["timestamp"].toString().toLong(),
                        message = sourceMap["message"].toString(),
                        priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                        tag = t,
                        subTag = sourceMap["subTag"]?.toString() ?: "",
                        jobId = sourceMap["jobId"]?.toString() ?: "",
                        executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                    )
                    queryLogs.logs.add(logLine)
                }
                times++
                val scrollRequest = SearchScrollRequest(scrollId).scroll(TimeValue(1000 * 64))
                val searchScrollResponse = try {
                    scrollClient.restClient.scroll(scrollRequest, RequestOptions.DEFAULT)
                } catch (e: IOException) {
                    scrollClient.restClient.scroll(scrollRequest, genLargeSearchOptions())
                }
                scrollId = searchScrollResponse.scrollId
                hits = searchScrollResponse.hits
            } while (hits.hits.isNotEmpty() && times <= Constants.SCROLL_MAX_TIMES)
            queryLogs.hasMore = logSize > queryLogs.logs.size
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
        } catch (e: ElasticsearchStatusException) {
            val exString = e.toString()
            if (exString.contains("index_closed_exception")) {
                logger.error("[$buildId] Can't search because of index_closed_exception", e)
                queryLogs.status = LogStatus.CLOSED.status
            }
        } catch (e: Exception) {
            logger.error("Query after logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL.status
            queryLogs.finished = true
            queryLogs.hasMore = false
        }
        return queryLogs
    }

    private fun doQueryLogsBeforeLine(
        buildId: String,
        index: String,
        end: Long,
        size: Int,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
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
        val queryLogs = QueryLogs(buildId = buildId, finished = logStatus, subTags = subTags)

        try {
            val startTime = System.currentTimeMillis()
            val logSize = getLogSize(
                index = index,
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                end = end
            )
            if (logSize == 0L) return queryLogs
            val start = when {
                end >= size -> {
                    end - size
                }
                logSize >= size -> {
                    logSize - size
                }
                else -> {
                    0
                }
            }
            val boolQueryBuilder = getQuery(
                buildId = buildId,
                debug = debug,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ).must(QueryBuilders.rangeQuery("lineNo").gte(start))
                .must(QueryBuilders.rangeQuery("lineNo").lte(end))

            logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] doQueryLogsBeforeLine get the query builder: $boolQueryBuilder")
            val searchRequest = SearchRequest(index)
                .source(
                    SearchSourceBuilder()
                        .query(boolQueryBuilder)
                        .docValueField("lineNo")
                        .docValueField("timestamp")
                        .size(size)
                        .sort("timestamp", SortOrder.ASC)
                        .sort("lineNo", SortOrder.ASC)
                        .timeout(TimeValue.timeValueSeconds(60))
                )

            val searchResponse = try {
                logClient.hashClient(buildId).restClient.search(searchRequest, RequestOptions.DEFAULT)
            } catch (e: IOException) {
                logClient.hashClient(buildId).restClient.search(searchRequest, genLargeSearchOptions())
            }

            searchResponse.hits.forEach { searchHitFields ->
                val sourceMap = searchHitFields.sourceAsMap
                val ln = sourceMap["lineNo"].toString().toLong()
                val t = sourceMap["tag"]?.toString() ?: ""
                val logLine = LogLine(
                    lineNo = ln,
                    timestamp = sourceMap["timestamp"].toString().toLong(),
                    message = sourceMap["message"].toString(),
                    priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    tag = t,
                    subTag = sourceMap["subTag"]?.toString() ?: "",
                    jobId = sourceMap["jobId"]?.toString() ?: "",
                    executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                queryLogs.logs.add(logLine)
            }
            queryLogs.hasMore = logSize > queryLogs.logs.size
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
        } catch (e: ElasticsearchStatusException) {
            val exString = e.toString()
            if (exString.contains("index_closed_exception")) {
                logger.error("[$buildId] Can't search because of index_closed_exception", e)
                queryLogs.status = LogStatus.CLOSED.status
            }
        } catch (e: Exception) {
            logger.error("Query before logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL.status
            queryLogs.finished = true
            queryLogs.hasMore = false
        }
        return queryLogs
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

    private fun getLogSize(
        index: String,
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        start: Long? = null,
        end: Long? = null
    ): Long {
        val query = getQuery(buildId, debug, logType, tag, subTag, jobId, executeCount)
        if (start != null) query.must(QueryBuilders.rangeQuery("lineNo").gte(start))
        if (end != null) query.must(QueryBuilders.rangeQuery("lineNo").lte(end))
        val countRequest = CountRequest(index).source(SearchSourceBuilder().query(query))
        val countResponse = logClient.hashClient(buildId).restClient.count(countRequest, RequestOptions.DEFAULT)
        return countResponse.count
    }

    private fun doAddMultiLines(logMessages: List<LogMessageWithLineNo>, buildId: String): Int {
        val currentEpoch = System.currentTimeMillis()
        val index = indexService.getIndexName(buildId)
        val bulkClient = logClient.hashClient(buildId)
        var lines = 0
        var bulkLines = 0
        val bulkRequest = BulkRequest()
            .timeout(TimeValue.timeValueMillis(bulkClient.requestTimeout))
        for (i in logMessages.indices) {
            val logMessage = logMessages[i]

            val indexRequest = genIndexRequest(
                buildId = buildId,
                logMessage = logMessage,
                index = index
            )
            if (indexRequest != null) {
                bulkRequest.add(indexRequest)
                lines++
            }
        }
        try {
            val bulkResponse = bulkClient.restClient.bulk(bulkRequest, RequestOptions.DEFAULT)
            bulkLines = bulkResponse.count()
            return if (bulkResponse.hasFailures()) {
                throw Exception(bulkResponse.buildFailureMessage())
            } else {
                bulkLines
            }
        } catch (ex: Exception) {
            val exString = ex.toString()
            if (exString.contains("circuit_breaking_exception")) {
                logger.error("$buildId|Add bulk lines failed|$exString, attempting to add index. [$logMessages]", ex)
                val bulkResponse = bulkClient.restClient
                    .bulk(bulkRequest.timeout(TimeValue.timeValueSeconds(60)), genLargeSearchOptions())
                bulkLines = bulkResponse.count()
                return if (bulkResponse.hasFailures()) {
                    logger.error(bulkResponse.buildFailureMessage())
                    0
                } else {
                    bulkLines
                }
            } else {
                logger.error("[$buildId] Add bulk lines failed because of unknown Exception. [$logMessages]", ex)
                throw ex
            }
        } finally {
            if (bulkLines != lines) {
                logger.warn("[$buildId] Part of bulk lines failed, lines:$lines, bulkLines:$bulkLines")
            }
            val elapse = System.currentTimeMillis() - currentEpoch
            logStorageBean.bulkRequest(elapse, bulkLines > 0)

            // #4265 当日志消息处理时间过长时打印消息内容
            if (elapse >= 500 && logMessages.isNotEmpty()) logger.warn(
                "[$buildId] doAddMultiLines spent too much time($elapse) with tag=${logMessages.first().tag}"
            )
        }
    }

    private fun genIndexRequest(
        buildId: String,
        logMessage: LogMessageWithLineNo,
        index: String
    ): IndexRequest? {
        val builder = ESIndexUtils.getDocumentObject(buildId, logMessage)
        return try {
            IndexRequest(index).source(builder)
        } catch (e: IOException) {
            logger.error("[$buildId] Convert logMessage to es document failure", e)
            null
        } finally {
            builder.close()
        }
    }

    private fun addLineNo(buildId: String, logMessages: List<LogMessage>): List<LogMessageWithLineNo> {
        val lineNum = indexService.getAndAddLineNum(buildId, logMessages.size)
        if (lineNum == null) {
            logger.error("Got null logIndex from indexService, buildId: $buildId")
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
        return if (!checkIndexCreate(buildId, index)) {
            createIndex(buildId, index)
            indexCache.put(index, true)
            true
        } else {
            true
        }
    }

    private fun checkIndexCreate(buildId: String, index: String): Boolean {
        if (indexCache.getIfPresent(index) == true) {
            return true
        }
        val redisLock = RedisLock(redisOperation, "LOG:index:create:lock:key:$index", 10)
        try {
            redisLock.lock()
            if (indexCache.getIfPresent(index) == true) {
                return true
            }

            // Check from ES
            if (isExistIndex(buildId, index)) {
                logger.info("[$buildId|$index] the index is already created")
                indexCache.put(index, true)
                return true
            }
            return false
        } finally {
            redisLock.unlock()
        }
    }

    private fun createIndex(buildId: String, index: String): Boolean {
        logger.info("[$index] Create index")
        var success = false
        val startEpoch = System.currentTimeMillis()
        val createClient = logClient.hashClient(buildId)
        return try {
            logger.info("[${createClient.clusterName}][$index]|createIndex|: shards[${createClient.shards}]" +
                " replicas[${createClient.replicas}] shardsPerNode[${createClient.shardsPerNode}]")
            val request = CreateIndexRequest(index)
                .settings(ESIndexUtils.getIndexSettings(
                    shards = createClient.shards,
                    replicas = createClient.replicas,
                    shardsPerNode = createClient.shardsPerNode
                ))
                .mapping(ESIndexUtils.getTypeMappings())
            request.setTimeout(TimeValue.timeValueSeconds(30))
            val response = createClient.restClient.indices()
                .create(request, RequestOptions.DEFAULT)
            success = true
            response.isShardsAcknowledged
        } catch (e: IOException) {
            logger.error("[${createClient.clusterName}] Create index $index failure", e)
            return false
        } finally {
            createIndexBean.execute(System.currentTimeMillis() - startEpoch, success)
        }
    }

    private fun isExistIndex(buildId: String, index: String): Boolean {
        val request = GetIndexRequest(index)
        request.setTimeout(TimeValue.timeValueSeconds(30))
        return logClient.hashClient(buildId).restClient.indices()
            .exists(request, RequestOptions.DEFAULT)
    }

    private fun getQuery(
        buildId: String,
        debug: Boolean,
        logType: LogType?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): BoolQueryBuilder {
        val query = QueryBuilders.boolQuery()
        if (!tag.isNullOrBlank()) {
            query.must(QueryBuilders.matchQuery("tag", tag).operator(Operator.AND))
        }
        if (!subTag.isNullOrBlank()) {
            query.must(QueryBuilders.matchQuery("subTag", subTag).operator(Operator.AND))
        }
        if (!jobId.isNullOrBlank()) {
            query.must(QueryBuilders.matchQuery("jobId", jobId).operator(Operator.AND))
        }
        if (logType != null) {
            query.must(QueryBuilders.matchQuery("logType", logType.name).operator(Operator.AND))
        }
        if (!debug) {
            query.mustNot(QueryBuilders.matchQuery("logType", LogType.DEBUG.name).operator(Operator.AND))
        }
        query.must(QueryBuilders.matchQuery("executeCount", executeCount ?: 1).operator(Operator.AND))
            .must(QueryBuilders.matchQuery("buildId", buildId).operator(Operator.AND))
        return query
    }

    private fun genLargeSearchOptions(): RequestOptions {
        val builder = RequestOptions.DEFAULT.toBuilder()
        builder.setHttpAsyncResponseConsumerFactory(HeapBufferedResponseConsumerFactory(
            Constants.RESPONSE_ENTITY_MAX_SIZE))
        return builder.build()
    }
}
