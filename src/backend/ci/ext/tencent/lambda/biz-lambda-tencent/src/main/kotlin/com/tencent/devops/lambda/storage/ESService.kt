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
package com.tencent.devops.lambda.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_OFFSET_LESS_THAN_ZERO
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_ORIGIN_TOO_MANY
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_START_DATE_AFTER_END_DATE
import com.tencent.devops.lambda.es.LambdaESClient
import com.tencent.devops.lambda.pojo.BuildData
import com.tencent.devops.lambda.pojo.BuildResult
import com.tencent.devops.lambda.pojo.BuildResultWithPage
import com.tencent.devops.lambda.pojo.DataType
import com.tencent.devops.lambda.pojo.ElementData
import org.elasticsearch.action.search.MultiSearchResponse
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ESService @Autowired constructor(
    private val lambdaEsClient: LambdaESClient,
    private val redisOperation: RedisOperation,
    private val indexService: IndexService,
    private val objectMapper: ObjectMapper
) {

    fun build(data: BuildData) {
        val index = indexService.getIndex(data.buildId)
        val type = getType(index, DataType.PipelineBuild)
        checkCondition(index, type, buildTypeMapping)
        indexService.updateTime(data.buildId, data.beginTime, data.endTime)
        lambdaEsClient.client.prepareIndex(index, type)
            .setCreate(false)
            .setSource(objectMapper.writeValueAsString(data), XContentType.JSON)
            .get()
    }

    fun buildElement(data: ElementData) {
        val index = indexService.getIndex(data.buildId)
        val type = getType(index, DataType.PipelineBuildElement)
        checkCondition(index, type, buildElementTypeMapping)
        lambdaEsClient.client.prepareIndex(index, type)
            .setCreate(false)
            .setSource(objectMapper.writeValueAsString(data), XContentType.JSON)
            .get()
    }

    fun getBuildResult(
        projectId: String?,
        pipelineId: String?,
        startTime: Long,
        endTime: Long?,
        bgName: String?,
        deptName: String?,
        centerName: String?,
        offset: Int = 0,
        limitOrigin: Int = 10,
        project: String
    ): BuildResultWithPage {
        if (offset < 0) {
            throw InvalidParamException(
                message = "Offset cannot be less than 0, offset=$offset",
                errorCode = ERROR_LAMBDA_OFFSET_LESS_THAN_ZERO,
                params = arrayOf("$offset"))
        }
        if (limitOrigin > 100) {
            throw InvalidParamException(
                message = "Limit cannot be bigger than 100, limitOrigin=$limitOrigin",
                errorCode = ERROR_LAMBDA_ORIGIN_TOO_MANY,
                params = arrayOf("$limitOrigin"))
        }
        val limit = if (limitOrigin <= 0) {
            10
        } else {
            limitOrigin
        }
        val startEpoch = System.currentTimeMillis()
        try {
            val beginTime = getBeginTime(startTime)
            logger.info("[$projectId|$pipelineId|$beginTime|$endTime|$bgName|$deptName|$centerName|$offset|$limit] Start to get the build result")

            val index = getIndex(beginTime, endTime).filter {
                isExistIndex(it)
            }.toSet()
            if (index.isEmpty()) {
                logger.warn("The index is empty")
                return BuildResultWithPage(0, emptyList())
            }
            val response =
                getBuildDataResult(index, projectId, pipelineId, beginTime, endTime, bgName, deptName, centerName, offset, limit)

            val totals = response.hits.totalHits

            val buildIds = response.hits.map {
                it.source["buildId"].toString()
            }.toSet()
            logger.info("Get the buildIds - ($buildIds)")
            val buildDataList = response.hits.map {
                convertBuildData(it.source)
            }
            val buildElementDataList = if (buildIds.isEmpty()) {
                mapOf()
            } else {
                val elementResponse = getBuildElementDataResult(index, buildIds)
                elementResponse.responses.filter {
                    !it.isFailure && it.response.hits.hits.isNotEmpty()
                }.map {
                    val hits = it.response.hits.hits
                    hits[0].source["buildId"] to hits.map { h -> convertBuildElementData(h.source) }.toList()
                }.toMap()
            }
            val buildResult = buildDataList.map {
                BuildResult(
                    buildData = it,
                    elementData = buildElementDataList[it.buildId] ?: emptyList()
                )
            }.toList()

            return BuildResultWithPage(totals, buildResult)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the build result")
        }
    }

    /**
     * 只允许查询近一年的数据
     */
    private fun getBeginTime(beginTime: Long): Long {
        val oneYearBefore = LocalDateTime.now().minusYears(1).timestamp()
        if (beginTime < oneYearBefore) {
            logger.warn("The begin time ($beginTime) is before one year, use one year")
            return oneYearBefore
        }
        return beginTime
    }

    private fun convertBuildData(source: Map<String, Any>): BuildData {
        return BuildData(
            projectId = source["projectId"].toString(),
            pipelineId = source["pipelineId"].toString(),
            buildId = source["buildId"].toString(),
            userId = source["userId"]?.toString() ?: "",
            status = source["status"]?.toString() ?: "",
            trigger = source["trigger"]?.toString() ?: "",
            beginTime = source["beginTime"]?.toString()?.toLong() ?: 0L,
            endTime = source["endTime"]?.toString()?.toLong() ?: 0L,
            buildNum = source["buildNum"]?.toString()?.toInt() ?: 0,
            templateId = source["templateId"]?.toString() ?: "",
            bgName = source["bgName"]?.toString() ?: "",
            deptName = source["deptName"]?.toString() ?: "",
            centerName = source["centerName"]?.toString() ?: "",
            model = source["model"]?.toString() ?: "",
            errorInfoList = source["errorInfoList"]?.toString() ?: ""
        )
    }

    private fun convertBuildElementData(source: Map<String, Any>): ElementData {
        return ElementData(
            projectId = source["projectId"].toString(),
            pipelineId = source["pipelineId"].toString(),
            buildId = source["buildId"].toString(),
            elementId = source["elementId"]?.toString() ?: "",
            elementName = source["elementName"]?.toString() ?: "",
            status = source["status"]?.toString() ?: "",
            beginTime = source["beginTime"]?.toString()?.toLong() ?: 0L,
            endTime = source["endTime"]?.toString()?.toLong() ?: 0L,
            type = source["type"]?.toString() ?: "",
            atomCode = source["atomCode"]?.toString() ?: "",
            errorType = source["errorType"]?.toString() ?: "",
            errorMsg = source["errorMsg"]?.toString() ?: "",
            errorCode = try { source["errorCode"]?.toString()?.toInt() ?: 0 } catch (e: NumberFormatException) { 0 }
        )
    }

    private fun getBuildDataResult(
        index: Set<String>,
        projectId: String?,
        pipelineId: String?,
        beginTime: Long,
        endTime: Long?,
        bgName: String?,
        deptName: String?,
        centerName: String?,
        offset: Int,
        limit: Int
    ) = lambdaEsClient.client.prepareSearch(*index.toTypedArray())
        .setTypes(*index.map { getType(it, DataType.PipelineBuild) }.toSet().toTypedArray())
        .setQuery(getBuildQuery(projectId, pipelineId, bgName, deptName, centerName, beginTime, endTime))
        .setFrom(offset)
        .setSize(limit)
        .get()

    private fun getBuildElementDataResult(
        index: Set<String>,
        buildIds: Set<String>
    ): MultiSearchResponse {
        val multiSearch = lambdaEsClient.client.prepareMultiSearch()
        val types = index.map { getType(it, DataType.PipelineBuildElement) }.toSet()
        buildIds.forEach {
            multiSearch.add(
                lambdaEsClient.client.prepareSearch(*index.toTypedArray())
                    .setTypes(*types.toTypedArray())
                    .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("buildId", it)))
            )
        }
        return multiSearch.get()
    }

    /**
     * @param beginTime timestamp in second
     * @param endTime timestamp in second
     */
    fun getIndex(beginTime: Long, endTime: Long?): Set<String> {
        val end = endTime ?: System.currentTimeMillis() / 1000
        var startDate = Timestamp(beginTime * 1000).toLocalDateTime()
        val endDate = Timestamp(end * 1000).toLocalDateTime()
        if (startDate.isAfter(endDate)) {
            throw InvalidParamException(
                message = "startDate cannot be after endDate, startDate=$startDate, endDate=$endDate",
                errorCode = ERROR_LAMBDA_START_DATE_AFTER_END_DATE,
                params = arrayOf("$startDate", "$endDate"))
        }
        val result = HashSet<String>()
        while (!startDate.isAfter(endDate)) {
            result.add(indexService.getIndexName(startDate))
            startDate = startDate.plusDays(1)
        }
        return result
    }

    private fun getBuildQuery(
        projectId: String?,
        pipelineId: String?,
        bgName: String?,
        deptName: String?,
        centerName: String?,
        beginTime: Long,
        endTime: Long?
    ): BoolQueryBuilder {
        val query = QueryBuilders.boolQuery()
        query.addQueryBuilder("projectId", projectId)
            .addQueryBuilder("pipelineId", pipelineId)
            .addQueryBuilder("bgName", bgName)
            .addQueryBuilder("deptName", deptName)
            .addQueryBuilder("centerName", centerName)
            .must(
                QueryBuilders.rangeQuery("beginTime").gte(beginTime * 1000)
            )
        if (endTime != null) {
            query.must(
                QueryBuilders.rangeQuery("beginTime").lt(endTime * 1000) // 都是以启动构建时间算起
            )
        }
        logger.info("The query is : $query")
        return query
    }

    private fun BoolQueryBuilder.addQueryBuilder(name: String, value: String?): BoolQueryBuilder {
        if (!value.isNullOrBlank()) {
            must(QueryBuilders.matchQuery(name, value).operator(Operator.AND))
        }
        return this
    }

    private fun checkCondition(index: String, type: String, typeMapping: XContentBuilder) {
        if (!checkIndexCreate(index)) {
            createIndex(index)
            indexCache.put(index, true)
        }
        if (!checkTypeCreate(index, type)) {
            createType(index, type, typeMapping)
            typeCache.put(composeTypeCacheKey(index, type), true)
        }
    }

    private fun composeTypeCacheKey(index: String, type: String) = "$index-$type"

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*Index*/, Boolean>()

    /**
     * Type is unique too
     */
    private val typeCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String/*Type*/, Boolean>()

    private fun createIndex(index: String): Boolean {
        logger.info("[$index] Create the pipeline build index")
        return try {
            logger.info("[$index] Start to create the index and type")
            val response = lambdaEsClient.client.admin()
                .indices()
                .prepareCreate(index)
                .setSettings(
                    Settings.builder()
                        .put("index.number_of_shards", 3)
                        .put("index.number_of_replicas", 1)
                        .put("index.refresh_interval", "3s")
                        .put("index.queries.cache.enabled", false)
                )
                .get()
            response.isShardsAcked
        } catch (e: Throwable) {
            logger.error("Create index $index failure", e)
            false
        }
    }

    private fun createType(index: String, type: String, typeMapping: XContentBuilder): Boolean {
        logger.info("[$index|$type] Create the type mapping - ($typeMapping)")
        return try {
            lambdaEsClient.client.admin()
                .indices()
                .preparePutMapping(index)
                .setType(type)
                .setSource(typeMapping)
                .get().isAcknowledged
        } catch (t: Throwable) {
            logger.info("[$index|$type] Fail to create the type mapping", t)
            false
        }
    }

    /**
     * {
     *   "projectId",
     *   "pipelineId",
     *   "buildId",
     *   "userId",
     *   "status",
     *   "trigger",
     *   "beginTime",
     *   "endTime",
     *   "buildNum",
     *   "templateId",
     *   "bgName",
     *   "deptName",
     *   "centerName",
     *   "model"
     * }
     */
    private val buildTypeMapping =
        XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("projectId").field("type", "keyword").endObject()
            .startObject("pipelineId").field("type", "keyword").endObject()
            .startObject("buildId").field("type", "keyword").endObject()
            .startObject("userId").field("type", "keyword").endObject()
            .startObject("status").field("type", "keyword").endObject()
            .startObject("trigger").field("type", "keyword").endObject()
            .startObject("beginTime").field("type", "long").endObject()
            .startObject("endTime").field("type", "long").endObject()
            .startObject("buildNum").field("type", "long").endObject()
            .startObject("templateId").field("type", "keyword").endObject()
            .startObject("bgName").field("type", "keyword").endObject()
            .startObject("deptName").field("type", "keyword").endObject()
            .startObject("centerName").field("type", "keyword").endObject()
            .startObject("model").field("type", "text").field("index", "not_analyzed").endObject()
            .endObject()
            .endObject()

    /**
     * {
     *   "projectId",
     *   "pipelineId",
     *   "buildId",
     *   "elementId",
     *   "elementName",
     *   "status",
     *   "beginTime",
     *   "endTime",
     *   "type",
     *   "atomCode"
     * }
     */
    private val buildElementTypeMapping =
        XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("projectId").field("type", "keyword").endObject()
            .startObject("pipelineId").field("type", "keyword").endObject()
            .startObject("buildId").field("type", "keyword").endObject()
            .startObject("elementId").field("type", "keyword").endObject()
            .startObject("elementName").field("type", "keyword").endObject()
            .startObject("status").field("type", "keyword").endObject()
            .startObject("beginTime").field("type", "long").endObject()
            .startObject("endTime").field("type", "long").endObject()
            .startObject("type").field("type", "keyword").endObject()
            .startObject("atomCode").field("type", "keyword").endObject()
            .endObject()
            .endObject()

    private fun checkTypeCreate(index: String, type: String): Boolean {
        val key = composeTypeCacheKey(index, type)
        if (typeCache.getIfPresent(key) == true) {
            return true
        }
        val redisLock = RedisLock(redisOperation, "$TYPE_LOCK_KEY:$key", 10)
        try {
            redisLock.lock()
            if (typeCache.getIfPresent(key) == true) {
                return true
            }
            if (isExistType(index, type)) {
                logger.info("[$index|$type] The type is already created")
                typeCache.put(key, true)
                return true
            }
            return false
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkIndexCreate(index: String): Boolean {
        if (indexCache.getIfPresent(index) == true) {
            return true
        }
        val redisLock = RedisLock(redisOperation, "$INDEX_LOCK_KEY:$index", 10)
        try {
            redisLock.lock()
            if (indexCache.getIfPresent(index) == true) {
                return true
            }
            if (isExistIndex(index)) {
                logger.info("[$index] The index is already created")
                indexCache.put(index, true)
                return true
            }
            return false
        } finally {
            redisLock.unlock()
        }
    }

    private fun isExistIndex(index: String): Boolean {
        val response = lambdaEsClient.client.admin()
            .indices()
            .prepareExists(index)
            .get()
        return response.isExists
    }

    private fun isExistType(index: String, type: String): Boolean {
        return lambdaEsClient.client.admin()
            .indices()
            .prepareTypesExists(index)
            .setTypes(type)
            .get().isExists
    }

    private fun getType(index: String, type: DataType): String {
        return when (type) {
            DataType.PipelineBuild -> "$index-build_type_mapping"
            DataType.PipelineBuildElement -> "$index-element_type_mapping"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ESService::class.java)
        private const val INDEX_LOCK_KEY = "lambda:index:create:lock:key"
        private const val TYPE_LOCK_KEY = "lambda:type:create:lock:key"
    }
}