package com.tencent.devops.common.websocket.v2

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * process 发往 v2 交换器的发布水位。websocket 进程也会创建这个 bean，但只有真正 publish 的一侧会上涨。
 * 通过 /management/prometheus 拉取。nack 表示 broker 已接收调用但没有确认，消息不会到达订阅者。
 */
class WsV2PublishMetrics(private val meterRegistry: MeterRegistry) {
    private val inFlight = AtomicInteger()

    init {
        Gauge.builder("websocket_v2_publish_inflight") { inFlight.get().toDouble() }
            .description("Invalidations currently blocked inside the Rabbit publish call")
            .register(meterRegistry)
    }

    fun <T> record(action: () -> T): T {
        inFlight.incrementAndGet()
        val started = System.nanoTime()
        return try {
            action().also { record(true, System.nanoTime() - started) }
        } catch (exception: Exception) {
            record(false, System.nanoTime() - started)
            throw exception
        } finally {
            inFlight.decrementAndGet()
        }
    }

    fun nack() {
        Counter.builder("websocket_v2_publish_nack_total")
            .description("Publishes the broker did not acknowledge")
            .register(meterRegistry)
            .increment()
    }

    private fun record(success: Boolean, elapsedNs: Long) {
        val tags = Tags.of("success", success.toString())
        Timer.builder("websocket_v2_publish")
            .description("Time spent sending one invalidation to Rabbit")
            .tags(tags)
            .publishPercentiles(*PERCENTILES)
            .register(meterRegistry)
            .record(elapsedNs.coerceAtLeast(0), TimeUnit.NANOSECONDS)
        Counter.builder("websocket_v2_publish_total")
            .description("Invalidations sent to the v2 exchange")
            .tags(tags)
            .register(meterRegistry)
            .increment()
    }

    companion object {
        private val PERCENTILES = doubleArrayOf(0.95, 0.99)
    }
}
