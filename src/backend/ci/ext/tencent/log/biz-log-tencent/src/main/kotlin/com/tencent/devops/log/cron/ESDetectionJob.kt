package com.tencent.devops.log.cron

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.impl.MultiESLogClient
import com.tencent.devops.log.model.message.LogMessageWithLineNo
import com.tencent.devops.log.util.ESIndexUtils.getIndexSettings
import com.tencent.devops.log.util.ESIndexUtils.getTypeMappings
import com.tencent.devops.log.util.ESIndexUtils.indexRequest
import com.tencent.devops.log.util.IndexNameUtils
import org.elasticsearch.common.unit.TimeValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class ESDetectionJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val logClient: MultiESLogClient
) {

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(2, TimeUnit.DAYS)
        .build<String/*index*/, Boolean>()
    private val executor = Executors.newCachedThreadPool()

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    fun detect() {
        logger.info("Start to detect es")
        val redisLock = RedisLock(redisOperation, MULTI_LOG_CLIENT_DETECTION_LOCK_KEY, 30)
        try {
            if (redisLock.tryLock()) {
                val buildId = UUIDUtil.generate()
                val index = IndexNameUtils.getIndexName()
                logClient.getActiveClients().forEach {
                    val f = executor.submit(Detection(it, index, buildId))
                    val documentIds = f.get(20, TimeUnit.SECONDS)
                    if (documentIds.isEmpty()) {
                        logger.warn("[${it.name}] Fail to insert data")
                        logClient.markESInactive(it.name)
                    } else {
                        executor.submit(Deletion(it, index, buildId, documentIds))
                    }
                }
                logger.info("Start to check the inactive clients")
                logClient.getInactiveClients().forEach {
                    val f = executor.submit(Detection(it, index, buildId))
                    val documentIds = f.get(20, TimeUnit.SECONDS)
                    if (documentIds.isNotEmpty()) {
                        logger.warn("[${it.name}] Success to insert data")
                        logClient.markESActive(it.name)
                        executor.submit(Deletion(it, index, buildId, documentIds))
                    }
                }
            } else {
                logger.info("Other instance took the key, ignore")
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun createIndex(esClient: ESClient, index: String) {
        logger.info("[${esClient.name}|$index] Create the index")
        try {
            val response = esClient.client.admin()
                .indices()
                .prepareCreate(index)
                .setSettings(getIndexSettings())
                .addMapping(getType(index), getTypeMappings())
                .get(TimeValue.timeValueSeconds(5))
            logger.info("Get the create index response: $response")
        } catch (t: Throwable) {
            logger.warn("Fail to create the index, ignore", t)
        }
    }

    private fun addLines(esClient: ESClient, index: String, buildId: String): List<String> {
        logger.info("[${esClient.name}|$index|$buildId] Start to add lines")
        val type = getType(index)
        val bulkRequestBuilder = esClient.client.prepareBulk()
        for (i in 1 until MULTI_LOG_LINES) {
            val log = LogMessageWithLineNo(
                tag = "test-tag-$i",
                jobId = "job-$i",
                message = "message lines - $i",
                timestamp = System.currentTimeMillis(),
                lineNo = i.toLong()
            )
            val builder = esClient.client.prepareIndex(buildId, index, type)
                .setCreate(false)
                .setSource(indexRequest(buildId, log, index, type))
            bulkRequestBuilder.add(builder)
        }
        try {
            val bulkResponse = bulkRequestBuilder.get(TimeValue.timeValueSeconds(10))
            if (bulkResponse.hasFailures()) {
                logger.warn("[${esClient.name}|$index|$buildId] Fail to add lines: ${bulkResponse.buildFailureMessage()}")
            } else {
                logger.info("[${esClient.name}|$index|$buildId] Success to add lines")
            }
            return bulkResponse.filter { !it.isFailed }.map { it.id }
        } catch (e: Exception) {
            logger.warn("[${esClient.name}|$index|$buildId] Fail to add lines", e)
        }
        return emptyList()
    }

    private inner class Detection(
        private val esClient: ESClient,
        private val index: String,
        private val buildId: String
    ): Callable<List<String>> {

        override fun call(): List<String> {
            try {
                // 1. Check if need to create index
                if (indexCache.getIfPresent(index) != true) {
                    createIndex(esClient, index)
                    indexCache.put(index, true)
                }
                // 2. insert data
                return addLines(esClient, index, buildId)
            } catch (t: Throwable) {
                logger.warn("[${esClient.name}|$index] Fail to do the es detection", t)
            }
            return emptyList()
        }
    }

    private inner class Deletion(
        private val esClient: ESClient,
        private val index: String,
        private val buildId: String,
        private val documentIds: List<String>
    ): Runnable {
        override fun run() {
            logger.info("[${esClient.name}|$index|$buildId|${documentIds.size}] Start to delete the record")
            if (documentIds.isEmpty()) {
                logger.info("Empty document ids")
                return
            }
            val type = getType(index)
            val builder = esClient.client.prepareBulk()
            documentIds.forEach {
                val deleteBuilder = esClient.client.prepareDelete(index, type, it)
                builder.add(deleteBuilder)
            }

            val bulkResponse = builder.get(TimeValue.timeValueSeconds(5))
            if (bulkResponse.hasFailures()) {
                logger.warn("[${esClient.name}|$index|$buildId] Fail to delete lines: $documentIds, ${bulkResponse.buildFailureMessage()}")
            } else {
                logger.info("[${esClient.name}|$index|$buildId] Success to delete the records")
            }
        }
    }

    private fun getType(index: String) = "$index-detection"

    companion object {
        private val logger = LoggerFactory.getLogger(ESDetectionJob::class.java)
        private const val MULTI_LOG_CLIENT_DETECTION_LOCK_KEY = "log:multi:log:client:detection:lock:key"
        private const val MULTI_LOG_LINES = 10
    }
}