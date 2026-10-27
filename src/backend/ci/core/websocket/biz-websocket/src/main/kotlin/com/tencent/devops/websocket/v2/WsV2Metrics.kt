package com.tencent.devops.websocket.v2

import com.tencent.devops.common.websocket.v2.WsV2Properties
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
 * websocket 服务上的 v2 水位，通过 /management/prometheus 拉取。
 * 队列长度接近容量、连接或 binding 顶满、发送变慢、broker 不 ready，都是下一次瓶颈的直接信号。
 * 百分位是单 Pod 客户端计算，不能跨 Pod 相加，看各 Pod 的最大值。
 */
@Component
class WsV2Metrics(
    private val meterRegistry: MeterRegistry,
    private val properties: WsV2Properties,
    private val handler: ObjectProvider<WsV2Handler>,
    private val broker: ObjectProvider<WsSubscriptionBroker>
) : WsV2Instrumentation {

    @PostConstruct
    fun registerGauges() {
        gauge("websocket_v2_connections", "Open v2 websocket connections") { handler()?.connectionCount() ?: 0 }
        gauge("websocket_v2_connections_limit", "Configured max v2 connections") { properties.maxConnections }
        gauge("websocket_v2_routes", "Distinct routing keys with at least one local subscriber") {
            handler()?.routeCount() ?: 0
        }
        gauge("websocket_v2_bindings", "Rabbit bindings held by this pod") { broker()?.bindingCount() ?: 0 }
        gauge("websocket_v2_bindings_limit", "Configured max Rabbit bindings on this pod") { properties.maxBindings }
        gauge("websocket_v2_broker_ready", "Whether the v2 broker channel is consuming (1=ready)") {
            if (broker()?.ready == true) 1 else 0
        }
        gauge("websocket_v2_worker_active", "Worker threads currently handling client frames") {
            handler()?.workerActiveCount() ?: 0
        }
        gauge("websocket_v2_worker_queue_size", "Client frames waiting for a worker thread") {
            handler()?.workerQueueSize() ?: 0
        }
        gauge("websocket_v2_worker_queue_capacity", "Worker queue capacity; a full queue rejects the connection") {
            properties.taskCapacity
        }
        gauge("websocket_v2_sender_active", "Sender threads currently writing to sockets") {
            handler()?.senderActiveCount() ?: 0
        }
        gauge("websocket_v2_sender_queue_size", "Socket writes waiting for a sender thread") {
            handler()?.senderQueueSize() ?: 0
        }
        gauge("websocket_v2_sender_queue_capacity", "Sender queue capacity; a full queue rejects the connection") {
            properties.taskCapacity
        }
        gauge("websocket_v2_pending_frames", "Frames queued on open sockets because the client is slower than the flush") {
            handler()?.pendingFrameCount() ?: 0
        }
        gauge("websocket_v2_pending_bytes", "Bytes queued on open sockets") { handler()?.pendingByteCount() ?: 0 }
        gauge("websocket_v2_dirty_topics", "Topics coalesced in the merge window and not yet flushed") {
            handler()?.dirtyTopicCount() ?: 0
        }
        gauge("websocket_v2_slow_writes", "Sockets currently blocked inside a send") { handler()?.slowWriteCount() ?: 0 }
    }

    override fun connectionRejected(reason: String) = count("websocket_v2_connection_rejected_total", "reason", reason,
        "Connections refused before they were registered")

    override fun connectionClosed(reason: String) = count("websocket_v2_connection_closed_total", "reason", reason,
        "Connections closed by the server or the client")

    override fun overload(reason: String) = count("websocket_v2_overload_total", "reason", reason,
        "Work rejected because a v2 limit was already reached")

    override fun invalidationsSent(count: Int) {
        if (count <= 0) return
        Counter.builder("websocket_v2_invalidate_sent_total")
            .description("Invalidate frames queued for delivery")
            .register(meterRegistry)
            .increment(count.toDouble())
    }

    override fun sendCompleted(elapsedNs: Long, success: Boolean) {
        timer("websocket_v2_send", "Time spent writing one frame to a socket", elapsedNs,
            Tags.of("success", success.toString()))
    }

    override fun handshakeRejected(reason: String) = count("websocket_v2_handshake_rejected_total", "reason", reason,
        "Handshakes rejected before the session was created")

    override fun brokerConsumed(result: String, elapsedNs: Long) {
        count("websocket_v2_broker_consume_total", "result", result, "Broker deliveries observed by this pod")
        timer("websocket_v2_broker_consume", "Time the consumer thread spent on one delivery, including local fanout",
            elapsedNs, Tags.of("result", result))
    }

    override fun brokerRecovered(success: Boolean) = count(
        "websocket_v2_broker_recovery_total", "success", success.toString(),
        "Attempts to redeclare the exclusive v2 queue and resume consuming"
    )

    private fun handler(): WsV2Handler? = handler.getIfAvailable()

    private fun broker(): WsSubscriptionBroker? = broker.getIfAvailable()

    private fun gauge(name: String, description: String, value: () -> Int) {
        Gauge.builder(name) { value().toDouble() }
            .description(description)
            .register(meterRegistry)
    }

    private fun count(name: String, tag: String, value: String, description: String) {
        Counter.builder(name)
            .description(description)
            .tag(tag, value)
            .register(meterRegistry)
            .increment()
    }

    private fun timer(name: String, description: String, elapsedNs: Long, tags: Tags) {
        Timer.builder(name)
            .description(description)
            .tags(tags)
            .publishPercentiles(*PERCENTILES)
            .register(meterRegistry)
            .record(elapsedNs.coerceAtLeast(0), TimeUnit.NANOSECONDS)
    }

    companion object {
        private val PERCENTILES = doubleArrayOf(0.95, 0.99)
    }
}
