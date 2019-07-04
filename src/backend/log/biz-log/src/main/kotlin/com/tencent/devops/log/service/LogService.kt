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

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.jmx.CreateIndexBean
import com.tencent.devops.log.jmx.LogBean
import com.tencent.devops.log.jmx.UpdateIndexBean
import com.tencent.devops.log.model.message.LogMessage
import com.tencent.devops.log.model.message.LogMessageWithLineNo
import com.tencent.devops.log.model.pojo.EndPageQueryLogs
import com.tencent.devops.log.model.pojo.LogBatchEvent
import com.tencent.devops.log.model.pojo.LogEvent
import com.tencent.devops.log.model.pojo.LogLine
import com.tencent.devops.log.model.pojo.LogStatusEvent
import com.tencent.devops.log.model.pojo.PageQueryLogs
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.model.pojo.enums.LogStatus
import com.tencent.devops.log.model.pojo.enums.LogType
import com.tencent.devops.log.util.Constants
import com.tencent.devops.log.utils.LogDispatcher
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.indices.IndexClosedException
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.LongStream
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

/**
 * Log 日志
 * 1. 日志先经过logEvent， 这一步单线程执行，主要是保证日志行号
 * 2. 日志再传入队列，然后多线程消费， 保证写入ES高效
 */
@Service
open class LogService @Autowired constructor(
    private val client: TransportClient,
    private val indexService: IndexService,
    private val defaultKeywords: List<String>,
    private val logBean: LogBean,
    private val updateIndexBean: UpdateIndexBean,
    private val createIndexBean: CreateIndexBean,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(LogService::class.java)

    private val typeMappingCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*buildId*/, Boolean/*has create the mapping*/>()

    private fun checkTypeMapping(buildId: String) {
        if (typeMappingCache.getIfPresent(buildId) == true) {
            return
        }
        val redisLock = RedisLock(redisOperation, genRedisKey(buildId), 10)
        redisLock.lock()
        try {
            if (typeMappingCache.getIfPresent(buildId) == true) {
                return
            }
            // Get from DB
            if (indexService.isTypeMappingCreate(buildId)) {
                logger.info("[$buildId] The type mapping is already created")
                typeMappingCache.put(buildId, true)
                return
            }

            startLog(buildId)
            typeMappingCache.put(buildId, true)
        } finally {
            redisLock.unlock()
        }
    }

    open fun addLogEvent(event: LogEvent) {
        logger.info("[${event.buildId}] Start to add the logs of build - (${event.logs.size})")
        val currentEpoch = System.currentTimeMillis()
        var success = false
        try {
            checkTypeMapping(event.buildId)
            val logMessage = addLineNo(event.buildId, event.logs)
            LogDispatcher.dispatch(rabbitTemplate, LogBatchEvent(event.buildId, logMessage))
            success = true
        } finally {
            logger.info(
                "[${event.buildId}] It took ${System.currentTimeMillis()
                    - currentEpoch}ms to add event log with result $success"
            )
        }
    }

    open fun addBatchLogEvent(event: LogBatchEvent) {
        logger.info("[${event.buildId}] Start to add the logs of build - (${event.logs.size})")
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
            logger.info(
                "It took ${elapse}ms to add the logs ${event.logs.size} " +
                    "with result $success [${event.buildId}]"
            )
            logBean.execute(elapse, success)
        }
    }

    open fun upsertLogStatus(event: LogStatusEvent): Boolean {
        val buildId = event.buildId
        val tag = event.tag
        val finished = event.finished
        logger.info("MQ: Start to update log status [$buildId|$finished|$tag]")
        checkTypeMapping(buildId)
        var id = buildId
        if (tag.isNotBlank()) id += "-$tag"

        if (finished) {
            refreshIndex(buildId)
        }

        val builder: XContentBuilder
        try {
            builder = XContentFactory.jsonBuilder()
                .startObject()
                .field("buildId", buildId)
                .field("finished", finished)
                .field("updateTime", System.currentTimeMillis())

            if (!tag.isBlank()) builder.field("tag", tag)

            builder.endObject()
        } catch (e: IOException) {
            logger.error("Convert data to es document failure", e)
            return false
        }

        val response = client.prepareIndex(Constants.INDEX_LOG_STATUS, Constants.TYPE_LOG_STATUS, id)
            .setCreate(false) // 不强制创建索引
            .setSource(builder).get()
        return response.status() == RestStatus.OK || response.status() == RestStatus.CREATED
    }

    open fun queryInitLogsPage(
        buildId: String,
        index: String,
        type: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): PageQueryLogs {

        val pageResult: QueryLogs
        val pageLog = if (keywordsStr == null || keywordsStr.isBlank()) {
            if (isAnalysis) {
                pageResult = doQueryByKeywords(buildId, index, type, 1, defaultKeywords, tag, executeCount)
                val logSize = pageResult.logs.size
                Page(logSize.toLong(), 1, logSize, 1, pageResult.logs.filter { it.lineNo != -1L })
            } else {
                pageResult = queryInitLogsPage(
                    buildId, index, type,
                    !isAnalysis, defaultKeywords, tag, executeCount, page, pageSize
                )
                val logSize = getLogSize(index, buildId)
                val totalPage = Math.ceil((logSize + 0.0) / pageSize).toInt()
                Page(logSize, page, pageSize, totalPage, pageResult.logs)
            }
        } else {
            val keywords = keywordsStr.split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .stream()
                .filter { k -> k.isNotBlank() }
                .collect(Collectors.toList())
            pageResult = doQueryByKeywords(buildId, index, type, 1, keywords, tag, executeCount)
            val logSize = pageResult.logs.size
            Page(logSize.toLong(), 1, logSize, 1, pageResult.logs.filter { it.lineNo != -1L })
        }
        return PageQueryLogs(pageResult.buildId, pageResult.finished, pageLog, pageResult.timeUsed, pageResult.status)
    }

    open fun queryInitLogs(
        buildId: String,
        index: String,
        type: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        executeCount: Int?
    ): QueryLogs {
        if (keywordsStr == null || keywordsStr.isBlank()) {
            return if (isAnalysis) {
                doQueryByKeywords(buildId, index, type, 1, defaultKeywords, tag, executeCount)
            } else
                doQueryInitLogs(buildId, index, type, !isAnalysis, defaultKeywords, tag, executeCount)
        }

        val keywords = keywordsStr.split(",".toRegex())
            .dropLastWhile { it.isEmpty() }
            .stream()
            .filter { k -> k.isNotBlank() }
            .collect(Collectors.toList())

        val result = doQueryByKeywords(buildId, index, type, 1, keywords, tag, executeCount)
        logger.info("query init logs for build($buildId): size-${result.logs.size} size-${result.status}")
        return result
    }

    private fun doQueryInitLogs(
        buildId: String,
        index: String,
        type: String,
        wholeQuery: Boolean,
        keywords: List<String>,
        tag: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val queryLogs = getLogStatus(buildId, tag, executeCount)

        try {
            val logs = getLogs(index, type, keywords, wholeQuery, tag, executeCount)
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
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

    open fun queryInitLogsPage(
        buildId: String,
        index: String,
        type: String,
        wholeQuery: Boolean,
        keywords: List<String>,
        tag: String? = null,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): QueryLogs {
        val queryLogs = getLogStatus(buildId, tag, executeCount)

        try {
            val logs = getLogsByPage(index, type, tag, executeCount, page, pageSize)
            queryLogs.logs.addAll(logs)
            if (logs.isEmpty()) queryLogs.status = LogStatus.EMPTY
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
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

    private fun doQueryByKeywords(
        buildId: String,
        index: String,
        type: String,
        start: Long,
        keywords: List<String>,
        tag: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val initLogs = getLogStatus(buildId, tag, executeCount)

        try {
            val size = getLogSize(index, type, tag)
            if (size == 0L) {
                return initLogs
            }

            val logs = getLogs(index, type, keywords, false, tag, executeCount)
            initLogs.logs.addAll(logs)

            //
            if (logs.isEmpty()) {
                initLogs.logs.add(
                    genLogMsgThereIsMore(
                        "soda_more",
                        java.lang.Long.MIN_VALUE,
                        size - start + 1,
                        start,
                        size,
                        tag,
                        executeCount
                    )
                )
            }
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

    open fun queryMoreLogsBetweenLines(
        buildId: String,
        index: String,
        type: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        tag: String? = null,
        executeCount: Int?
    ): QueryLogs {
        val logs = mutableListOf<LogLine>()
        val queryLogs = getLogStatus(buildId, tag, executeCount)

        try {
            val searchResponse = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(
                    addExecuteCountQuery(
                        addTagQuery(QueryBuilders.rangeQuery("lineNo").gte(start).lte(end), tag), executeCount
                    )
                )
                .highlighter(
                    HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                        .field("message").fragmentSize(100000)
                )
                .setSize(num)
                .addDocValueField("lineNo")
                .addDocValueField("timestamp")
                //                    .addDocValueField("message")
                .addSort("lineNo", if (fromStart) SortOrder.ASC else SortOrder.DESC)
                .get()
            searchResponse.hits.forEach { searchHitFields ->
                val sourceMap = searchHitFields.source
                val logLine = LogLine(
                    sourceMap["lineNo"].toString().toLong(),
                    sourceMap["timestamp"].toString().toLong(),
                    sourceMap["message"].toString(),
                    Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    sourceMap["tag"].toString()
                )
                logs.add(logLine)
            }
            if (!fromStart) {
                logs.reverse()
            }
            queryLogs.logs.addAll(logs)
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
            logger.error(
                "Query more logs between lines failed because of " +
                    "IndexNotFoundException. buildId: $buildId", ex
            )
        }

        return queryLogs
    }

    open fun queryMoreLogsAfterLine(
        buildId: String,
        index: String,
        type: String,
        start: Long,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String? = null,
        executeCount: Int?
    ): QueryLogs {
        if (keywordsStr == null || keywordsStr.isBlank()) {
            return if (isAnalysis) {
                doQueryByKeywords(buildId, index, type, start, defaultKeywords, tag, executeCount)
            } else
                doQueryMoreLogsAfterLine(buildId, index, type, start, !isAnalysis, defaultKeywords, tag, executeCount)
        }
        val keywords = keywordsStr.split(",".toRegex())
            .dropLastWhile { it.isEmpty() }
            .stream()
            .filter { k -> k.isNotBlank() }
            .collect(Collectors.toList())

        return doQueryByKeywords(buildId, index, type, start, keywords, tag, executeCount)
    }

    /**
     *
     * @param buildId
     * @param index
     * @param type
     * @param start
     * @param wholeQuery 是否查找所有关键
     * @param keywords
     * @return
     */
    private fun doQueryMoreLogsAfterLine(
        buildId: String,
        index: String,
        type: String,
        start: Long,
        wholeQuery: Boolean,
        keywords: List<String>,
        tag: String?,
        executeCount: Int?
    ): QueryLogs {
        logger.info("more logs params: $buildId, $index, $type, $start, $wholeQuery, $keywords, $tag, $executeCount")

        val logs = ArrayList<LogLine>()
        val moreLogs = getLogStatus(buildId, tag, executeCount)
        logger.info("more logs status: $moreLogs")

        try {
            val multiSearchRequestBuilder = client.prepareMultiSearch()

            if (wholeQuery) {
                val startQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("logType", LogType.START.name))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))
                addExecuteTagQuery(startQuery, tag, executeCount)
                val srbFoldStart = client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(startQuery)
                    .addDocValueField("lineNo")
                    .setSize(100)

                val stopQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("logType", LogType.START.name))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))
                addExecuteTagQuery(stopQuery, tag, executeCount)
                val srbFoldStop = client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(stopQuery)
                    .addDocValueField("lineNo")
                    .setSize(100)

                multiSearchRequestBuilder.add(srbFoldStart).add(srbFoldStop)
            }

            val tempKeywords = if (!keywords.isEmpty()) {
                keywords
            } else {
                defaultKeywords
            }

            for (keyword in tempKeywords) {
                val query = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("message", keyword).operator(Operator.AND))
                    .must(QueryBuilders.rangeQuery("lineNo").from(start))
                addExecuteTagQuery(query, tag, executeCount)
                val srbKeyword = client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(query)
                    .highlighter(
                        HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                            .field("message").fragmentSize(100000)
                    )
                    .addDocValueField("lineNo")
                    .setSize(50)
                multiSearchRequestBuilder.add(srbKeyword)
            }

            val timeStart = System.currentTimeMillis()

            val multiSearchResponse = multiSearchRequestBuilder.get()
            moreLogs.timeUsed = System.currentTimeMillis() - timeStart
            val lineNoSet = TreeSet<Long>()
            val highlights = HashMap<Long, String>()

            multiSearchResponse.responses
                .map { it.response }
                .filter { it != null && it.hits != null }
                .forEach { response ->
                    response.hits.forEach {
                        // 对 No such process 作特殊处理
                        val message = it.source["message"].toString()
                        if (!message.isBlank() && !message.contains("No such process")) {
                            val ln = it.getField("lineNo").getValue<Long>()
                            lineNoSet.add(ln)
                            if (!it.highlightFields.isEmpty()) {
                                highlights.put(
                                    ln,
                                    it.highlightFields["message"]!!.fragments[0].toString()
                                )
                            }
                        }
                    }
                }
            logger.info("$type more logs lineNoSet: $lineNoSet")

            // 开始处理需要返回的行号
            val lineRanges = parseToLineRangesGetAfterLines(
                lineNoSet, Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
            val lines = parseToLineNos(lineRanges)

            logger.info("$type more logs lineRanges: $lineRanges")
            val searchResponse = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(
                    addExecuteCountQuery(
                        addTagQuery(QueryBuilders.rangeQuery("lineNo").gte(start), tag), executeCount
                    )
                )
                .setSize(Constants.MAX_LINES)
                .addDocValueField("lineNo")
                .addDocValueField("timestamp")
                //                    .addDocValueField("message")
                .addSort("lineNo", SortOrder.ASC)
                .get()

            // 简单处理，如果得到的数据量与请求的数据量一样，认为还未 finished
//            if (searchResponse.hits.getTotalHits() == Constants.MAX_LINES.toLong()) {
//                moreLogs.finished = false
//            }

            var lastLineNo = -1L
            for (searchHitFields in searchResponse.hits) {
                val sourceMap = searchHitFields.source
                val lineNo = java.lang.Long.parseLong(sourceMap["lineNo"].toString())
                if (lastLineNo != -1L && lineNo - lastLineNo > 1L) {
                    break
                }
                lastLineNo = lineNo

                val logLine = LogLine(
                    lineNo,
                    sourceMap["timestamp"].toString().toLong(),
                    if (highlights.containsKey(lineNo)) {
                        highlights[lineNo] ?: ""
                    } else {
                        sourceMap["message"].toString()
                    },
                    if (lines.contains(lineNo)) {
                        Constants.DEFAULT_PRIORITY_NOT_DELETED
                    } else {
                        0
                    },
                    sourceMap["tag"].toString()
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

    /**
     * @return 若不存在，返回 null
     */
    private fun getLogStatus(
        buildId: String,
        tag: String? = null,
        executeCount: Int?,
        retry: Boolean = true
    ): QueryLogs {

        var id = buildId
        if (!tag.isNullOrBlank()) id += "-$tag"
        if (executeCount != null) id += "-$executeCount"

        val query = QueryBuilders.boolQuery().should(QueryBuilders.idsQuery().addIds(buildId))
            .should(QueryBuilders.idsQuery().addIds(id))
        try {
            client.prepareSearch(Constants.INDEX_LOG_STATUS)
                .setTypes(Constants.TYPE_LOG_STATUS)
                .setQuery(query)
                .setSize(2)
                .get()
                .hits.forEach { searchHit ->
                val source = searchHit.source
                val finish = source["finished"] as Boolean
                // 流水线或插件任意一个执行完成即完成
                if (finish) return QueryLogs(buildId, finish)
            }
        } catch (e: org.elasticsearch.index.IndexNotFoundException) {
            logger.warn(
                "[$buildId|$tag|$executeCount] The index [${Constants.INDEX_LOG_STATUS}] is not exist, retry=$retry",
                e
            )
            if (retry) {
                createLogStatusIndexAndType()
                return getLogStatus(buildId, tag, executeCount, false)
            }
            throw e
        }
        return QueryLogs(buildId, false)
    }

    open fun preCreateIndices(numDays: Int): Int {
        val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
        val typeName = "typeForResetting"
        var numCreated = 0
        (0 until numDays).forEach {
            val date = LocalDate.now().plusDays(it.toLong())
            val indexName = "log-" + formatter.format(date)
            if (!isExistIndex(indexName)) {
                val result = createIndexAndType(indexName, typeName)
                if (result) numCreated++
            }
            for (i in 0..23) {
                val hour = getTensDigit(i)
                val indexNameHour = "$indexName-$hour"
                if (!isExistIndex(indexName)) {
                    val result = createIndexAndType(indexNameHour, typeName)
                    if (result) numCreated++
                }
            }
            if (!isExistIndex(indexName)) {
                val result = createIndexAndType(indexName, typeName)
                if (result) numCreated++
            }
        }
        logger.info("preCreateIndices num: $numCreated")
        return numCreated
    }

    open fun getTensDigit(num: Int): String {
        return if (num < 10) {
            "0" + num.toString()
        } else {
            num.toString()
        }
    }

    open fun createLogStatusIndex(): Boolean {
        if (isExistIndex(Constants.INDEX_LOG_STATUS)) {
            return false
        }

        return createLogStatusIndexAndType()
    }

    open fun downloadLogs(pipelineId: String, buildId: String, tag: String, executeCount: Int?): Response {
        val index = indexService.queryIndex(buildId)

        val query = addExecuteCountQuery(
            addTagQuery(QueryBuilders.matchQuery("logType", LogType.LOG.name), tag), executeCount
        )

        var scrollResp = client.prepareSearch(index)
            .setTypes(buildId)
            .setQuery(query)
            .addDocValueField("lineNo")
            .addDocValueField("timestamp")
            .addSort("lineNo", SortOrder.ASC)
            .setScroll(TimeValue(1000 * 32))
            .setSize(4000)
            .get()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        // 一边读一边流式下载
        val fileStream = StreamingOutput { output ->
            do {
                val sb = StringBuilder()
                scrollResp.hits.hits.forEach { searchHit ->
                    val sourceMap = searchHit.source

                    val logLine = LogLine(
                        sourceMap["lineNo"].toString().toLong(),
                        sourceMap["timestamp"].toString().toLong(),
                        sourceMap["message"].toString().removePrefix("\u001b[31m")
                            .removePrefix("\u001b[1m").replace("\u001B[m", "")
                            .removeSuffix("\u001b[m"),
                        Constants.DEFAULT_PRIORITY_NOT_DELETED
                    )
                    val dateTime = sdf.format(Date(logLine.timestamp))
                    val str = "[${logLine.lineNo}] - $dateTime : ${logLine.message}" + System.lineSeparator()
                    sb.append(str)
                }
                output.write(sb.toString().toByteArray())
                output.flush()
                scrollResp = client.prepareSearchScroll(scrollResp.scrollId)
                    .setScroll(TimeValue(1000 * 32)).execute().actionGet()
            } while (scrollResp.hits.hits.isNotEmpty())
        }

        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $pipelineId-$buildId-log.txt")
            .header("Cache-Control", "no-cache")
            .build()
    }

    open fun getEndLogs(
        pipelineId: String,
        buildId: String,
        tag: String,
        executeCount: Int?,
        size: Int
    ): EndPageQueryLogs {
        val queryLogs = EndPageQueryLogs(buildId)
        try {
            return doGetEndLogs(buildId, tag, executeCount, size)
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
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

    private fun startLog(buildId: String): Boolean {
        logger.info("[$buildId] Start logs")
        val indexAndType = indexService.parseIndexAndType(buildId)
        return if (isExistIndex(indexAndType.left)) {
            updateIndexAndType(indexAndType.left, indexAndType.right)
        } else {
            createIndexAndType(indexAndType.left, indexAndType.right)
        }
    }

    private fun addLineNo(buildId: String, logMessages: List<LogMessage>): List<LogMessageWithLineNo> {
        val logIndex = indexService.getAndAddLineNum(buildId, logMessages.size.toLong())
        if (logIndex == null) {
            logger.error("Got null logIndex from indexService, buildId: $buildId")
            return emptyList()
        }

        var startLineNum = logIndex.lastLineNum
        return logMessages.map {
            val timestamp = if (it.timestamp == 0L) {
                System.currentTimeMillis()
            } else {
                it.timestamp
            }
            LogMessageWithLineNo(
                it.tag,
                it.message,
                timestamp,
                it.logType,
                startLineNum++,
                it.executeCount
            )
        }
    }

    private fun doAddMultiLines(logMessages: List<LogMessageWithLineNo>, buildId: String): Int {
        val logIndex = indexService.getLineNum(buildId)
        if (logIndex == null) {
            logger.error("Got null logIndex from indexService, buildId: $buildId")
            return 0
        }

        var lines = 0
        val bulkRequestBuilder = client.prepareBulk()
        for (i in logMessages.indices) {
            val logMessage = logMessages[i]

            val indexRequestBuilder = indexRequestBuilder(
                logMessage,
                logIndex.indexName, logIndex.getTypeName()
            )
            if (indexRequestBuilder != null) {
                bulkRequestBuilder.add(indexRequestBuilder)
                lines++
            }
        }
        try {
            // 注意，在 bulk 下，TypeMissingException 不会抛出，需要判断 bulkResponse.hasFailures() 抛出
            val bulkResponse = bulkRequestBuilder.get()
            return if (bulkResponse.hasFailures()) {
                throw Exception(bulkResponse.buildFailureMessage())
            } else {
                lines
            }
        } catch (ex: Exception) {
            val exString = ex.toString()
            if (exString.contains("TypeMissingException")) {
                logger.error(
                    "[$buildId] Add bulk lines failed because of TypeMissingException," +
                        " attempting to add index. [$logMessages]", ex
                )

                startLog(buildId)

                val bulkResponse = bulkRequestBuilder.get()
                return if (bulkResponse.hasFailures()) {
                    logger.error(bulkResponse.buildFailureMessage())
                    0
                } else {
                    lines
                }
            } else {
                logger.error("[$buildId] Add bulk lines failed because of unknown Exception. [$logMessages]", ex)
                return 0
            }
        }
    }

    private fun genLogMsgThereIsMore(
        tagPrefix: String,
        timeStamp: Long,
        numMore: Long,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): LogLine {
        return LogLine(
            -1L,
            timeStamp,
            "$tagPrefix:num=$numMore,start=$start,end=$end",
            Constants.DEFAULT_PRIORITY_NOT_DELETED,
            tag ?: "",
            executeCount
        )
    }

    private fun getLogsByPage(
        index: String, type: String, tag: String?, executeCount: Int?,
        page: Int, pageSize: Int
    ): List<LogLine> {

        val boolQuery = QueryBuilders.boolQuery()
        if (page != -1 && pageSize != -1) {
            val endLineNo = pageSize * page
            val beginLineNo = endLineNo - pageSize + 1
            boolQuery.must(QueryBuilders.rangeQuery("lineNo").gte(beginLineNo).lte(endLineNo))
        }

        val query = addExecuteCountQuery(addTagQuery(boolQuery, tag), executeCount)

        val result = mutableListOf<LogLine>()

        var scrollResp = client.prepareSearch(index)
            .setTypes(type)
            .setQuery(query)
            .addDocValueField("lineNo")
            .addDocValueField("timestamp")
            .addSort("lineNo", SortOrder.ASC)
            .setScroll(TimeValue(1000 * 8))
            .setSize(pageSize)
            .get()
        do {
            scrollResp.hits.hits.forEach { searchHit ->
                val sourceMap = searchHit.source
                val logType = sourceMap["logType"].toString()
                val logLine = LogLine(
                    sourceMap["lineNo"].toString().toLong(),
                    sourceMap["timestamp"].toString().toLong(),
                    if (logType == LogType.LOG.name) sourceMap["message"].toString() else "",
                    Constants.DEFAULT_PRIORITY_NOT_DELETED
                )
                result.add(logLine)
            }
            scrollResp = client.prepareSearchScroll(scrollResp.scrollId)
                .setScroll(TimeValue(100)).execute().actionGet()
        } while (scrollResp.hits.hits.isNotEmpty())

        return result
    }

    open fun getLogSize(index: String, type: String, tag: String? = null, executeCount: Int? = null): Long {
        val query = addExecuteCountQuery(addTagQuery(QueryBuilders.matchAllQuery(), tag), executeCount)
        logger.info("[$index|$type|$tag|$executeCount] Get the log size - ($query)")
        val searchResponse = client.prepareSearch(index)
            .setTypes(type)
            .setQuery(query)
            .setSize(0)
            .get()
        return searchResponse.hits.getTotalHits()
    }

    private fun addExecuteTagQuery(query: BoolQueryBuilder, tag: String?, executeCount: Int?) {
        if (!tag.isNullOrBlank()) {
            query.must(QueryBuilders.matchQuery("tag", tag).operator(Operator.AND))
        }
        if (executeCount != null) {
            query.must(QueryBuilders.matchQuery("executeCount", executeCount).operator(Operator.AND))
        }
    }

    private fun addTagQuery(queryBuilder: QueryBuilder, tag: String?): QueryBuilder {
        val query = QueryBuilders.boolQuery()
        query.must(queryBuilder)
        if (!tag.isNullOrBlank()) {
            query.must(QueryBuilders.matchQuery("tag", tag).operator(Operator.AND))
        }
        return query
    }

    private fun addExecuteCountQuery(query: QueryBuilder, executeCount: Int?): QueryBuilder {
        // 目前只支持BoolQueryBuilder
        if (executeCount != null && query is BoolQueryBuilder) {
            query.must(QueryBuilders.matchQuery("executeCount", executeCount).operator(Operator.AND))
        }
        return query
    }

    private fun getLogs(
        index: String, type: String, keywords: List<String>,
        wholeQuery: Boolean, tag: String?, executeCount: Int?
    ): List<LogLine> {
        logger.info(
            "log params for type($type): index: $index, keywords: $keywords, " +
                "wholeQuery: $wholeQuery, tag: $tag, executeCount: $executeCount"
        )

        val size = getLogSize(index, type, tag, executeCount)
        if (size == 0L) {
            return listOf()
        }

        val multiSearchRequestBuilder = client.prepareMultiSearch()

        val logRange = if (tag.isNullOrBlank()) Pair(1L, size)
        else getLogRange(index, type, tag!!, executeCount, size)

        logger.info("log range for $type: (${logRange.first}, ${logRange.second}), size: $size")

        val query = QueryBuilders.boolQuery()

        var startTime = System.currentTimeMillis()

        // 高亮关键字
        // 传了tag就认为不是全量查询
        if (wholeQuery && tag.isNullOrBlank()) {

            val srbFoldStart = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(QueryBuilders.matchQuery("logType", LogType.START.name))
                .addDocValueField("lineNo")
                .setSize(100)
            val srbFoldStop = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(QueryBuilders.prefixQuery("logType", LogType.END.name))
                .addDocValueField("lineNo")
                .setSize(100)

            multiSearchRequestBuilder.add(srbFoldStart).add(srbFoldStop)
        }

        addExecuteTagQuery(query, tag, executeCount)

        keywords.forEach {
            val srbKeyword = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(
                    query
                        .must(QueryBuilders.matchQuery("message", it).operator(Operator.AND))
                        .must(QueryBuilders.rangeQuery("lineNo").gte(logRange.first))
                )
                .highlighter(
                    HighlightBuilder().preTags("\u001b[31m").postTags("\u001b[0m")
                        .field("message").fragmentSize(100000)
                )
                .addDocValueField("lineNo")
                .setSize(50)
            multiSearchRequestBuilder.add(srbKeyword)
        }

        val lineNoSet = TreeSet<Long>()

        val highlights = HashMap<Long, String>()
        val multiSearchResponse = multiSearchRequestBuilder.get()
        multiSearchResponse.responses
            .map { it.response }
            .filter { it != null && it.hits != null }
            .forEach { response ->
                response.hits.forEach {
                    // 对 No such process 作特殊处理
                    val message = it.source["message"].toString()
                    if (!message.isBlank() && !message.contains("No such process")) {
                        val ln = it.getField("lineNo").getValue<Long>()
                        lineNoSet.add(ln)
                        if (!it.highlightFields.isEmpty()) {
                            highlights.put(
                                ln,
                                it.highlightFields["message"]!!.fragments[0].toString()
                            )
                        }
                    }
                }
            }

        logger.info("step1 time cost($type): ${System.currentTimeMillis() - startTime}")
        logger.info("$type line no set: $lineNoSet")
        logger.info("$type highlights map: $highlights")
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
                lineNoSet, Constants.NUM_LINES_START.toLong(), Constants.NUM_LINES_END.toLong(),
                Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
        } else {
            parseToLineRangesGetInitLines(
                lineNoSet, Constants.NUM_LINES_AROUND_TAGS.toLong(), Constants.NUM_LINES_AROUND_TAGS.toLong(),
                Constants.NUM_LINES_AROUND_TAGS.toLong()
            )
        }

        val logs = mutableListOf<LogLine>()
        logger.info("$type logs lineRanges: $lineRanges")
        if (!lineRanges.isEmpty()) {
            val boolQueryBuilder = QueryBuilders.boolQuery()
            addExecuteTagQuery(boolQueryBuilder, tag, executeCount)

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

            val response = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(boolQueryBuilder)
                .setSize(Constants.MAX_LINES)
                .addDocValueField("lineNo")
                .addDocValueField("timestamp")
                //                        .addDocValueField("message")
                .addSort("lineNo", SortOrder.ASC)
                .get()
            response.hits.forEach { searchHitFields ->
                val sourceMap = searchHitFields.source
                val ln = sourceMap["lineNo"].toString().toLong()
                val t = sourceMap["tag"]?.toString() ?: ""
                val logLine = LogLine(
                    ln,
                    sourceMap["timestamp"].toString().toLong(),
                    if (highlights.containsKey(ln)) {
                        highlights[ln] ?: ""
                    } else {
                        sourceMap["message"].toString()
                    },
                    Constants.DEFAULT_PRIORITY_NOT_DELETED,
                    t,
                    sourceMap["executeCount"]?.toString()?.toInt() ?: 1
                )
                logs.add(logLine)
            }
            val numLogs = logs.size

            logger.info("step2 time cost($type): ${System.currentTimeMillis() - startTime}")
            startTime = System.currentTimeMillis()

            // 添加上线查看更多的标志日志
            if (numLogs > 0) {
                if (logs[0].lineNo > logRange.first) {
                    logs.add(
                        0, genLogMsgThereIsMore(
                            "soda_more",
                            java.lang.Long.MIN_VALUE,
                            logs[0].lineNo - logRange.first,
                            logRange.first,
                            logs[0].lineNo - 1,
                            tag,
                            executeCount
                        )
                    )
                }
                if (logs[logs.size - 1].lineNo < logRange.second) {
                    logs.add(
                        genLogMsgThereIsMore(
                            "soda_more",
                            java.lang.Long.MAX_VALUE,
                            logRange.second - logs[logs.size - 1].lineNo,
                            logs[logs.size - 1].lineNo + 1,
                            logRange.second,
                            tag,
                            executeCount
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
                        i, genLogMsgThereIsMore(
                            "soda_more",
                            timestamp,
                            lineNo1 - lineNo - 1,
                            lineNo + 1,
                            lineNo1 - 1,
                            tag,
                            executeCount
                        )
                    )
                }
            }
        }
        logger.info("step3 time cost($type): ${System.currentTimeMillis() - startTime}")

        return logs
    }

    private fun getLogRange(
        index: String, type: String, tag: String,
        executeCount: Int?, size: Long
    ): Pair<Long, Long> {
        val query = QueryBuilders.boolQuery()
            .should(QueryBuilders.matchQuery("logType", LogType.START.name))
            .should(QueryBuilders.matchQuery("logType", LogType.END.name))

        val q = addExecuteCountQuery(addTagQuery(query, tag), executeCount)

        logger.info("[$index|$type|$tag|$executeCount|$size] Get log range with query ($q)")

        val hits = client.prepareSearch(index)
            .setTypes(type)
            .setQuery(q)
            .addDocValueField("lineNo")
            .setSize(200)
            .addSort("lineNo", SortOrder.ASC)
            .get()
            .hits

        logger.info("hits 0 for build($type) with response (${hits.hits.size})")

        if (hits.totalHits == 0L) return Pair(0, 0)

        logger.info("hits one for build($type)>>> ${hits.getAt(0).source}")

        val beginIndex = hits.getAt(0).source["lineNo"].toString().toLong()

        if (hits.totalHits == 1L) return Pair(beginIndex, beginIndex + size - 1)

        logger.info("hits two for build($type)>>> ${hits.getAt(1).source}")

        val endIndex = hits.getAt(1).source["lineNo"].toString().toLong()

        return Pair(beginIndex, endIndex)
    }

    private fun indexRequestBuilder(
        logMessage: LogMessageWithLineNo, index: String?,
        type: String?
    ): IndexRequestBuilder? {
        val builder: XContentBuilder
        try {
            builder = XContentFactory.jsonBuilder()
                .startObject()
                .field("lineNo", logMessage.lineNo)
                .field("message", logMessage.message)
                .field("timestamp", logMessage.timestamp)
                .field("tag", logMessage.tag)
                .field("logType", logMessage.logType.name)
                .field("executeCount", logMessage.executeCount)
                .endObject()
        } catch (e: IOException) {
            logger.error("Convert logMessage to es document failure", e)
            return null
        }

        return client.prepareIndex(index, type)
            .setCreate(false) // 不强制创建索引
            .setSource(builder)
    }

    private fun createTypeMapping(): XContentBuilder {
        return XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("timestamp").field("type", "long").endObject()
            .startObject("lineNo").field("type", "long").endObject()
            .startObject("tag").field("type", "keyword").endObject()
            .startObject("executeCount").field("type", "keyword").endObject()
            .startObject("logType").field("type", "text").endObject()
            .startObject("message").field("type", "text")
            .field("analyzer", "standard")
            //                        .field("fielddata", true)
            .endObject()
            .endObject()
            .endObject()
    }

    private fun createSodaLogTypeMapping(): XContentBuilder {
        return XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("buildId").field("type", "keyword").endObject()
            .startObject("createTime").field("type", "long").endObject()
            .startObject("finished").field("type", "boolean").endObject()
            .startObject("updateTime").field("type", "long").endObject()
            .startObject("tag").field("type", "keyword").endObject()
            .startObject("executeCount").field("type", "keyword").endObject()
            .endObject()
            .endObject()
    }

    private fun isExistIndex(index: String): Boolean {
        val response = client.admin()
            .indices()
            .prepareExists(index)
            .get()
        return response.isExists
    }

    private fun createIndexAndType(index: String, type: String): Boolean {
        logger.info("[$index|$type] Create index and type")
        var success = false
        val startEpoch = System.currentTimeMillis()
        return try {
            logger.info("[$index|$type] Start to create the index and type")
            val response = client.admin()
                .indices()
                .prepareCreate(index)
                .setSettings(
                    Settings.builder()
                        .put("index.number_of_shards", 6)
                        .put("index.number_of_replicas", 1)
                        .put("index.refresh_interval", "3s")
                        .put("index.queries.cache.enabled", false)
                )
                .addMapping(type, createTypeMapping())
                .get()
            success = true
            response.isShardsAcked
        } catch (e: IOException) {
            logger.error(String.format("Create index %s type %s failure", index, type), e)
            false
        } finally {
            createIndexBean.execute(System.currentTimeMillis() - startEpoch, success)
        }
    }

    private fun createLogStatusIndexAndType() = client.admin()
        .indices()
        .prepareCreate(Constants.INDEX_LOG_STATUS)
        .setSettings(
            Settings.builder()
                .put("index.number_of_shards", 6)
                .put("index.number_of_replicas", 1)
                .put("index.refresh_interval", "3s")
                .put("index.queries.cache.enabled", false)
        )
        .addMapping(Constants.TYPE_LOG_STATUS, createSodaLogTypeMapping())
        .get().isShardsAcked

    private fun updateIndexAndType(index: String, type: String): Boolean {
        logger.info("[$index|$type] Update index and type")
        var success = false
        val startEpoch = System.currentTimeMillis()
        return try {
            logger.info("[$index|$type] Start to update the index and type")
            val response = client.admin()
                .indices()
                .preparePutMapping(index)
                .setType(type)
                .setSource(createTypeMapping())
                .get()
            success = true
            response.isAcknowledged
        } catch (e: Exception) {
            logger.error(String.format("Update index %s type %s failure", index, type), e)
            false
        } finally {
            updateIndexBean.execute(System.currentTimeMillis() - startEpoch, success)
        }
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

    private fun refreshIndex(buildId: String): Boolean {
        val indexAndType = indexService.parseIndexAndType(buildId)

        return try {
            client.admin().indices()
                .prepareRefresh(indexAndType.left)
                .get().failedShards == 0
        } catch (ex: org.elasticsearch.index.IndexNotFoundException) {
            logger.error("Refresh index failed because of IndexNotFoundException, buildId: $buildId")
            false
        }
    }

    private fun doGetEndLogs(buildId: String, tag: String, executeCount: Int?, size: Int): EndPageQueryLogs {
        val beginTime = System.currentTimeMillis()

        val index = indexService.queryIndex(buildId)

        val query = addExecuteCountQuery(addTagQuery(QueryBuilders.matchAllQuery(), tag), executeCount)

        val scrollResp = client.prepareSearch(index)
            .setTypes(buildId)
            .setQuery(query)
            .addDocValueField("lineNo")
            .addDocValueField("timestamp")
            .addSort("timestamp", SortOrder.DESC)
            .setScroll(TimeValue(1000 * 32))
            .setSize(size)
            .get()
        val logs = mutableListOf<LogLine>()
        scrollResp.hits.hits.forEach { searchHit ->
            val sourceMap = searchHit.source

            val logLine = LogLine(
                sourceMap["lineNo"].toString().toLong(),
                sourceMap["timestamp"].toString().toLong(),
                sourceMap["message"].toString(),
                Constants.DEFAULT_PRIORITY_NOT_DELETED,
                sourceMap["tag"].toString(),
                sourceMap["executeCount"]?.toString()?.toInt() ?: 1
            )
            logs.add(logLine)
        }
        return EndPageQueryLogs(
            buildId, logs.lastOrNull()?.lineNo ?: 0,
            logs.firstOrNull()?.lineNo ?: 0, logs, System.currentTimeMillis() - beginTime
        )
    }

    private fun genRedisKey(buildId: String) = "LOG:index:lock:key:$buildId"
}
