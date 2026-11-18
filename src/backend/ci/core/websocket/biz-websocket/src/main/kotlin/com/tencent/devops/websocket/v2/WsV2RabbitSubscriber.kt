package com.tencent.devops.websocket.v2

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.websocket.v2.WsInvalidation
import com.tencent.devops.common.websocket.v2.WsTopic
import com.tencent.devops.common.websocket.v2.WsV2Properties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WsV2RabbitSubscriber(
    private val factory: ConnectionFactory,
    private val properties: WsV2Properties,
    private val metrics: WsV2Instrumentation = WsV2Instrumentation.NONE
) : WsSubscriptionBroker {
    private val monitor = Any()
    private val references = linkedMapOf<String, Int>()
    @Volatile private var channel: Channel? = null
    @Volatile private var queueName: String? = null
    @Volatile private var stopping = false
    @Volatile override var ready = false
        private set
    @Volatile private var onMessage: (String, WsInvalidation) -> Unit = { _, _ -> }
    @Volatile private var onReset: () -> Unit = {}
    private val timer = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "websocket-v2-broker").apply { isDaemon = true }
    }

    init {
        timer.scheduleWithFixedDelay(::recover, 0, 5, TimeUnit.SECONDS)
    }

    override fun listen(onMessage: (String, WsInvalidation) -> Unit, onReset: () -> Unit) {
        this.onMessage = onMessage
        this.onReset = onReset
    }

    override fun bindingCount(): Int = synchronized(monitor) { references.size }

    override fun bind(keys: Set<String>) {
        onBroker { bindOnBroker(keys) }
    }

    override fun unbind(keys: Set<String>) {
        onBroker { unbindOnBroker(keys) }
    }

    private fun bindOnBroker(keys: Set<String>) {
        val activeChannel: Channel
        val missing: List<String>
        synchronized(monitor) {
            check(ready) { "Websocket broker is unavailable" }
            require(references.size + keys.count { it !in references } <= properties.maxBindings) { "Binding limit" }
            activeChannel = requireNotNull(channel)
            missing = keys.filter { it !in references }
        }
        try {
            missing.forEach { key ->
                activeChannel.queueBind(queueName, properties.exchangeName(), key)
            }
        } catch (exception: Exception) {
            missing.forEach { key ->
                try {
                    activeChannel.queueUnbind(queueName, properties.exchangeName(), key)
                } catch (ignored: Exception) {
                    logger.debug("Rollback of websocket v2 binding failed")
                }
            }
            if (!activeChannel.isOpen) ready = false
            throw exception
        }
        synchronized(monitor) {
            if (channel !== activeChannel || !ready) throw IllegalStateException("Websocket broker is unavailable")
            keys.forEach { references[it] = (references[it] ?: 0) + 1 }
        }
    }

    private fun unbindOnBroker(keys: Set<String>) {
        val activeChannel = channel
        keys.forEach { key -> releaseBinding(activeChannel, key) }
    }

    private fun releaseBinding(activeChannel: Channel?, key: String) {
        if (!dropReference(key) || activeChannel == null) return
        try {
            activeChannel.queueUnbind(queueName, properties.exchangeName(), key)
        } catch (ignored: Exception) {
            forget(activeChannel)
        }
    }

    private fun dropReference(key: String): Boolean {
        synchronized(monitor) {
            val count = references[key] ?: return false
            if (count > 1) {
                references[key] = count - 1
                return false
            }
            references.remove(key)
            return ready
        }
    }

    private fun acknowledge(activeChannel: Channel, deliveryTag: Long) {
        try {
            timer.execute { confirm(activeChannel, deliveryTag) }
        } catch (ignored: Exception) {
            forget(activeChannel)
        }
    }

    private fun confirm(activeChannel: Channel, deliveryTag: Long) {
        try {
            activeChannel.basicAck(deliveryTag, false)
        } catch (ignored: Exception) {
            forget(activeChannel)
        }
    }

    private fun forget(activeChannel: Channel) {
        if (!activeChannel.isOpen && channel === activeChannel) ready = false
    }

    private fun <T> onBroker(block: () -> T): T {
        if (Thread.currentThread().name == BROKER_THREAD) return block()
        return timer.submit(Callable { block() }).get()
    }

    private fun recover() {
        if (ready || stopping) return
        try {
            synchronized(monitor) {
                if (ready || stopping) return
                try { channel?.abort() } catch (ignored: Exception) { logger.debug("Broker channel already closed") }
                val activeChannel = factory.createConnection().createChannel(false)
                channel = activeChannel
                activeChannel.exchangeDeclare(properties.exchangeName(), "direct", true)
                val queue = "${properties.namespace}.websocket.v2.${UUID.randomUUID()}"
                activeChannel.queueDeclare(queue, false, true, true, mapOf(
                    "x-message-ttl" to properties.queueTtlMs,
                    "x-max-length" to properties.queueMaxLength,
                    "x-max-length-bytes" to properties.queueMaxBytes,
                    "x-expires" to 60000
                ))
                queueName = queue
                references.keys.forEach { key -> activeChannel.queueBind(queue, properties.exchangeName(), key) }
                activeChannel.basicQos(128)
                activeChannel.addShutdownListener { ready = false }
                activeChannel.basicConsume(queue, false, DeliverCallback { _, delivery ->
                    val started = System.nanoTime()
                    var result = "invalid"
                    try {
                        if (delivery.body.size > properties.maxFrameBytes) {
                            result = "oversize"
                        } else {
                            val event = JsonUtil.getObjectMapper(false)
                                .readValue(delivery.body, WsInvalidation::class.java)
                            if (WsTopic.routingKey(event.topic) == delivery.envelope.routingKey) {
                                onMessage(delivery.envelope.routingKey, event)
                                result = "delivered"
                            } else {
                                result = "ignored"
                            }
                        }
                    } catch (exception: Exception) {
                        result = "invalid"
                        logger.warn("Invalid websocket v2 broker message", exception)
                    } finally {
                        metrics.brokerConsumed(result, System.nanoTime() - started)
                        acknowledge(activeChannel, delivery.envelope.deliveryTag)
                    }
                }, CancelCallback { ready = false })
                ready = true
            }
            onReset()
            metrics.brokerRecovered(true)
        } catch (exception: Exception) {
            ready = false
            metrics.brokerRecovered(false)
            logger.warn("Websocket v2 broker recovery failed", exception)
        }
    }

    @PreDestroy
    fun shutdown() {
        stopping = true
        ready = false
        try {
            onBroker {
                try { channel?.abort() } catch (ignored: Exception) { logger.debug("Broker channel already closed") }
                synchronized(monitor) { references.clear() }
            }
        } catch (ignored: Exception) {
            logger.debug("Broker thread already stopped")
        }
        timer.shutdownNow()
    }

    companion object {
        private const val BROKER_THREAD = "websocket-v2-broker"
        private val logger = LoggerFactory.getLogger(WsV2RabbitSubscriber::class.java)
    }
}
