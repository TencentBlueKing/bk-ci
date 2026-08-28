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

package com.tencent.devops.log.service

import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.log.constant.Constants
import com.tencent.devops.log.configuration.LogBulkProperties
import com.tencent.devops.log.jmx.LogStorageBean
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.core.TimeValue
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

data class BulkOfferResult(
    val success: Boolean,
    val elapseMs: Long,
    val message: String? = null,
    val reason: String = REASON_OK
) {
    companion object {
        /** 写入成功 */
        const val REASON_OK = "ok"

        /** 聚合器待 flush 队列已满，直接背压拒绝 */
        const val REASON_QUEUE_FULL = "queue_full"

        /** 等待 bulk 结果超时：ES 慢或 flush 线程池排队 */
        const val REASON_TIMEOUT = "timeout"

        /** bulk 执行本身失败（ES 返回 failures 或抛异常） */
        const val REASON_BULK_FAILED = "bulk_failed"

        /** 等待过程中出现非超时异常 */
        const val REASON_ERROR = "error"
    }
}

/**
 * 跨 MQ 消息的 ES bulk 聚合器：按 ES 集群分桶，达到条数/字节/等待时间后统一 flush。
 * 调用方必须等待结果返回后再 ack，避免先 ack 后丢失。
 */
@Component
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
class LogBulkAggregator(
    private val bulkProperties: LogBulkProperties,
    private val logStorageBean: LogStorageBean
) {

    private data class PendingWrite(
        val buildId: String,
        val requests: List<IndexRequest>,
        val approxBytes: Long,
        val future: CompletableFuture<BulkOfferResult>,
        val enqueueTime: Long = System.currentTimeMillis()
    )

    private class ClusterBuffer(
        val client: ESClient
    ) {
        val lock = Any()
        val pending = ArrayList<PendingWrite>()
        var docs: Int = 0
        var bytes: Long = 0
        val buildIds = HashSet<String>()
    }

    private val buffers = ConcurrentHashMap<String, ClusterBuffer>()
    private val pendingBatchCount = AtomicInteger(0)
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "log-bulk-aggregator-schedule").apply { isDaemon = true }
    }
    private val flushQueue = LinkedBlockingQueue<Runnable>()
    private val flushExecutor = bulkProperties.flushPoolSize.coerceAtLeast(1).let { poolSize ->
        ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, flushQueue) { r ->
            Thread(r, "log-bulk-flush").apply { isDaemon = true }
        }
    }

    /** 正在执行 ES bulk 的 flush 线程数，持续等于池大小说明 flush 已打满 */
    fun flushActiveCount(): Int = flushExecutor.activeCount

    /** 等待 flush 线程的批次数，非零即说明 bulk 执行速度已跟不上聚合速度 */
    fun flushQueueSize(): Int = flushQueue.size

    fun flushPoolSize(): Int = flushExecutor.maximumPoolSize

    /** 仍在缓冲区内等待攒批的批次数，反映 offer 洪峰 */
    fun pendingBatches(): Int = pendingBatchCount.get()

    @PostConstruct
    fun start() {
        val interval = (bulkProperties.maxWaitMs / 2).coerceAtLeast(20)
        scheduler.scheduleAtFixedRate(
            { flushExpired() },
            interval,
            interval,
            TimeUnit.MILLISECONDS
        )
    }

    @PreDestroy
    fun shutdown() {
        try {
            buffers.values.forEach { buffer ->
                val batch = synchronized(buffer.lock) {
                    if (buffer.pending.isEmpty()) {
                        emptyList()
                    } else {
                        val pending = ArrayList(buffer.pending)
                        buffer.pending.clear()
                        buffer.docs = 0
                        buffer.bytes = 0
                        buffer.buildIds.clear()
                        pendingBatchCount.addAndGet(-pending.size)
                        pending
                    }
                }
                if (batch.isNotEmpty()) {
                    doFlush(buffer.client, batch)
                }
            }
        } finally {
            scheduler.shutdownNow()
            flushExecutor.shutdown()
            try {
                flushExecutor.awaitTermination(10, TimeUnit.SECONDS)
            } catch (ignore: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * 提交一批已生成的 IndexRequest，阻塞直到所属 bulk flush 完成或超时。
     */
    fun offer(
        client: ESClient,
        buildId: String,
        requests: List<IndexRequest>,
        approxBytes: Long,
        timeoutMs: Long = bulkProperties.writeTimeoutMs
    ): BulkOfferResult {
        if (requests.isEmpty()) {
            return BulkOfferResult(success = true, elapseMs = 0)
        }
        if (pendingBatchCount.get() >= bulkProperties.maxPendingBatches) {
            logStorageBean.bulkOffer(0, BulkOfferResult.REASON_QUEUE_FULL)
            return BulkOfferResult(
                success = false,
                elapseMs = 0,
                message = "bulk pending queue is full",
                reason = BulkOfferResult.REASON_QUEUE_FULL
            )
        }

        val offerStart = System.currentTimeMillis()
        val future = CompletableFuture<BulkOfferResult>()
        val pending = PendingWrite(
            buildId = buildId,
            requests = requests,
            approxBytes = approxBytes,
            future = future
        )
        val buffer = buffers.computeIfAbsent(client.clusterName) { ClusterBuffer(client) }
        synchronized(buffer.lock) {
            buffer.pending.add(pending)
            buffer.docs += requests.size
            buffer.bytes += approxBytes
            buffer.buildIds.add(buildId)
            pendingBatchCount.incrementAndGet()
            if (needFlush(buffer)) {
                flushLocked(buffer)
            }
        }

        val result = try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (ignore: TimeoutException) {
            BulkOfferResult(
                success = false,
                elapseMs = timeoutMs,
                message = "bulk write timeout",
                reason = BulkOfferResult.REASON_TIMEOUT
            )
        } catch (ignore: Exception) {
            BulkOfferResult(
                success = false,
                elapseMs = timeoutMs,
                message = ignore.message ?: "bulk write failed",
                reason = BulkOfferResult.REASON_ERROR
            )
        }
        // offer 总耗时包含攒批等待，与 log_es_bulk 的差值即聚合窗口与 flush 排队的开销
        logStorageBean.bulkOffer(System.currentTimeMillis() - offerStart, result.reason)
        return result
    }

    private fun needFlush(buffer: ClusterBuffer): Boolean {
        if (buffer.docs >= bulkProperties.maxDocs) {
            return true
        }
        if (buffer.bytes >= bulkProperties.maxBytes) {
            return true
        }
        if (buffer.buildIds.size >= bulkProperties.maxBuildsPerBulk) {
            return true
        }
        val oldest = buffer.pending.firstOrNull()?.enqueueTime ?: return false
        return System.currentTimeMillis() - oldest >= bulkProperties.maxWaitMs
    }

    private fun flushExpired() {
        buffers.values.forEach { buffer ->
            synchronized(buffer.lock) {
                if (buffer.pending.isNotEmpty() && needFlush(buffer)) {
                    flushLocked(buffer)
                }
            }
        }
    }

    private fun flushLocked(buffer: ClusterBuffer) {
        if (buffer.pending.isEmpty()) {
            return
        }
        val batch = ArrayList(buffer.pending)
        buffer.pending.clear()
        buffer.docs = 0
        buffer.bytes = 0
        buffer.buildIds.clear()
        pendingBatchCount.addAndGet(-batch.size)

        // 必须在锁外执行 ES bulk，避免同集群写入被串行化
        flushExecutor.execute {
            doFlush(buffer.client, batch)
        }
    }

    private fun doFlush(client: ESClient, batch: List<PendingWrite>) {
        val start = System.currentTimeMillis()
        val docs = batch.sumOf { it.requests.size }
        var success = false
        var errorMessage: String? = null
        try {
            val bulkRequest = BulkRequest()
                .timeout(TimeValue.timeValueMillis(client.requestTimeout))
            batch.forEach { pending ->
                pending.requests.forEach { bulkRequest.add(it) }
            }
            if (bulkRequest.numberOfActions() == 0) {
                success = true
            } else {
                success = executeBulk(client, bulkRequest)
                if (!success) {
                    errorMessage = "bulk response has failures"
                }
            }
        } catch (ignore: Exception) {
            success = false
            errorMessage = ignore.message
            logger.warn(
                "Flush log bulk failed, cluster={}, batches={}, docs={}",
                client.clusterName,
                batch.size,
                docs,
                ignore
            )
        } finally {
            val elapse = System.currentTimeMillis() - start
            logStorageBean.bulkRequest(elapse, success, client.clusterName)
            logStorageBean.bulkFlush(batch.size, docs, client.clusterName)
            val result = BulkOfferResult(
                success = success,
                elapseMs = elapse,
                message = errorMessage,
                reason = if (success) BulkOfferResult.REASON_OK else BulkOfferResult.REASON_BULK_FAILED
            )
            batch.forEach { pending ->
                pending.future.complete(result)
            }
            if (elapse >= SLOW_FLUSH_WARN_MS) {
                logger.warn(
                    "Log bulk flush spent too much time({}ms), cluster={}, batches={}, docs={}",
                    elapse,
                    client.clusterName,
                    batch.size,
                    docs
                )
            }
        }
    }

    private fun executeBulk(client: ESClient, bulkRequest: BulkRequest): Boolean {
        return try {
            val response = client.restClient.bulk(bulkRequest, RequestOptions.DEFAULT)
            !response.hasFailures()
        } catch (ignore: Exception) {
            val exString = ignore.toString()
            if (exString.contains("circuit_breaking_exception")) {
                logger.warn(
                    "Bulk hit circuit_breaking_exception, retry with larger buffer, cluster={}",
                    client.clusterName,
                    ignore
                )
                val response = client.restClient.bulk(
                    bulkRequest.timeout(TimeValue.timeValueSeconds(SEARCH_TIMEOUT_SECONDS)),
                    genLargeSearchOptions()
                )
                !response.hasFailures()
            } else {
                throw ignore
            }
        }
    }

    private fun genLargeSearchOptions(): RequestOptions {
        val builder = RequestOptions.DEFAULT.toBuilder()
        builder.setHttpAsyncResponseConsumerFactory(
            HeapBufferedResponseConsumerFactory(Constants.RESPONSE_ENTITY_MAX_SIZE)
        )
        return builder.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogBulkAggregator::class.java)
        private const val SEARCH_TIMEOUT_SECONDS = 60L
        private const val SLOW_FLUSH_WARN_MS = 1000L
    }
}
