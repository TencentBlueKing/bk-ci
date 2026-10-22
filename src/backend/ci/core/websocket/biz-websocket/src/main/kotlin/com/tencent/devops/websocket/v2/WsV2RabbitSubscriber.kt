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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WsV2RabbitSubscriber(
    private val factory: ConnectionFactory,
    private val properties: WsV2Properties
) : WsSubscriptionBroker {
    private val monitor = Any()
    private val references = linkedMapOf<String, Int>()
    private var channel: Channel? = null
    private var queueName: String? = null
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

    override fun bind(keys: Set<String>) = synchronized(monitor) {
        check(ready) { "Websocket broker is unavailable" }
        require(references.size + keys.count { it !in references } <= properties.maxBindings) { "Binding limit" }
        val activeChannel = requireNotNull(channel)
        try {
            keys.filter { it !in references }.forEach { key ->
                activeChannel.queueBind(queueName, properties.exchangeName(), key)
            }
            keys.forEach { references[it] = (references[it] ?: 0) + 1 }
        } catch (exception: Exception) {
            ready = false
            throw exception
        }
    }

    override fun unbind(keys: Set<String>) {
        synchronized(monitor) {
            keys.forEach { key ->
                val count = references[key] ?: return@forEach
                if (count > 1) {
                    references[key] = count - 1
                } else {
                    references.remove(key)
                    if (ready) {
                        try {
                            channel?.queueUnbind(queueName, properties.exchangeName(), key)
                        } catch (exception: Exception) {
                            ready = false
                        }
                    }
                }
            }
        }
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
                    try {
                        if (delivery.body.size <= properties.maxFrameBytes) {
                            val event = JsonUtil.getObjectMapper(false).readValue(delivery.body, WsInvalidation::class.java)
                            if (WsTopic.routingKey(event.topic) == delivery.envelope.routingKey) {
                                onMessage(delivery.envelope.routingKey, event)
                            }
                        }
                    } catch (exception: Exception) {
                        logger.warn("Invalid websocket v2 broker message", exception)
                    } finally {
                        try {
                            activeChannel.basicAck(delivery.envelope.deliveryTag, false)
                        } catch (exception: Exception) {
                            ready = false
                        }
                    }
                }, CancelCallback { ready = false })
                ready = true
            }
            onReset()
        } catch (exception: Exception) {
            ready = false
            logger.warn("Websocket v2 broker recovery failed", exception)
        }
    }

    @PreDestroy
    fun shutdown() {
        stopping = true
        ready = false
        timer.shutdownNow()
        synchronized(monitor) {
            try { channel?.abort() } catch (ignored: Exception) { logger.debug("Broker channel already closed") }
            references.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WsV2RabbitSubscriber::class.java)
    }
}
