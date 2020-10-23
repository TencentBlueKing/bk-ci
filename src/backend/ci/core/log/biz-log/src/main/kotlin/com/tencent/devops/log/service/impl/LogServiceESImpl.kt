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
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.common.log.utils.LogMQEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.jmx.v2.CreateIndexBeanV2
import com.tencent.devops.log.jmx.v2.LogBeanV2
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.util.Constants
import com.tencent.devops.log.util.ESIndexUtils
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.MultiSearchRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.indices.IndexClosedException
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.LongStream
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import kotlin.math.max
import kotlin.math.min

class LogServiceESImpl constructor(
    private val client: LogClient,
    private val indexService: IndexService,
    private val logStatusService: LogStatusService,
    private val logTagService: LogTagService,
    private val defaultKeywords: List<String>,
    private val createIndexBeanV2: CreateIndexBeanV2,
    private val logBeanV2: LogBeanV2,
    private val redisOperation: RedisOperation,
    private val logMQEventDispatcher: LogMQEventDispatcher
) : LogService {

    companion object {
        private val logger = LoggerFactory.getLogger(LogServiceESImpl::class.java)
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
            val index = indexService.getIndexName(buildId)

            val logs = mutableListOf<LogLine>()
            val queryLogs = QueryLogs(buildId, getLogStatus(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ))

            try {
                val query = getQuery(buildId, tag, subTag, jobId, executeCount)
                    .must(QueryBuilders.rangeQuery("lineNo").gte(start).lte(end))

                val searchRequest = SearchRequest(index)
                    .source(SearchSourceBuilder()
                        .query(query)
                        .highlighter(
                            HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                                .field("message").fragmentSize(100000)
                        )
                        .docValueField("lineNo")
                        .docValueField("timestamp")
                        .size(num)
                        .sort("lineNo", if (fromStart) SortOrder.ASC else SortOrder.DESC)
                        .timeout(TimeValue.timeValueSeconds(60)))

                val searchResponse = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
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
                    logs.add(logLine)
                }
                if (!fromStart) {
                    logs.reverse()
                }
                queryLogs.logs.addAll(logs)
                success = true
            } catch (ex: IndexNotFoundException) {
                logger.error(
                    "Query more logs between lines failed because of IndexNotFoundException. buildId: $buildId",
                    ex
                )
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

        val index = indexService.getIndexName(buildId)
        val query = getQuery(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
            .must(QueryBuilders.matchQuery("logType", LogType.LOG.name).operator(Operator.AND))

        val scrollClient = client.restClient(buildId)
        val searchRequest = SearchRequest(index)
            .source(SearchSourceBuilder()
                .query(query)
                .docValueField("lineNo")
                .docValueField("timestamp")
                .size(4000)
                .sort("lineNo", SortOrder.ASC))
            .scroll(TimeValue(1000 * 64))

        var scrollResp = scrollClient.search(searchRequest, RequestOptions.DEFAULT)

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
                scrollResp = scrollClient.scroll(SearchScrollRequest(scrollResp.scrollId).scroll(TimeValue(1000 * 64)), RequestOptions.DEFAULT)
            } while (scrollResp.hits.hits.isNotEmpty())
        }

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
        } catch (ex: IndexNotFoundException) {
            logger.error("Query end logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            queryLogs.status = LogStatus.CLEAN
        } catch (e: IndexClosedException) {
            logger.error("Query end logs failed because of IndexClosedException. buildId: $buildId", e)
            queryLogs.status = LogStatus.CLOSED
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
                    val logSize = getLogSize(
                        index = index,
                        buildId = buildId,
                        tag = tag,
                        subTag = subTag,
                        jobId = jobId,
                        executeCount = executeCount
                    )
                    val totalPage = Math.ceil((logSize + 0.0) / pageSize).toInt()
                    Page(
                        count = logSize,
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
        logger.info("Reopen Index - $buildId")
        val index = indexService.getIndexName(buildId)
        return openIndex(buildId, index)
    }

    private fun logStatusSuccess(logStatus: LogStatus) = (logStatus == LogStatus.EMPTY || logStatus == LogStatus.SUCCEED)

    private fun openIndex(buildId: String, index: String): Boolean {
        logger.info("[$buildId|$index] Start to open the index")
        return client.restClient(buildId).indices()
            .open(OpenIndexRequest(index), RequestOptions.DEFAULT)
            .isAcknowledged
    }

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
        val index = indexService.getIndexName(buildId)

        try {
            val logs = getLogsByPage(
                buildId = buildId,
                index = index,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                page = page,
                pageSize = pageSize
            )
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
        } catch (ex: IndexNotFoundException) {
            logger.error("Query init logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            queryLogs.status = LogStatus.CLEAN
            queryLogs.finished = true
        } catch (e: IndexClosedException) {
            logger.error("Query init logs failed because of IndexClosedException. buildId: $buildId", e)
            queryLogs.status = LogStatus.CLOSED
            queryLogs.finished = true
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            queryLogs.status = LogStatus.FAIL
            queryLogs.finished = true
        }
        return queryLogs
    }

    private fun getLogsByPage(
        buildId: String,
        index: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): List<LogLine> {

        val boolQuery = QueryBuilders.boolQuery()
        if (page != -1 && pageSize != -1) {
            val endLineNo = pageSize * page
            val beginLineNo = endLineNo - pageSize + 1
            boolQuery.must(QueryBuilders.rangeQuery("lineNo").gte(beginLineNo).lte(endLineNo))
        }

        val query = getQuery(buildId, tag, subTag, jobId, executeCount)
            .must(boolQuery)

        val result = mutableListOf<LogLine>()
        val scrollClient = client.restClient(buildId)
        val searchRequest = SearchRequest(index)
            .source(SearchSourceBuilder()
                .query(query)
                .docValueField("lineNo")
                .docValueField("timestamp")
                .size(pageSize)
                .sort("lineNo", SortOrder.ASC)
                .timeout(TimeValue.timeValueSeconds(60)))
            .scroll(TimeValue(1000 * 64))

        var scrollResp = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
        do {
            scrollResp.hits.hits.forEach { searchHit ->
                val sourceMap = searchHit.sourceAsMap
                val logType = sourceMap["logType"].toString()
                val logLine = LogLine(
                    sourceMap["lineNo"].toString().toLong(),
                    sourceMap["timestamp"].toString().toLong(),
                    if (logType == LogType.LOG.name) sourceMap["message"].toString() else "",
                    Constants.DEFAULT_PRIORITY_NOT_DELETED
                )
                result.add(logLine)
            }
            scrollResp = scrollClient.scroll(SearchScrollRequest(scrollResp.scrollId).scroll(TimeValue(1000 * 64)), RequestOptions.DEFAULT)
        } while (scrollResp.hits.hits.isNotEmpty())

        return result
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
        val index = indexService.getIndexName(buildId)
        val query = getQuery(buildId, tag, subTag, jobId, executeCount)
        val searchRequest = SearchRequest(index)
            .source(SearchSourceBuilder()
                .query(query)
                .docValueField("lineNo")
                .docValueField("timestamp")
                .size(size)
                .sort("lineNo", SortOrder.ASC)
                .timeout(TimeValue.timeValueSeconds(60)))
            .scroll(TimeValue(1000 * 32))

        val scrollResp = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
        val logs = mutableListOf<LogLine>()
        scrollResp.hits.hits.forEach { searchHit ->
            val sourceMap = searchHit.sourceAsMap

            val logLine = LogLine(
                lineNo = sourceMap["lineNo"].toString().toLong(),
                timestamp = sourceMap["timestamp"].toString().toLong(),
                message = sourceMap["message"].toString(),
                priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                tag = sourceMap["tag"].toString() ?: "",
                subTag = sourceMap["subTag"].toString() ?: "",
                jobId = sourceMap["jobId"].toString() ?: "",
                executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
            )
            logs.add(logLine)
        }
        return EndPageQueryLogs(
            buildId = buildId,
            startLineNo = logs.lastOrNull()?.lineNo ?: 0,
            endLineNo = logs.firstOrNull()?.lineNo ?: 0,
            logs = logs,
            timeUsed = System.currentTimeMillis() - beginTime
        )
    }

    /**
     *
     * @param buildId
     * @param index
     * @param start
     * @param wholeQuery 是否查找所有关键
     * @param keywords
     * @return
     */
    private fun doQueryMoreLogsAfterLine(
        buildId: String,
        index: String,
        start: Long,
        wholeQuery: Boolean,
        keywords: List<String>,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val logs = ArrayList<LogLine>()
        val moreLogs = QueryLogs(buildId, getLogStatus(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        ))
        logger.info("more logs status: $moreLogs")

        try {
            val multiSearchRequest = MultiSearchRequest()

            if (wholeQuery) {
                val startQuery = getQuery(buildId, tag, subTag, jobId, executeCount)
                    .must(QueryBuilders.matchQuery("logType", LogType.START.name))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))
                val srbFoldStart = SearchRequest(index)
                    .source(SearchSourceBuilder()
                        .query(startQuery)
                        .docValueField("lineNo")
                        .size(100))

                val stopQuery = getQuery(buildId, tag, subTag, jobId, executeCount)
                    .must(QueryBuilders.matchQuery("logType", LogType.END.name))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))
                val srbFoldStop = SearchRequest(index)
                    .source(SearchSourceBuilder()
                        .query(stopQuery)
                        .docValueField("lineNo")
                        .size(100))

                multiSearchRequest.add(srbFoldStart).add(srbFoldStop)
            }

            val tempKeywords = if (keywords.isNotEmpty()) {
                keywords
            } else {
                defaultKeywords
            }

            for (keyword in tempKeywords) {
                val query = getQuery(buildId, tag, subTag, jobId, executeCount)
                    .must(QueryBuilders.matchQuery("message", keyword).operator(Operator.AND))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))

                val srbKeyword = SearchRequest(index)
                    .source(SearchSourceBuilder()
                        .query(query)
                        .highlighter(
                            HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                                .field("message").fragmentSize(100000)
                        )
                        .docValueField("lineNo")
                        .size(50))
                multiSearchRequest.add(srbKeyword)
            }

            val timeStart = System.currentTimeMillis()

            val multiSearchResponse = client.restClient(buildId).msearch(multiSearchRequest, RequestOptions.DEFAULT)
            moreLogs.timeUsed = System.currentTimeMillis() - timeStart
            val lineNoSet = TreeSet<Long>()
            val highlights = HashMap<Long, String>()

            multiSearchResponse.responses
                .map { it.response }
                .filter { it != null && it.hits != null }
                .forEach { response ->
                    response.hits.forEach {
                        // 对 No such process 作特殊处理
                        val message = it.sourceAsMap["message"].toString()
                        if (!message.isBlank() && !message.contains("No such process")) {
                            val ln = it.field("lineNo").getValue<Long>()
                            lineNoSet.add(ln)
                            if (it.highlightFields.isNotEmpty()) {
                                highlights[ln] = it.highlightFields["message"]!!.fragments[0].toString()
                            }
                        }
                    }
                }

            // 开始处理需要返回的行号
            val lineRanges = parseToLineRangesGetAfterLines(
                lineNoSet, Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
            val lines = parseToLineNos(lineRanges)

            val query = getQuery(buildId, tag, subTag, jobId, executeCount)
                .must(QueryBuilders.rangeQuery("lineNo").gte(start))

            val searchRequest = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(query)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(Constants.MAX_LINES)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60)))

            val searchResponse = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)

            // 简单处理，如果得到的数据量与请求的数据量一样，认为还未 finished
