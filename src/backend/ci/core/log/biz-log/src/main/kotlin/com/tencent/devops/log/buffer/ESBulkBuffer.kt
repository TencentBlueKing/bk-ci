package com.tencent.devops.log.buffer

import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.log.pojo.message.LogMessageToBulk
import com.tencent.devops.log.es.ESClient
import com.tencent.devops.log.jmx.LogStorageBean
import com.tencent.devops.log.util.Constants.SEARCH_TIMEOUT_SECONDS
import com.tencent.devops.log.util.ESIndexUtils
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.core.TimeValue
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock

class ESBulkBuffer(private val maxSize: Int) {

    fun enqueue(items: List<LogMessageToBulk>, bulkClient: ESClient, logStorageBean: LogStorageBean) {
        lock.lock()
        try {
            if (storageQueue.size >= maxSize) {
                flushBuffer(bulkClient, logStorageBean)
            }
            storageQueue.addAll(items)
        } finally {
            lock.unlock()
        }
    }

    fun flushBuffer(bulkClient: ESClient, logStorageBean: LogStorageBean) {
        lock.lock()
        try {
            if (storageQueue.isEmpty()) return
            if (doBulkMultiLines(bulkClient, storageQueue, logStorageBean) == 0) {
                logger.error(
                    "BKSystemErrorMonitor|flushBuffer|${bulkClient.clusterName}|failed with: [$storageQueue]"
                )
            }
            storageQueue.clear()
        } finally {
            lock.unlock()
        }
    }

    companion object {

        private val storageQueue: LinkedList<LogMessageToBulk> = LinkedList()
        private val logger = LoggerFactory.getLogger(ESBulkBuffer::class.java)
        private val lock = ReentrantLock(false)

        fun doBulkMultiLines(
            bulkClient: ESClient,
            logMessages: List<LogMessageToBulk>,
            logStorageBean: LogStorageBean
        ): Int {
            val clusterName = bulkClient.clusterName
            val currentEpoch = System.currentTimeMillis()
            var lines = 0
            var bulkLines = 0
            val bulkRequest = BulkRequest()
                .timeout(TimeValue.timeValueMillis(bulkClient.requestTimeout))
            for (i in logMessages.indices) {
                val logMessage = logMessages[i]

                val indexRequest = genIndexRequest(
                    logMessage = logMessage
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
                        "$clusterName|Add bulk lines failed|$exString, attempting to add index. [$logMessages]",
                        ignore
                    )
                    val bulkResponse = bulkClient.restClient.bulk(
                        bulkRequest.timeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS)),
                        ESIndexUtils.genLargeSearchOptions()
                    )
                    bulkLines = bulkResponse.count()
                    return if (bulkResponse.hasFailures()) {
                        logger.error(bulkResponse.buildFailureMessage())
                        0
                    } else {
                        bulkLines
                    }
                } else {
                    logger.warn("[$clusterName] Add bulk lines failed because of unknown Exception. [$logMessages]", ignore)
                    throw ignore
                }
            } finally {
                if (bulkLines != lines) {
                    logger.warn("[$clusterName] Part of bulk lines failed, lines:$lines, bulkLines:$bulkLines")
                }
                val elapse = System.currentTimeMillis() - currentEpoch
                logStorageBean.bulkRequest(elapse, bulkLines > 0)

                // #4265 当日志消息处理时间过长时打印消息内容
                if (elapse >= 1000 && logMessages.isNotEmpty()) logger.warn(
                    "[$clusterName] doAddMultiLines spent too much time($elapse) with tag=${logMessages.first().tag}"
                )
            }
        }

        private fun genIndexRequest(logMessage: LogMessageToBulk): IndexRequest? {
            val builder = ESIndexUtils.getDocumentObject(logMessage)
            val index = logMessage.index
            return try {
                IndexRequest(index).source(builder)
            } catch (e: IOException) {
                logger.error("[$index] Convert logMessage to es document failure", e)
                null
            } finally {
                builder.close()
            }
        }
    }
}
