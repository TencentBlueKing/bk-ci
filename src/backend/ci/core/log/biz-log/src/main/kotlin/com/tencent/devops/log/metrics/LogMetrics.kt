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

package com.tencent.devops.log.metrics

import com.tencent.devops.log.event.ILogEvent
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogOriginHeavyEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import com.tencent.devops.log.jmx.LogPrintBean
import com.tencent.devops.log.service.LogStorageDegradeSwitcher
import com.tencent.devops.log.service.LogTrafficStatsService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * log 服务 Micrometer 指标：
 * - ES 读写耗时/成败（含 bulk 聚合、直写、查询、下载、降级）
 * - Kafka 投递/消费耗时与成败（按队列 destination 区分）
 * - 打印线程池、熔断、热点 build 规模等 Gauge
 *
 * 通过 /management/prometheus 拉取。
 */
@Component
class LogMetrics(
    private val meterRegistry: MeterRegistry,
    private val logPrintBean: LogPrintBean,
    private val logTrafficStatsService: LogTrafficStatsService,
    private val logStorageDegradeSwitcher: ObjectProvider<LogStorageDegradeSwitcher>
) {

    @PostConstruct
    fun registerGauges() {
        Gauge.builder("log_print_task_count") { logPrintBean.getPrintTaskCount().toDouble() }
            .description("Log print executor completed task count")
            .register(meterRegistry)
        Gauge.builder("log_print_active_count") { logPrintBean.getPrintActiveCount().toDouble() }
            .description("Log print executor active thread count")
            .register(meterRegistry)
        Gauge.builder("log_print_queue_size") { logPrintBean.getPrintQueueSize().toDouble() }
            .description("Log print executor queue size")
            .register(meterRegistry)
        Gauge.builder("log_traffic_heavy_size") { logTrafficStatsService.heavySize().toDouble() }
            .description("Number of builds currently marked as heavy traffic")
            .register(meterRegistry)
        Gauge.builder("log_es_circuit_open") {
            if (logStorageDegradeSwitcher.getIfAvailable()?.isCircuitOpen() == true) 1.0 else 0.0
        }
            .description("Whether ES direct-write circuit is open (1=open, 0=closed)")
            .register(meterRegistry)
    }

    fun recordEsBatchWrite(elapseMs: Long, success: Boolean) {
        recordTimer(
            name = "log_es_batch_write",
            description = "ES batch write latency (origin/storage path)",
            elapseMs = elapseMs,
            tags = Tags.of("success", success.toString())
        )
        Counter.builder("log_es_batch_write_total")
            .description("ES batch write count")
            .tags("success", success.toString())
            .register(meterRegistry)
            .increment()
    }

    fun recordEsBulk(elapseMs: Long, success: Boolean, cluster: String? = null) {
        val tags = Tags.of("success", success.toString())
            .and("cluster", cluster?.takeIf { it.isNotBlank() } ?: "unknown")
        recordTimer(
            name = "log_es_bulk",
            description = "ES bulk request latency",
            elapseMs = elapseMs,
            tags = tags
        )
        Counter.builder("log_es_bulk_total")
            .description("ES bulk request count")
            .tags(tags)
            .register(meterRegistry)
            .increment()
    }

    fun recordEsQuery(elapseMs: Long, success: Boolean) {
        recordTimer(
            name = "log_es_query",
            description = "ES log query latency",
            elapseMs = elapseMs,
            tags = Tags.of("success", success.toString())
        )
        Counter.builder("log_es_query_total")
            .description("ES log query count")
            .tags("success", success.toString())
            .register(meterRegistry)
            .increment()
    }

    fun recordEsDownload(elapseMs: Long, success: Boolean) {
        recordTimer(
            name = "log_es_download",
            description = "ES log download latency",
            elapseMs = elapseMs,
            tags = Tags.of("success", success.toString())
        )
        Counter.builder("log_es_download_total")
            .description("ES log download count")
            .tags("success", success.toString())
            .register(meterRegistry)
            .increment()
    }

    fun recordEsDirectWrite(success: Boolean) {
        Counter.builder("log_es_direct_write_total")
            .description("Origin path direct ES write attempts")
            .tags("success", success.toString())
            .register(meterRegistry)
            .increment()
    }

    fun recordEsDegradeToStorage() {
        Counter.builder("log_es_degrade_to_storage_total")
            .description("Times origin direct-write degraded to storage queue")
            .register(meterRegistry)
            .increment()
    }

    fun recordKafkaProduce(event: ILogEvent, elapseMs: Long, success: Boolean) {
        val destination = destinationOf(event)
        val tags = Tags.of("destination", destination).and("success", success.toString())
        recordTimer(
            name = "log_kafka_produce",
            description = "Kafka produce (dispatch) latency for log events",
            elapseMs = elapseMs,
            tags = tags
        )
        Counter.builder("log_kafka_produce_total")
            .description("Kafka produce count for log events")
            .tags(tags)
            .register(meterRegistry)
            .increment()
    }

    fun recordKafkaConsume(destination: String, elapseMs: Long, success: Boolean, retried: Boolean) {
        val tags = Tags.of(
            "destination", destination,
            "success", success.toString(),
            "retried", retried.toString()
        )
        recordTimer(
            name = "log_kafka_consume",
            description = "Kafka consume (handleEvent) latency for log events",
            elapseMs = elapseMs,
            tags = tags
        )
        Counter.builder("log_kafka_consume_total")
            .description("Kafka consume count for log events")
            .tags(tags)
            .register(meterRegistry)
            .increment()
    }

    fun recordPrintRejected() {
        Counter.builder("log_print_rejected_total")
            .description("Async print executor rejected (queue full)")
            .register(meterRegistry)
            .increment()
    }

    fun destinationOf(event: ILogEvent): String {
        return when (event) {
            is LogOriginHeavyEvent -> DESTINATION_ORIGIN_HEAVY
            is LogOriginEvent -> DESTINATION_ORIGIN
            is LogStorageEvent -> DESTINATION_STORAGE
            is LogStatusEvent -> DESTINATION_STATUS
            else -> DESTINATION_UNKNOWN
        }
    }

    private fun recordTimer(name: String, description: String, elapseMs: Long, tags: Tags) {
        Timer.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .record(elapseMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
    }

    companion object {
        const val DESTINATION_ORIGIN = "origin"
        const val DESTINATION_ORIGIN_HEAVY = "origin_heavy"
        const val DESTINATION_STORAGE = "storage"
        const val DESTINATION_STATUS = "status"
        const val DESTINATION_UNKNOWN = "unknown"
    }
}