//            if (searchResponse.hits.getTotalHits() == Constants.MAX_LINES.toLong()) {
//                moreLogs.finished = false
//            }

            var lastLineNo = -1L
            for (searchHitFields in searchResponse.hits) {
                val sourceMap = searchHitFields.sourceAsMap
                val lineNo = java.lang.Long.parseLong(sourceMap["lineNo"].toString())
                if (lastLineNo != -1L && lineNo - lastLineNo > 1L) {
                    break
                }
                lastLineNo = lineNo

                val logLine = LogLine(
                    lineNo = lineNo,
                    timestamp = sourceMap["timestamp"].toString().toLong(),
                    message = if (highlights.containsKey(lineNo)) {
                        highlights[lineNo] ?: ""
                    } else {
                        sourceMap["message"].toString()
                    },
                    priority = if (lines.contains(lineNo)) {
                        Constants.DEFAULT_PRIORITY_NOT_DELETED
                    } else {
                        0
                    },
                    tag = sourceMap["tag"].toString(),
                    subTag = sourceMap["subTag"].toString(),
                    jobId = sourceMap["jobId"].toString()
                )
                logs.add(logLine)
            }

            moreLogs.logs.addAll(logs)
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
            logger.error("Query after logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            moreLogs.status = LogStatus.CLEAN
            moreLogs.finished = true
        } catch (e: IndexClosedException) {
            logger.error("Query after logs failed because of IndexClosedException. buildId: $buildId", e)
            moreLogs.status = LogStatus.CLOSED
            moreLogs.finished = true
        } catch (e: Exception) {
            logger.error("Query after logs failed because of ${e.javaClass}. buildId: $buildId", e)
            moreLogs.status = LogStatus.FAIL
            moreLogs.finished = true
        }

        return moreLogs
    }

    private fun doQueryMoreOriginLogsAfterLine(
        buildId: String,
        index: String,
        start: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): QueryLogs {
        val logs = ArrayList<LogLine>()
        val moreLogs = QueryLogs(buildId, getLogStatus(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        ))
        val querySize = Constants.MAX_LINES
        logger.info("more logs status: $moreLogs")

        try {
            val query = getQuery(buildId, tag, subTag, jobId, executeCount)
                .must(QueryBuilders.rangeQuery("lineNo").gte(start))

            val searchRequest = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(query)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(querySize)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60)))

            val searchResponse = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
            var lastLineNo = -1L
            for (searchHitFields in searchResponse.hits) {
                val sourceMap = searchHitFields.sourceAsMap
                val lineNo = java.lang.Long.parseLong(sourceMap["lineNo"].toString())
                if (lastLineNo != -1L && lineNo - lastLineNo > 1L) {
                    break
                }
                lastLineNo = lineNo

                val logLine = LogLine(
                    sourceMap["lineNo"].toString().toLong(),
                    sourceMap["timestamp"].toString().toLong(),
                    sourceMap["message"].toString(),
                    Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    sourceMap["tag"].toString() ?: "",
                    sourceMap["subTag"].toString() ?: "",
                    sourceMap["jobId"].toString() ?: "",
                    sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                logs.add(logLine)
            }
            moreLogs.logs.addAll(logs)
            moreLogs.hasMore = moreLogs.logs.size >= querySize
        } catch (ex: IndexNotFoundException) {
            logger.error("Query after logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            moreLogs.status = LogStatus.CLEAN
            moreLogs.finished = true
            moreLogs.hasMore = false
        } catch (e: IndexClosedException) {
            logger.error("Query after logs failed because of IndexClosedException. buildId: $buildId", e)
            moreLogs.status = LogStatus.CLOSED
            moreLogs.finished = true
            moreLogs.hasMore = false
        } catch (e: Exception) {
            logger.error("Query after logs failed because of ${e.javaClass}. buildId: $buildId", e)
            moreLogs.status = LogStatus.FAIL
            moreLogs.finished = true
            moreLogs.hasMore = false
        }

        return moreLogs
    }

    /**
     * 获取实时的尾部更多日志时，根据权限关键日志的行号，得到高权重的行号范围；
     * @param lineNos 关键日志行号集
     * @param numLinesAroundTags 关键日志周围显示的行数
     * @return 行号范围集
     */
    private fun parseToLineRangesGetAfterLines(
        lineNos: TreeSet<Long>?,
        numLinesAroundTags: Long
    ): List<Pair<Long, Long>> {
        val lineRanges = ArrayList<Pair<Long, Long>>()
        if (lineNos == null || lineNos.size == 0) {
            return lineRanges
        }

        // 由于 TreeSet<Long> lineNos 没有 get() 操作，所以先转成List<Long>
        val lineNosList = lineNos.stream().sorted().collect(Collectors.toList())

        var lastPair = Pair(
            lineNosList[0] - numLinesAroundTags,
            lineNosList[0] + numLinesAroundTags
        )
        var tempLeft: Long
        var tempRight: Long

        for (lineNo in lineNosList) {
            tempLeft = lineNo - numLinesAroundTags
            tempRight = lineNo + numLinesAroundTags

            lastPair = if (lastPair.second < tempLeft) {
                lineRanges.add(lastPair)
                Pair(tempLeft, tempRight)
            } else {
                Pair(
                    if (lastPair.first <= tempLeft) lastPair.first else tempLeft,
                    if (lastPair.second >= tempRight) lastPair.second else tempRight
                )
            }
        }

        lineRanges.add(lastPair)

        return lineRanges
    }

    /**
     * 将行号范围集转为行号集
     */
    private fun parseToLineNos(ranges: List<Pair<Long, Long>>?): Set<Long> {
        val nos = HashSet<Long>()
        if (ranges != null && !ranges.isEmpty()) {
            for (pair in ranges) {
                nos.addAll(LongStream.rangeClosed(pair.first, pair.second).boxed().collect(Collectors.toList()))
            }
        }
        return nos
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
            val size = getLogSize(
                index = index,
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            if (size == 0L) {
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
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
            logger.error("Query init logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            initLogs.status = LogStatus.CLEAN
        } catch (e: IndexClosedException) {
            logger.error("Query init logs failed because of IndexClosedException. buildId: $buildId", e)
            initLogs.status = LogStatus.CLOSED
        } catch (e: Exception) {
            logger.error("Query init logs failed because of ${e.javaClass}. buildId: $buildId", e)
            initLogs.status = LogStatus.FAIL
        }
        return initLogs
    }

    private fun getLogSize(
        index: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Long {
        val query = getQuery(buildId, tag, subTag, jobId, executeCount)
        val countRequest = CountRequest(index)
            .source(SearchSourceBuilder().query(query))
        return try {
            val countResponse = client.restClient(buildId).count(countRequest, RequestOptions.DEFAULT)
            countResponse.count
        } catch (e: Throwable) {
            logger.error("[$buildId] Get log size with error: ${e.printStackTrace()}")
            0
        }
    }

    private fun getQuery(
        buildId: String,
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
        query.must(QueryBuilders.matchQuery("executeCount", executeCount ?: 1).operator(Operator.AND))
            .must(QueryBuilders.matchQuery("buildId", buildId).operator(Operator.AND))
        return query
    }

    private fun doQueryInitLogs(
        buildId: String,
        index: String,
        tag: String? = null,
        subTag: String? = null,
        jobId: String? = null,
        executeCount: Int?
    ): QueryLogs {
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
            val size = getLogSize(
                index = index,
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            if (size == 0L) return queryLogs
            val logRange = getLogRange(
                buildId = buildId,
                index = index,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )
            logger.info("[$index|$buildId|$tag|$subTag|$jobId|$executeCount] getOriginLogs with range: $logRange")

            val startTime = System.currentTimeMillis()
            val logs = mutableListOf<LogLine>()
            val boolQueryBuilder = getQuery(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            logger.info("Get the query builder: $boolQueryBuilder")

            val searchRequest = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(boolQueryBuilder)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(Constants.MAX_LINES)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60)))

            val response = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
            response.hits.forEach { searchHitFields ->
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
                logs.add(logLine)
            }
            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
            queryLogs.hasMore = size > logs.size
        } catch (ex: IndexNotFoundException) {
            logger.error("Query init logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            queryLogs.status = LogStatus.CLEAN
            queryLogs.finished = true
            queryLogs.hasMore = false
        } catch (e: IndexClosedException) {
            logger.error("Query init logs failed because of IndexClosedException. buildId: $buildId", e)
            queryLogs.status = LogStatus.CLOSED
            queryLogs.finished = true
            queryLogs.hasMore = false
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
            val logs = mutableListOf<LogLine>()
            val boolQueryBuilder = getQuery(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            ).must(QueryBuilders.rangeQuery("lineNo").gte(start))

            val searchRequest = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(boolQueryBuilder)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(Constants.MAX_LINES)
                    .sort("lineNo", SortOrder.ASC))
                .scroll(TimeValue(1000 * 64))
            val scrollClient = client.restClient(buildId)

            // 初始化请求
            val searchResponse = scrollClient.search(searchRequest, RequestOptions.DEFAULT)
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
                    logs.add(logLine)
                }
                times++
                val scrollRequest = SearchScrollRequest(scrollId).scroll(TimeValue(1000 * 64))
                val searchScrollResponse = scrollClient.scroll(scrollRequest, RequestOptions.DEFAULT)
                scrollId = searchScrollResponse.scrollId
                hits = searchScrollResponse.hits
            } while (hits.hits.isNotEmpty() && times < Constants.SCROLL_MAX_TIMES)

            logger.info("logs query time cost: ${System.currentTimeMillis() - startTime}")
            moreLogs.logs.addAll(logs)
            moreLogs.hasMore = moreLogs.logs.size >= Constants.MAX_LINES * Constants.SCROLL_MAX_TIMES
        } catch (ex: IndexNotFoundException) {
            logger.error("Query after logs failed because of IndexNotFoundException. buildId: $buildId", ex)
            moreLogs.status = LogStatus.CLEAN
            moreLogs.finished = true
            moreLogs.hasMore = false
        } catch (e: IndexClosedException) {
            logger.error("Query after logs failed because of IndexClosedException. buildId: $buildId", e)
            moreLogs.status = LogStatus.CLOSED
            moreLogs.finished = true
            moreLogs.hasMore = false
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

    private fun getLogsByKeywords(
        buildId: String,
        index: String,
        keywords: List<String>,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): TreeSet<Long> {
        logger.info("[$buildId|$index|$tag|$subTag|$jobId|$executeCount] get log by keywords: " +
            "index: $index, keywords: $keywords, tag: $tag, jobId: $jobId, executeCount: $executeCount")

        val size = getLogSize(
            index = index,
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        if (size == 0L) {
            return TreeSet()
        }

        val query = getQuery(buildId, tag, subTag, jobId, executeCount)
        val multiSearchRequest = MultiSearchRequest()

        val logRange =
            if (tag.isNullOrBlank()) Pair(1L, size)
            else getLogRange(
                buildId = buildId,
                index = index,
                tag = tag!!,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )

        keywords.forEach {
            val srbKeyword = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(query
                        .must(QueryBuilders.matchQuery("message", it).operator(Operator.AND))
                        .must(QueryBuilders.rangeQuery("lineNo").gte(logRange.first)))
                    .docValueField("lineNo")
                    .size(50)
                    .timeout(TimeValue.timeValueSeconds(60)))
            multiSearchRequest.add(srbKeyword)
        }

        val lineNoSet = TreeSet<Long>()

        val multiSearchResponse = client.restClient(buildId).msearch(multiSearchRequest, RequestOptions.DEFAULT)
        multiSearchResponse.responses
            .map { it.response }
            .filter { it != null && it.hits != null }
            .forEach { response ->
                response.hits.forEach {
                    // 对 No such process 作特殊处理
                    val message = it.sourceAsMap["message"].toString()
                    if (!message.isBlank() && !message.contains("No such process")) {
                        val ln = it.field("lineNo").getValue<Long>()
                        lineNoSet.add(ln)
                    }
                }
            }
        return lineNoSet
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
        logger.info("[$buildId|$index|$tag|$subTag|$jobId|$executeCount] log params: " +
            "index: $index, keywords: $keywords, wholeQuery: $wholeQuery, tag: $tag, jobId: $jobId, executeCount: $executeCount")

        val size = getLogSize(
            index = index,
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
        if (size == 0L) {
            return listOf()
        }

        val multiSearchRequest = MultiSearchRequest()

        val logRange =
            if (tag.isNullOrBlank()) Pair(1L, size)
            else getLogRange(
                buildId = buildId,
                index = index,
                tag = tag!!,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )

        logger.info("log range for $buildId: (${logRange.first}, ${logRange.second}), size: $size")

        var startTime = System.currentTimeMillis()

        // 高亮关键字
        // 传了tag就认为不是全量查询
        if (wholeQuery && tag.isNullOrBlank()) {

            val srbFoldStart = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(QueryBuilders.matchQuery("logType", LogType.START.name))
                    .docValueField("lineNo")
                    .size(100))
            val srbFoldStop = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(QueryBuilders.matchQuery("logType", LogType.START.name))
                    .docValueField("lineNo")
                    .size(100))
            multiSearchRequest.add(srbFoldStart).add(srbFoldStop)
        }

        val query = getQuery(buildId, tag, subTag, jobId, executeCount)

        keywords.forEach {
            val srbKeyword = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(query
                        .must(QueryBuilders.matchQuery("message", it).operator(Operator.AND))
                        .must(QueryBuilders.rangeQuery("lineNo").gte(logRange.first)))
                    .highlighter(
                        HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                            .field("message").fragmentSize(100000)
                    )
                    .docValueField("lineNo")
                    .size(50)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60)))
            multiSearchRequest.add(srbKeyword)
        }

        val lineNoSet = java.util.TreeSet<Long>()

        val highlights = HashMap<Long, String>()

        val multiSearchResponse = client.restClient(buildId).msearch(multiSearchRequest, RequestOptions.DEFAULT)

        multiSearchResponse.responses
            .map { it.response }
            .filter { it != null && it.hits != null }
            .forEach { response ->
                response.hits.forEach {
                    // 对 No such process 作特殊处理
                    val message = it.sourceAsMap["message"].toString()
                    if (!message.isBlank() && !message.contains("No such process")) {
                        val ln = it.field("lineNo").getValue<Long>()
                        lineNoSet.add(ln)
                        if (it.highlightFields.isNotEmpty()) {
                            highlights[ln] = it.highlightFields["message"]!!.fragments[0].toString()
                        }
                    }
                }
            }

        logger.info("step1 time cost: ${System.currentTimeMillis() - startTime}")
        startTime = System.currentTimeMillis()

        if (wholeQuery) {
            lineNoSet.add(logRange.first)
            lineNoSet.add(logRange.second)
        } else {
            if (!lineNoSet.isEmpty()) {
                lineNoSet.add(lineNoSet.first() - Constants.NUM_LINES_AROUND_TAGS)
                lineNoSet.add(lineNoSet.last() + Constants.NUM_LINES_AROUND_TAGS)
            }
        }

        // 开始处理需要返回的行号
        val lineRanges = if (wholeQuery) {
            parseToLineRangesGetInitLines(
                lineNoSet,
                Constants.NUM_LINES_START.toLong(),
                Constants.NUM_LINES_END.toLong(),
                Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
        } else {
            parseToLineRangesGetInitLines(
                lineNoSet,
                Constants.NUM_LINES_AROUND_TAGS.toLong(),
                Constants.NUM_LINES_AROUND_TAGS.toLong(),
                Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
        }

        val logs = mutableListOf<LogLine>()
        if (!lineRanges.isEmpty()) {
            val boolQueryBuilder = getQuery(buildId, tag, subTag, jobId, executeCount)

            val rangeQuery = QueryBuilders.boolQuery()
            for (lineRange in lineRanges) {
                rangeQuery.should(
                    QueryBuilders
                        .rangeQuery("lineNo")
                        .gte(lineRange.first)
                        .lte(lineRange.second)
                )
            }
            boolQueryBuilder.must(rangeQuery)

            val searchRequest = SearchRequest(index)
                .source(SearchSourceBuilder()
                    .query(boolQueryBuilder)
                    .docValueField("lineNo")
                    .docValueField("timestamp")
                    .size(Constants.MAX_LINES)
                    .sort("lineNo", SortOrder.ASC)
                    .timeout(TimeValue.timeValueSeconds(60)))

            val response = client.restClient(buildId).search(searchRequest, RequestOptions.DEFAULT)
            response.hits.forEach { searchHitFields ->
                val sourceMap = searchHitFields.sourceAsMap
                val ln = sourceMap["lineNo"].toString().toLong()
                val logLine = LogLine(
                    lineNo = ln,
                    timestamp = sourceMap["timestamp"].toString().toLong(),
                    message = if (highlights.containsKey(ln)) {
                        highlights[ln] ?: ""
                    } else {
                        sourceMap["message"].toString()
                    },
                    priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    tag = sourceMap["tag"]?.toString() ?: "",
                    subTag = sourceMap["subTag"]?.toString() ?: "",
                    jobId = sourceMap["jobId"]?.toString() ?: "",
                    executeCount = sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                logs.add(logLine)
            }
            val numLogs = logs.size

            logger.info("step2 time cost: ${System.currentTimeMillis() - startTime}")
            startTime = System.currentTimeMillis()

            // 添加上线查看更多的标志日志
            if (numLogs > 0) {
                if (logs[0].lineNo > logRange.first) {
                    logs.add(
                        index = 0,
                        element = genLogMsgThereIsMore(
                            tagPrefix = "soda_more",
                            timeStamp = java.lang.Long.MIN_VALUE,
                            numMore = logs[0].lineNo - logRange.first,
                            start = logRange.first,
                            end = logs[0].lineNo - 1,
                            tag = tag,
                            jobId = jobId,
                            executeCount = executeCount
                        )
                    )
                }
                if (logs[logs.size - 1].lineNo < logRange.second) {
                    logs.add(
                        genLogMsgThereIsMore(
                            tagPrefix = "soda_more",
                            timeStamp = java.lang.Long.MAX_VALUE,
                            numMore = logRange.second - logs[logs.size - 1].lineNo,
                            start = logs[logs.size - 1].lineNo + 1,
                            end = logRange.second,
                            tag = tag,
                            jobId = jobId,
                            executeCount = executeCount
                        )
                    )
                }
            }

            // 取数据
            for (i in numLogs - 1 downTo 2) {
                val (lineNo, timestamp) = logs[i - 1]
                val (lineNo1) = logs[i]
                if (lineNo1 > lineNo + 1) {
                    logs.add(
                        index = i,
                        element = genLogMsgThereIsMore(
                            tagPrefix = "soda_more",
                            timeStamp = timestamp,
                            numMore = lineNo1 - lineNo - 1,
                            start = lineNo + 1,
                            end = lineNo1 - 1,
                            tag = tag,
                            jobId = jobId,
                            executeCount = executeCount
                        )
                    )
                }
            }
        }
        logger.info("step3 time cost: ${System.currentTimeMillis() - startTime}")

        return logs
    }

    private fun genLogMsgThereIsMore(
        tagPrefix: String,
        timeStamp: Long,
        numMore: Long,
        start: Long,
        end: Long,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): LogLine {
        return LogLine(
            lineNo = -1L,
            timestamp = timeStamp,
            message = "$tagPrefix:num=$numMore,start=$start,end=$end",
            priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
            tag = tag ?: "",
            jobId = jobId ?: "",
            executeCount = executeCount
        )
    }

    /**
     * 获取首屏日志时，根据权限关键日志的行号，得到高权重的行号范围；
     * @param lineNos 关键日志行号集
     * @param numLinesStart 首部必须显示的行数
     * @param numLinesEnd 尾部必须显示的行数
     * @param numLinesAroundTags 关键日志周围显示的行数
     * @return 行号范围集
     */
    private fun parseToLineRangesGetInitLines(
        lineNos: TreeSet<Long>?,
        numLinesStart: Long,
        numLinesEnd: Long,
        numLinesAroundTags: Long
    ): List<Pair<Long, Long>> {
        val lineRanges = ArrayList<Pair<Long, Long>>()
        if (lineNos == null || lineNos.size == 0) {
            return lineRanges
        }
        if (lineNos.size == 1) {
            lineRanges.add(Pair(lineNos.first(), lineNos.first()))
            return lineRanges
        }

        // 由于 TreeSet<Long> lineNos 没有 get() 操作，所以先转成List<Long>
        val lineNosList = lineNos.stream().sorted().collect(Collectors.toList())
        val numNos = lineNosList.size
        val minLine = lineNosList[0]
        val maxLine = lineNosList[numNos - 1]

        if (maxLine - minLine <= numLinesStart + numLinesEnd) {
            lineRanges.add(Pair(minLine, maxLine))
            return lineRanges
        }

        var lastPair = Pair(minLine, minLine + numLinesStart - 1)
        var tempLeft: Long
        var tempRight: Long

        (1 until lineNosList.size - 1 - 1).forEach {
            tempLeft = lineNosList[it] - numLinesAroundTags
            tempRight = lineNosList[it] + numLinesAroundTags

            if (tempLeft < minLine) tempLeft = minLine
            if (tempRight > maxLine) tempRight = maxLine

            lastPair = if (lastPair.second < tempLeft) {
                lineRanges.add(lastPair)
                Pair(tempLeft, tempRight)
            } else {
                Pair(
                    if (lastPair.first <= tempLeft) lastPair.first else tempLeft,
                    if (lastPair.second >= tempRight) lastPair.second else tempRight
                )
            }
        }

        tempLeft = lineNosList[numNos - 1] - numLinesEnd + 1
        tempRight = lineNosList[numNos - 1]

        if (tempLeft < minLine) tempLeft = minLine
        if (tempRight > maxLine) tempRight = maxLine

        lastPair = if (lastPair.second < tempLeft) {
            lineRanges.add(lastPair)
            Pair(tempLeft, tempRight)
        } else {
            Pair(
                if (lastPair.first <= tempLeft) lastPair.first else tempLeft,
                if (lastPair.second >= tempRight) lastPair.second else tempRight
            )
        }

        lineRanges.add(lastPair)

        return lineRanges
    }

    private fun getLogRange(
        buildId: String,
        index: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Long
    ): Pair<Long, Long> {

        val q = getQuery(buildId, tag, subTag, jobId, executeCount)
            .must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("logType", LogType.START.name).operator(Operator.OR))
                    .should(QueryBuilders.matchQuery("logType", LogType.END.name).operator(Operator.OR))
            )

        logger.info("[$index|$tag|$subTag|$jobId|$executeCount|$size] Get log range with query ($q)")

        val searchRequest = SearchRequest(index)
            .source(SearchSourceBuilder()
                .query(q)
                .docValueField("lineNo")
                .size(200)
                .sort("lineNo", SortOrder.ASC)
                .timeout(TimeValue.timeValueSeconds(60)))
        try {
            val searchResponse = client.restClient(buildId)
                .search(searchRequest, RequestOptions.DEFAULT)
            val hits = searchResponse.hits
            logger.info("[$index|$tag|$subTag|$jobId|$executeCount|$size] Get log range with searchResponse $searchResponse")
            logger.info("hits 0 for build with response (${hits.hits.size})")
            if (hits.hits.isEmpty()) return Pair(0, 0)
            return getRangeIndex(hits, tag, subTag, jobId)
        } catch (e: Exception) {
            logger.error("Get log range with error", e.toString())
            return Pair(0, 0)
        }
    }

    private fun getRangeIndex(
        hits: SearchHits,
        tag: String?,
        subTag: String?,
        jobId: String?
    ): Pair<Long, Long> {
        var beginIndex: Long? = null
        var endIndex: Long? = null
        run lit@{
            hits.forEach { hit ->
                if ((tag != null && hit.sourceAsMap["tag"] == tag) ||
                    (subTag != null && hit.sourceAsMap["subTag"] == tag) ||
                    (jobId != null && hit.sourceAsMap["jobId"] == jobId)) {
                    // 尽量取最大的区间
                    if (hit.sourceAsMap["logType"] == LogType.START.name) {
                        val lineNo = hit.sourceAsMap["lineNo"].toString().toLong()
                        beginIndex = if (beginIndex == null) lineNo else min(beginIndex!!, lineNo)
                    } else if (hit.sourceAsMap["logType"] == LogType.END.name) {
                        val lineNo = hit.sourceAsMap["lineNo"].toString().toLong()
                        endIndex = if (endIndex == null) lineNo else max(endIndex!!, lineNo)
                    }
                    if (beginIndex != null && endIndex != null) {
                        return@lit
                    }
                }
            }
        }
        if (beginIndex == null || endIndex == null) {
            logger.warn("[$tag|$beginIndex|$endIndex] Fail to get the begin index or endIndex")
            return Pair(0, 0)
        }
        return Pair(beginIndex!!, endIndex!!)
    }

    private fun doAddMultiLines(logMessages: List<LogMessageWithLineNo>, buildId: String): Int {

        val index = indexService.getIndexName(buildId)

        var lines = 0
        val bulkRequest = BulkRequest()
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
            // 注意，在 bulk 下，TypeMissingException 不会抛出，需要判断 bulkResponse.hasFailures() 抛出
            val bulkResponse = client.restClient(buildId).bulk(bulkRequest, RequestOptions.DEFAULT)
            return if (bulkResponse.hasFailures()) {
                throw Exception(bulkResponse.buildFailureMessage())
            } else {
                lines
            }
        } catch (ex: Exception) {
            val exString = ex.toString()
            if (exString.contains("TypeMissingException")) {
                logger.error(
                    "[$buildId] Add bulk lines failed because of TypeMissingException, attempting to add index. [$logMessages]",
                    ex
                )

                startLog(buildId, true)

                val bulkResponse = client.restClient(buildId)
                    .bulk(bulkRequest.timeout(TimeValue.timeValueSeconds(60)), RequestOptions.DEFAULT)
                return if (bulkResponse.hasFailures()) {
                    logger.error(bulkResponse.buildFailureMessage())
                    0
                } else {
                    lines
                }
            } else {
                logger.error("[$buildId] Add bulk lines failed because of unknown Exception. [$logMessages]", ex)
                throw ex
            }
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

    private fun startLog(buildId: String, force: Boolean = false): Boolean {
        val index = indexService.getIndexName(buildId)
        return if (force || !checkIndexCreate(buildId, index)) {
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
        return try {
            logger.info("[$index] Start to create the index")
            val request = CreateIndexRequest(index)
                .settings(ESIndexUtils.getIndexSettings())
                .mapping(ESIndexUtils.getTypeMappings())
            request.setTimeout(TimeValue.timeValueSeconds(30))
            val response = client.restClient(buildId).indices()
                .create(request, RequestOptions.DEFAULT)
            success = true
            response.isShardsAcknowledged
        } catch (e: IOException) {
            logger.error("Create index $index failure", e)
            return false
        } finally {
            createIndexBeanV2.execute(System.currentTimeMillis() - startEpoch, success)
        }
    }

    private fun isExistIndex(buildId: String, index: String): Boolean {
        val request = GetIndexRequest(index)
        request.setTimeout(TimeValue.timeValueSeconds(30))
        return client.restClient(buildId).indices()
            .exists(request, RequestOptions.DEFAULT)
    }
}