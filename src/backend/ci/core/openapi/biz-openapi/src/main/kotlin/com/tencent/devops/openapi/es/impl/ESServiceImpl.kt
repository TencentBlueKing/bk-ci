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

package com.tencent.devops.openapi.es.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.es.client.LogClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.openapi.es.ESIndexUtils
import com.tencent.devops.openapi.es.ESMessage
import com.tencent.devops.openapi.es.IESService
import com.tencent.devops.openapi.es.IndexNameUtils
import com.tencent.devops.openapi.es.config.ESAutoConfiguration
import com.tencent.devops.openapi.es.mq.ESEvent
import com.tencent.devops.openapi.es.mq.MQDispatcher
import com.tencent.devops.openapi.pojo.MetricsApiData
import com.tencent.devops.openapi.pojo.MetricsProjectData
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.pipeline.AvgBucketPipelineAggregationBuilder
import org.elasticsearch.search.aggregations.pipeline.BucketMetricValue
import org.elasticsearch.search.aggregations.pipeline.MaxBucketPipelineAggregationBuilder
import org.elasticsearch.search.aggregations.pipeline.SimpleValue
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory

@Suppress("NestedBlockDepth", "ComplexMethod")
class ESServiceImpl constructor(
    private val logClient: LogClient,
    private val redisOperation: RedisOperation,
    private val dispatcher: MQDispatcher,
    private val configuration: ESAutoConfiguration
) : IESService {

    companion object {
        private val logger = LoggerFactory.getLogger(ESServiceImpl::class.java)
        private const val SEARCH_TIMEOUT_SECONDS = 60L
        private const val INDEX_CACHE_MAX_SIZE = 10L
        private const val INDEX_CACHE_EXPIRE_DAY = 1L
        private const val INDEX_LOCK_EXPIRE_SECONDS = 10L
        private const val INDEX_STORAGE_WARN_MILLIS = 1000
        private const val BULK_BUFFER_SIZE = 100
        private const val maxQueueSize = 9000 // 队列溢出的最大大小
        private const val RESPONSE_ENTITY_MAX_SIZE = 1024 * 1024 * 1024
    }

    private val queue: BlockingQueue<ESMessage> = ArrayBlockingQueue(10000) // 创建一个容量为10000的阻塞队列

    private val indexCache = Caffeine.newBuilder()
        .maximumSize(INDEX_CACHE_MAX_SIZE)
        .expireAfterAccess(INDEX_CACHE_EXPIRE_DAY, TimeUnit.DAYS)
        .build<String/*index*/, Boolean/*Has created the index*/>()

    private val executor = Executors.newCachedThreadPool()

    init {
        repeat(configuration.consumerCount) {
            executor.submit(BulkSend())
        }
    }

    override fun addMessage(message: ESMessage) {
        if (queue.size >= maxQueueSize) {
            dispatcher.dispatchEvent(ESEvent(message)) // 将消息推送到es
        } else {
            queue.put(message) // 将消息放入队列，如果队列已满则阻塞等待
        }
    }

    override fun esReady() = true

    override fun esAddMessage(event: ESEvent) {
        queue.put(event.logs)
    }

    private inner class BulkSend : Runnable {
        val buf = mutableListOf<ESMessage>()
        override fun run() {
            while (true) {
                val message = queue.take() ?: continue
                buf.add(message)
                if (buf.size >= BULK_BUFFER_SIZE) {
                    RetryUtils.execute(action = object : RetryUtils.Action<Unit> {
                        override fun execute() {
                            doAdd()
                        }

                        override fun fail(e: Throwable) {
                            logger.error("add to es failed", e)
                        }
                    }, retryTime = 6, retryPeriodMills = 10000)
                }
            }
        }

        private fun doAdd() {
            val currentEpoch = System.currentTimeMillis()
            try {
                prepareIndex()
                if (doAddMultiLines(buf) == 0) {
                    throw ExecuteException(
                        "None of lines is inserted successfully to ES "
                    )
                } else {
                    buf.clear()
                }
            } finally {
                val elapse = System.currentTimeMillis() - currentEpoch
                // #4265 当日志消息处理时间过长时打印消息内容
                if (elapse >= INDEX_STORAGE_WARN_MILLIS) logger.warn(
                    " addBatchLogEvent spent too much time($elapse)"
                )
            }
        }
    }

    private fun doAddMultiLines(logMessages: List<ESMessage>): Int {
        val currentEpoch = System.currentTimeMillis()
        val index = IndexNameUtils.getIndexName()
        val bulkClient = logClient.hashClient(index)
        var lines = 0
        var bulkLines = 0
        val bulkRequest = BulkRequest()
            .timeout(TimeValue.timeValueMillis(bulkClient.requestTimeout))
        for (i in logMessages.indices) {
            val logMessage = logMessages[i]

            val indexRequest = genIndexRequest(
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
                throw ExecuteException(bulkResponse.buildFailureMessage())
            } else {
                bulkLines
            }
        } catch (ignore: Exception) {
            val exString = ignore.toString()
            if (exString.contains("circuit_breaking_exception")) {
                logger.warn(
                    "Add bulk lines failed|$exString, attempting to add index. [$logMessages]",
                    ignore
                )
                val bulkResponse = bulkClient.restClient.bulk(
                    bulkRequest.timeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS)),
                    genLargeSearchOptions()
                )
                bulkLines = bulkResponse.count()
                return if (bulkResponse.hasFailures()) {
                    logger.error(bulkResponse.buildFailureMessage())
                    0
                } else {
                    bulkLines
                }
            } else {
                logger.warn("Add bulk lines failed because of unknown Exception. [$logMessages]", ignore)
                throw ignore
            }
        } finally {
            if (bulkLines != lines) {
                logger.warn("Part of bulk lines failed, lines:$lines, bulkLines:$bulkLines")
            }
            val elapse = System.currentTimeMillis() - currentEpoch

            // #4265 当日志消息处理时间过长时打印消息内容
            if (elapse >= INDEX_STORAGE_WARN_MILLIS && logMessages.isNotEmpty()) logger.warn(
                "doAddMultiLines spent too much time($elapse) with tag=${logMessages.first()}"
            )
        }
    }

    private fun genIndexRequest(
        logMessage: ESMessage,
        index: String
    ): IndexRequest? {
        val builder = ESIndexUtils.getDocumentObject(logMessage)
        return try {
            IndexRequest(index).source(builder)
        } catch (e: IOException) {
            logger.error("Convert logMessage to es document failure", e)
            null
        } finally {
            builder.close()
        }
    }

    private fun prepareIndex(): Boolean {
        val index = IndexNameUtils.getIndexName()
        return if (!checkIndexCreate(index)) {
            createIndex(index)
            indexCache.put(index, true)
            true
        } else {
            false
        }
    }

    private fun checkIndexCreate(index: String): Boolean {
        if (indexCache.getIfPresent(index) == true) {
            return true
        }
        val redisLock = RedisLock(redisOperation, "LOG:index:create:lock:key:$index", INDEX_LOCK_EXPIRE_SECONDS)
        try {
            redisLock.lock()
            if (indexCache.getIfPresent(index) == true) {
                return true
            }

            // Check from ES
            if (isExistIndex(index)) {
                logger.info("[$index] the index is already created")
                indexCache.put(index, true)
                return true
            }
            return false
        } finally {
            redisLock.unlock()
        }
    }

    private fun createIndex(index: String): Boolean {
        val createClient = logClient.hashClient(index)
        // 提前创建第二天的索引备用
        createESIndex(createClient, IndexNameUtils.getNextIndexName())
        return createESIndex(createClient, index)
    }

    private fun createESIndex(createClient: ESClient, index: String): Boolean {
        logger.info("[$index] Create index")
        return try {
            logger.info(
                "[${createClient.clusterName}][$index]|createIndex|: shards[${createClient.shards}]" +
                    " replicas[${createClient.replicas}] shardsPerNode[${createClient.shardsPerNode}]"
            )
            val request = CreateIndexRequest(index)
                .settings(
                    ESIndexUtils.getIndexSettings(
                        shards = createClient.shards,
                        replicas = createClient.replicas,
                        shardsPerNode = createClient.shardsPerNode
                    )
                )
                .mapping(ESIndexUtils.getTypeMappings())
            request.setTimeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS))
            val response = createClient.restClient.indices()
                .create(request, RequestOptions.DEFAULT)
            response.isShardsAcknowledged
        } catch (e: IOException) {
            logger.error("BKSystemErrorMonitor|[${createClient.clusterName}] Create index $index failure", e)
            return false
        }
    }

    private fun isExistIndex(index: String): Boolean {
        val request = GetIndexRequest(index)
        request.setTimeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS))
        return logClient.hashClient(index).restClient.indices()
            .exists(request, RequestOptions.DEFAULT)
    }

    private fun genLargeSearchOptions(): RequestOptions {
        val builder = RequestOptions.DEFAULT.toBuilder()
        builder.setHttpAsyncResponseConsumerFactory(
            HeapBufferedResponseConsumerFactory(RESPONSE_ENTITY_MAX_SIZE)
        )
        return builder.build()
    }

    /*
    * 每5分钟执行一次
    * 根据"api"字段进行分组，然后在每个分组内根据"key"字段进行进一步的分组。
    * 在每个"key"分组内，使用"date_histogram"聚合，按照"timestamp"字段的时间间隔（1秒）进行分桶。
    * 最后，在每个时间桶内，使用"value_count"聚合计算"timestamp"值的数量，即并发请求数量。
    */
    fun executeElasticsearchQueryS(keyMap: MutableMap<String, MetricsApiData>, newDay: Boolean) {
        val indexName = IndexNameUtils.getIndexName()

        val searchRequest = SearchRequest(indexName)
            .source(
                SearchSourceBuilder()
                    .query(
                        // 设置查询条件
                        QueryBuilders.rangeQuery(ESMessage::timestamp.name)
                            .from("now-5m")
                            .to("now")
                    )
                    .aggregation(
                        // 设置聚合
                        AggregationBuilders.terms(ESMessage::api.name).field(ESMessage::api.name)
                            .subAggregation(
                                AggregationBuilders.terms(ESMessage::key.name).field(ESMessage::key.name)
                                    .subAggregation(
                                        AggregationBuilders.dateHistogram("concurrency")
                                            .field(ESMessage::timestamp.name)
                                            .calendarInterval(DateHistogramInterval.SECOND)
                                            .minDocCount(1)
                                            .subAggregation(
                                                AggregationBuilders.count("count").field(ESMessage::timestamp.name)
                                            )
                                    )
                                    .subAggregation(
                                        MaxBucketPipelineAggregationBuilder(
                                            "max_concurrency",
                                            "concurrency>count"
                                        )
                                    )
                                    .subAggregation(
                                        AvgBucketPipelineAggregationBuilder(
                                            "avg_concurrency",
                                            "concurrency>count"
                                        )
                                    )
                            )
                    )
                    // 不返回任何文档，只返回聚合结果。
                    .size(0)
            )

        val response: SearchResponse = logClient.hashClient(indexName)
            .restClient
            .search(searchRequest, RequestOptions.DEFAULT)

        // 处理聚合结果
        val apiAggregation: Terms = response.aggregations.get(ESMessage::api.name)

        for (apiBucket in apiAggregation.buckets) {
            val apiName = apiBucket.keyAsString
            val keyAggregation: Terms = apiBucket.aggregations.get(ESMessage::key.name)

            for (keyBucket in keyAggregation.buckets) {
                val keyName: String = keyBucket.keyAsString
                val max = keyBucket.aggregations.get<BucketMetricValue>("max_concurrency").value().toInt()
                val avg = keyBucket.aggregations.get<SimpleValue>("avg_concurrency").value().toInt()
                val count = keyBucket.docCount.toInt()
                keyMap["$apiName@$keyName"]?.apply {
                    secondLevelConcurrency = avg
                    peakConcurrency = if (newDay) max else max.coerceAtLeast(peakConcurrency ?: 0)
                    call5m = count
                } ?: kotlin.run {
                    keyMap["$apiName@$keyName"] = MetricsApiData(
                        api = apiName,
                        key = keyName,
                        secondLevelConcurrency = avg,
                        peakConcurrency = max,
                        call5m = count
                    )
                }
            }
        }
    }

    /*
    * 查询时间间隔为time(1h、1d、7d)
    * 根据"api"字段进行分组，然后在每个分组内根据"key"字段进行进一步的分组。
    * 得到每组内的计数
    */
    fun executeElasticsearchQueryM(
        keyMap: MutableMap<String, MetricsApiData>,
        time: String,
        f: (count: Int, data: MetricsApiData) -> MetricsApiData
    ) {
        val indexName = IndexNameUtils.getIndexNamePrefix()

        val searchRequest = SearchRequest(indexName)
            .source(
                SearchSourceBuilder()
                    .query(
                        // 设置查询条件
                        QueryBuilders.rangeQuery(ESMessage::timestamp.name)
                            .from("now-$time")
                            .to("now")
                    )
                    .aggregation(
                        // 计数
                        AggregationBuilders.terms(ESMessage::api.name).field(ESMessage::api.name)
                            .subAggregation(
                                AggregationBuilders.terms(ESMessage::key.name).field(ESMessage::key.name)
                                    .subAggregation(
                                        AggregationBuilders.count("count").field(ESMessage::timestamp.name)
                                    )
                            )
                    )
                    // 不返回任何文档，只返回聚合结果。
                    .size(0)
            )

        val response: SearchResponse = logClient.hashClient(indexName)
            .restClient
            .search(searchRequest, RequestOptions.DEFAULT)

        // 处理聚合结果
        val apiAggregation: Terms = response.aggregations.get(ESMessage::api.name)

        for (apiBucket in apiAggregation.buckets) {
            val apiName = apiBucket.keyAsString
            val keyAggregation: Terms = apiBucket.aggregations.get(ESMessage::key.name)

            for (keyBucket in keyAggregation.buckets) {
                val keyName: String = keyBucket.keyAsString
                val count = keyBucket.docCount.toInt()
                keyMap["$apiName@$keyName"]?.let {
                    f(count, it)
                } ?: kotlin.run {
                    keyMap["$apiName@$keyName"] = f(
                        count, MetricsApiData(
                            api = apiName,
                            key = keyName
                        )
                    )
                }
            }
        }
    }

    /*
    * 查询时间间隔为1h
    * 查询api-key-project三重分组
    * 得到每组内的计数
    */
    fun executeElasticsearchQueryP(
        keyList: MutableList<MetricsProjectData>
    ) {
        val indexName = IndexNameUtils.getIndexName()

        val searchRequest = SearchRequest(indexName)
            .source(
                SearchSourceBuilder()
                    .query(
                        // 设置查询条件
                        QueryBuilders.rangeQuery(ESMessage::timestamp.name)
                            .from("now-1h")
                            .to("now")
                    )
                    .aggregation(
                        // 计数
                        AggregationBuilders.terms(ESMessage::api.name).field(ESMessage::api.name)
                            .subAggregation(
                                AggregationBuilders.terms(ESMessage::key.name).field(ESMessage::key.name)
                                    .subAggregation(
                                        AggregationBuilders.terms(ESMessage::projectId.name)
                                            .field(ESMessage::projectId.name)
                                            .subAggregation(
                                                AggregationBuilders.count("count").field(ESMessage::timestamp.name)
                                            )
                                    )
                            )
                    )
                    // 不返回任何文档，只返回聚合结果。
                    .size(0)
            )

        val response: SearchResponse = logClient.hashClient(indexName)
            .restClient
            .search(searchRequest, RequestOptions.DEFAULT)

        // 处理聚合结果
        val apiAggregation: Terms = response.aggregations.get(ESMessage::api.name)

        for (apiBucket in apiAggregation.buckets) {
            val apiName = apiBucket.keyAsString
            val keyAggregation: Terms = apiBucket.aggregations.get(ESMessage::key.name)

            for (keyBucket in keyAggregation.buckets) {
                val keyName: String = keyBucket.keyAsString
                val projectAggregation: Terms = keyBucket.aggregations.get(ESMessage::projectId.name)

                for (projectBucket in projectAggregation.buckets) {
                    val projectName: String = projectBucket.keyAsString
                    val count = projectBucket.docCount.toInt()
                    keyList.add(
                        MetricsProjectData(
                            api = apiName,
                            key = keyName,
                            projectId = projectName,
                            callHistory = count
                        )
                    )
                }
            }
        }
    }
}
