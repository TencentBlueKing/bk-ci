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

package com.tencent.devops.openapi.es

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.es.client.LogClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.openapi.es.mq.ESEvent
import com.tencent.devops.openapi.es.mq.MQDispatcher
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.core.TimeValue
import org.slf4j.LoggerFactory

class ESServiceImpl constructor(
    private val logClient: LogClient,
    private val redisOperation: RedisOperation,
    private val dispatcher: MQDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ESServiceImpl::class.java)
        private const val LONG_SEARCH_TIME: Long = 64000
        private const val SHORT_SEARCH_TIME: Long = 32000
        private const val SEARCH_TIMEOUT_SECONDS = 60L
        private const val SEARCH_FRAGMENT_SIZE = 100000
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
        executor.submit(BulkSend())
    }

    fun addMessage(message: ESMessage) {
        if (queue.size >= maxQueueSize) {
            dispatcher.dispatchEvent(ESEvent(message)) // 将消息推送到es
        } else {
            queue.put(message) // 将消息放入队列，如果队列已满则阻塞等待
        }
    }

    fun esAddMessage(event: ESEvent) {
        queue.put(event.logs)
    }

    private inner class BulkSend : Runnable {
        val buf = mutableListOf<ESMessage>()
        override fun run() {
            while (true) {
                val message = queue.poll() ?: continue
                buf.add(message)
                if (buf.size == BULK_BUFFER_SIZE) {
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

    fun
}
