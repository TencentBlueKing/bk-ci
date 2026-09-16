/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.dao

import com.tencent.devops.common.es.client.LogClient
import com.tencent.devops.common.log.constant.LogMessageCode.LOG_INDEX_HAS_BEEN_CLEANED
import com.tencent.devops.common.log.pojo.LogPanelLine
import com.tencent.devops.common.log.pojo.QueryLogPanel
import com.tencent.devops.common.log.pojo.enums.LogPanelLevel
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.util.LogPanelQueryBuilder
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Repository
import java.io.IOException

@Repository
@ConditionalOnBean(LogClient::class)
class LogPanelEsDao(
    private val logClient: LogClient,
    private val indexService: IndexService,
    private val logStatusService: LogStatusService,
    private val logTagService: LogTagService
) {
    enum class Direction { LATEST, BEFORE, AFTER }

    fun query(
        buildId: String,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        levels: List<LogPanelLevel>,
        pageSize: Int,
        direction: Direction,
        cursorLineNo: Long?,
        sinceTimestamp: Long? = null,
        lookbackMs: Long? = null
    ): QueryLogPanel {
        val finished = logStatusService.isFinish(
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            jobId = null,
            stepId = null
        )
        val subTags = tag?.let { logTagService.getSubTags(buildId, it) }
        val levelNames = levels.map { it.name }
        val indexName = indexService.getBuildIndexName(buildId)
        if (indexName.isNullOrBlank() || !indexExists(buildId, indexName)) {
            return QueryLogPanel(
                buildId = buildId,
                finished = true,
                cleaned = true,
                status = LogStatus.CLEAN.status,
                message = LogPanelQueryBuilder.cleanedMessage(LogStatus.CLEAN.status)
                    ?: I18nUtil.getCodeLanMessage(LOG_INDEX_HAS_BEEN_CLEANED),
                subTags = subTags,
                levels = levelNames
            )
        }
        return try {
            search(
                buildId = buildId,
                indexName = indexName,
                tag = tag,
                subTag = subTag,
                containerHashId = containerHashId,
                executeCount = executeCount,
                levels = levels,
                pageSize = pageSize,
                direction = direction,
                cursorLineNo = cursorLineNo,
                sinceTimestamp = sinceTimestamp,
                lookbackMs = lookbackMs,
                finished = finished,
                subTags = subTags
            )
        } catch (e: ElasticsearchStatusException) {
            handleEsStatus(buildId, finished, subTags, levelNames, e)
        } catch (ignore: Throwable) {
            logger.warn("Log panel query failed. buildId=$buildId", ignore)
            QueryLogPanel(
                buildId = buildId,
                finished = true,
                cleaned = false,
                status = LogStatus.FAIL.status,
                message = ignore.message,
                subTags = subTags,
                levels = levelNames
            )
        }
    }

    private fun search(
        buildId: String,
        indexName: String,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        levels: List<LogPanelLevel>,
        pageSize: Int,
        direction: Direction,
        cursorLineNo: Long?,
        sinceTimestamp: Long?,
        lookbackMs: Long?,
        finished: Boolean,
        subTags: List<String>?
    ): QueryLogPanel {
        val filter = baseFilter(buildId, tag, subTag, containerHashId, executeCount, levels)
        when (direction) {
            Direction.BEFORE -> filter.must(QueryBuilders.rangeQuery("lineNo").lt(cursorLineNo ?: 0))
            Direction.AFTER -> filter.must(QueryBuilders.rangeQuery("lineNo").gt(cursorLineNo ?: 0))
            Direction.LATEST -> Unit
        }
        val desc = direction != Direction.AFTER
        val (lines, total) = executeSearch(buildId, indexName, filter, pageSize, desc)
        var merged = lines
        if (direction == Direction.AFTER) {
            val lookback = LogPanelQueryBuilder.resolveAfterLookback(
                cursorLineNo = cursorLineNo ?: 0L,
                sinceTimestamp = sinceTimestamp,
                lookbackMs = lookbackMs
            )
            if (lookback.enabled) {
                val backfillQuery = baseFilter(buildId, tag, subTag, containerHashId, executeCount, levels)
                backfillQuery.must(QueryBuilders.rangeQuery("lineNo").lte(lookback.cursorLineNo))
                backfillQuery.must(QueryBuilders.rangeQuery("timestamp").gte(lookback.backfillFromTimestamp))
                val (backfill, _) = executeSearch(buildId, indexName, backfillQuery, pageSize, desc = false)
                merged = LogPanelQueryBuilder.mergeByLineNo(backfill + lines)
            }
        }
        val moreInDir = total > lines.size
        val startLineNo = merged.firstOrNull()?.lineNo
        val endLineNo = merged.maxOfOrNull { it.lineNo }
        val hasBefore = when (direction) {
            Direction.AFTER -> true
            Direction.BEFORE, Direction.LATEST -> moreInDir
        } && merged.isNotEmpty()
        val hasAfter = when (direction) {
            Direction.BEFORE -> true
            Direction.AFTER -> moreInDir || !finished
            Direction.LATEST -> !finished
        }
        val empty = merged.isEmpty() && direction == Direction.LATEST
        val matchedTotal = if (direction == Direction.LATEST) total else 0L
        return QueryLogPanel(
            buildId = buildId,
            finished = finished,
            cleaned = false,
            status = if (empty) LogStatus.EMPTY.status else LogStatus.SUCCEED.status,
            subTags = subTags,
            logs = merged,
            startLineNo = startLineNo,
            endLineNo = endLineNo,
            matchedTotal = matchedTotal,
            hasBefore = hasBefore && startLineNo != null,
            hasAfter = hasAfter,
            levels = levels.map { it.name }
        )
    }

    private fun baseFilter(
        buildId: String,
        tag: String?,
        subTag: String?,
        containerHashId: String?,
        executeCount: Int?,
        levels: List<LogPanelLevel>
    ): BoolQueryBuilder {
        val boolQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("buildId", buildId).operator(Operator.AND))
            .must(QueryBuilders.matchQuery("executeCount", executeCount ?: 1).operator(Operator.AND))
        if (!tag.isNullOrBlank()) {
            boolQuery.must(QueryBuilders.matchQuery("tag", tag).operator(Operator.AND))
        }
        if (!subTag.isNullOrBlank()) {
            boolQuery.must(QueryBuilders.matchQuery("subTag", subTag).operator(Operator.AND))
        }
        if (!containerHashId.isNullOrBlank()) {
            boolQuery.must(QueryBuilders.matchQuery("containerHashId", containerHashId).operator(Operator.AND))
        }
        if (!LogPanelQueryBuilder.includeAllTypes(levels)) {
            boolQuery.must(QueryBuilders.termsQuery("logType", LogPanelQueryBuilder.esLogTypeNames(levels)))
        }
        return boolQuery
    }

    private fun executeSearch(
        buildId: String,
        indexName: String,
        query: BoolQueryBuilder,
        pageSize: Int,
        desc: Boolean
    ): Pair<List<LogPanelLine>, Long> {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val source = SearchSourceBuilder()
            .query(query)
            .docValueField("lineNo")
            .docValueField("timestamp")
            .size(pageSize)
            .trackTotalHits(true)
            .timeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS))
            .sort("timestamp", order)
            .sort("lineNo", order)
        val request = SearchRequest(indexName).preference(routingPreference(buildId)).source(source)
        val client = logClient.hashClient(buildId)
        val response = try {
            client.restClient.search(request, RequestOptions.DEFAULT)
        } catch (ignore: IOException) {
            client.restClient.search(request, RequestOptions.DEFAULT)
        }
        val lines = response.hits.hits.map { hit ->
            val src = hit.sourceAsMap
            LogPanelLine(
                lineNo = src["lineNo"].toString().toLong(),
                timestamp = src["timestamp"].toString().toLong(),
                message = src["message"]?.toString() ?: "",
                level = LogPanelQueryBuilder.levelFromEs(src["logType"]?.toString()),
                tag = src["tag"]?.toString() ?: "",
                subTag = src["subTag"]?.toString() ?: "",
                executeCount = src["executeCount"]?.toString()?.toInt() ?: 1
            )
        }.toMutableList()
        if (desc) lines.reverse()
        val total = response.hits.totalHits?.value ?: lines.size.toLong()
        return lines to total
    }

    private fun handleEsStatus(
        buildId: String,
        finished: Boolean,
        subTags: List<String>?,
        levels: List<String>,
        e: ElasticsearchStatusException
    ): QueryLogPanel {
        val closed = e.toString().contains("index_closed_exception")
        val status = if (closed) LogStatus.CLOSED else LogStatus.FAIL
        if (closed) {
            logger.warn("[$buildId] log panel index closed", e)
        } else {
            logger.warn("[$buildId] log panel ES status error", e)
        }
        return QueryLogPanel(
            buildId = buildId,
            finished = true,
            cleaned = closed,
            status = status.status,
            message = LogPanelQueryBuilder.cleanedMessage(status.status) ?: e.message,
            subTags = subTags,
            levels = levels
        )
    }

    private fun indexExists(buildId: String, index: String): Boolean {
        val request = GetIndexRequest(index)
        request.setTimeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS))
        return logClient.hashClient(buildId).restClient.indices().exists(request, RequestOptions.DEFAULT)
    }

    private fun routingPreference(buildId: String): String =
        if (buildId.startsWith("_")) "b_$buildId" else buildId

    companion object {
        private val logger = LoggerFactory.getLogger(LogPanelEsDao::class.java)
        private const val SEARCH_TIMEOUT_SECONDS = 60L
    }
}
