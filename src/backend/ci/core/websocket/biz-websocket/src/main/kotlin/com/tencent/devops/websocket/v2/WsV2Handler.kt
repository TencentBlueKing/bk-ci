package com.tencent.devops.websocket.v2

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.websocket.v2.WsInvalidation
import com.tencent.devops.common.websocket.v2.WsTopic
import com.tencent.devops.common.websocket.v2.WsV2Properties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WsV2Handler(
    private val properties: WsV2Properties,
    private val broker: WsSubscriptionBroker,
    private val authorizer: WsTopicAuthorizer
) : TextWebSocketHandler() {
    private data class Subscription(val topic: WsTopic, val generation: String)
    private data class Target(val connectionId: String, val topic: String)
    private class Connection(val userId: String, val socket: WebSocketSession) {
        val subscriptions = linkedMapOf<String, Subscription>()
        val dirty = linkedMapOf<String, WsInvalidation>()
        val input = ArrayDeque<() -> Unit>()
        val output = ArrayDeque<TextMessage>()
        val closed = AtomicBoolean(false)
        val authorizing = AtomicBoolean(false)
        var reading = false
        var writing = false
        var outputBytes = 0
        @Volatile var writeStarted = 0L
        @Volatile var lastSeen = System.currentTimeMillis()
        @Volatile var lastPing = lastSeen
        @Volatile var lastAuthorization = lastSeen
        var rateWindow = lastSeen
        var operationCount = 0
    }

    private val mapper = JsonUtil.getObjectMapper(false)
    private val connections = ConcurrentHashMap<String, Connection>()
    private val routes = ConcurrentHashMap<String, MutableSet<Target>>()
    private val workers = ThreadPoolExecutor(
        properties.workerThreads, properties.workerThreads, 60L, TimeUnit.SECONDS,
        ArrayBlockingQueue(properties.taskCapacity),
        { runnable -> Thread(runnable, "websocket-v2-worker").apply { isDaemon = true } }
    )
    private val senders = ThreadPoolExecutor(
        properties.workerThreads, properties.workerThreads, 60L, TimeUnit.SECONDS,
        ArrayBlockingQueue(properties.taskCapacity),
        { runnable -> Thread(runnable, "websocket-v2-sender").apply { isDaemon = true } }
    )
    private val timer = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "websocket-v2-timer").apply { isDaemon = true }
    }
    @Volatile private var epoch = UUID.randomUUID().toString()
    @Volatile private var stopping = false

    init {
        properties.validate()
        broker.listen(::invalidate, ::reset)
        timer.scheduleWithFixedDelay(::tickSafely, properties.mergeWindowMs, properties.mergeWindowMs, TimeUnit.MILLISECONDS)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes[USER_ATTRIBUTE] as? String
        val state = if (userId != null) Connection(
            userId, ConcurrentWebSocketSessionDecorator(session, properties.sendTimeoutMs, properties.maxPendingBytes)
        ) else null
        synchronized(connections) {
            if (state == null || stopping || !broker.ready || connections.size >= properties.maxConnections) {
                session.close(CloseStatus(1013, "Try again later"))
                return
            }
            connections[session.id] = state
        }
        session.textMessageSizeLimit = properties.maxFrameBytes
        send(requireNotNull(state), mapOf(
            "op" to "hello", "connectionId" to session.id, "epoch" to epoch,
            "heartbeatInterval" to properties.heartbeatMs,
            "limits" to mapOf("topics" to properties.maxTopics, "batch" to properties.maxBatchSize)
        ))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val state = connections[session.id] ?: return
        if (message.payloadLength > properties.maxFrameBytes) {
            close(state, CloseStatus.TOO_BIG_TO_PROCESS)
            return
        }
        state.lastSeen = System.currentTimeMillis()
        synchronized(state) {
            if (state.lastSeen - state.rateWindow > 1000) {
                state.rateWindow = state.lastSeen
                state.operationCount = 0
            }
            if (++state.operationCount > 64) {
                close(state, CloseStatus.POLICY_VIOLATION)
                return
            }
        }
        submitInput(state) { process(state, message.payload) }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        connections[session.id]?.let { close(it, status) }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        connections[session.id]?.let { close(it, CloseStatus.SERVER_ERROR) }
    }

    private fun process(state: Connection, payload: String) {
        val frame = mapper.readTree(payload)
        require(frame != null && frame.isObject && frame.path("v").asInt() == 2) { "INVALID_FRAME" }
        when (frame.path("op").asText()) {
            "ping" -> send(state, mapOf("op" to "pong", "nonce" to frame.path("nonce").asText().take(128)))
            "pong" -> Unit
            "sub", "unsub" -> {
                val requestId = frame.path("requestId").asText()
                val generation = frame.path("generation").asText()
                require(requestId.length in 1..128 && generation.length in 1..128) { "INVALID_REQUEST" }
                val values = frame.path("topics")
                require(values.isArray && values.size() in 1..properties.maxBatchSize) { "INVALID_TOPICS" }
                require(values.all { it.isTextual && it.asText().length <= 600 }) { "INVALID_TOPICS" }
                val topics = values.map { it.asText() }.toSet()
                if (frame.path("op").asText() == "unsub") {
                    synchronized(state) {
                        topics.forEach { value ->
                            if (state.subscriptions[value]?.generation == generation) removeSubscription(state, value)
                        }
                    }
                    send(state, mapOf("op" to "unsub_ack", "requestId" to requestId, "generation" to generation))
                } else {
                    subscribe(state, topics, requestId, generation)
                }
            }
            else -> throw IllegalArgumentException("UNKNOWN_OPERATION")
        }
    }

    private fun subscribe(state: Connection, values: Set<String>, requestId: String, generation: String) {
        val parsed = values.mapNotNull(WsTopic::parse).associateBy { it.value }
        val permitted = authorizer.authorize(state.userId, parsed.values)
        val accepted = mutableListOf<String>()
        val rejected = linkedMapOf<String, String>()
        synchronized(state) {
            if (state.closed.get()) return
            values.forEach { value ->
                val topic = parsed[value]
                when {
                    topic == null -> rejected[value] = "INVALID_TOPIC"
                    value !in permitted -> {
                        removeSubscription(state, value)
                        rejected[value] = "FORBIDDEN"
                    }
                    value !in state.subscriptions && state.subscriptions.size >= properties.maxTopics ->
                        rejected[value] = "TOPIC_LIMIT"
                    else -> {
                        if (value !in state.subscriptions) {
                            try {
                                broker.bind(topic.routingKeys())
                            } catch (exception: Exception) {
                                rejected[value] = "UNAVAILABLE"
                                return@forEach
                            }
                            topic.routingKeys().forEach { key ->
                                routes.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }
                                    .add(Target(state.socket.id, value))
                            }
                        }
                        state.subscriptions[value] = Subscription(topic, generation)
                        accepted.add(value)
                    }
                }
            }
        }
        send(state, mapOf(
            "op" to "sub_ack", "requestId" to requestId, "generation" to generation,
            "accepted" to accepted, "rejected" to rejected, "epoch" to epoch
        ))
    }

    private fun removeSubscription(state: Connection, value: String) {
        val removed = state.subscriptions.remove(value) ?: return
        state.dirty.remove(value)
        removed.topic.routingKeys().forEach { key ->
            routes.computeIfPresent(key) { _, targets ->
                targets.remove(Target(state.socket.id, value))
                targets.takeIf { it.isNotEmpty() }
            }
        }
        broker.unbind(removed.topic.routingKeys())
    }

    private fun invalidate(key: String, event: WsInvalidation) {
        routes[key]?.forEach { target ->
            val state = connections[target.connectionId] ?: return@forEach
            synchronized(state) {
                if (target.topic in state.subscriptions && !state.closed.get()) state.dirty[target.topic] = event
            }
        }
    }

    private fun reset() {
        epoch = UUID.randomUUID().toString()
        connections.values.forEach { state ->
            send(state, mapOf("op" to "resync_required", "epoch" to epoch, "reason" to "BROKER_RECOVERY"))
        }
    }

    private fun tickSafely() {
        try {
            val now = System.currentTimeMillis()
            connections.values.forEach { state ->
                if (now - state.lastSeen > properties.heartbeatMs * 2 ||
                    (state.writeStarted > 0 && now - state.writeStarted > properties.sendTimeoutMs)
                ) {
                    close(state, CloseStatus.SESSION_NOT_RELIABLE)
                    return@forEach
                }
                if (now - state.lastPing >= properties.heartbeatMs) {
                    state.lastPing = now
                    send(state, mapOf("op" to "ping", "nonce" to now.toString()))
                    if (!broker.ready) {
                        close(state, CloseStatus.SERVICE_RESTARTED)
                        return@forEach
                    }
                }
                val frames = synchronized(state) {
                    val pending = state.dirty.mapNotNull { (topic, event) ->
                        state.subscriptions[topic]?.let { subscription -> mapOf(
                            "op" to "invalidate", "topic" to topic, "generation" to subscription.generation,
                            "eventId" to event.eventId, "emittedAt" to event.emittedAt, "epoch" to epoch
                        ) }
                    }
                    state.dirty.clear()
                    pending
                }
                frames.forEach { send(state, it) }
                if (now - state.lastAuthorization >= properties.authorizationTtlMs &&
                    state.authorizing.compareAndSet(false, true)
                ) {
                    submitInput(state) { reauthorize(state) }
                }
            }
        } catch (exception: Exception) {
            logger.warn("Websocket v2 maintenance failed", exception)
        }
    }

    private fun reauthorize(state: Connection) {
        try {
            val topics = synchronized(state) { state.subscriptions.values.map { it.topic } }
            val allowed = authorizer.authorize(state.userId, topics)
            synchronized(state) {
                topics.filter { it.value !in allowed }.forEach {
                    removeSubscription(state, it.value)
                    send(state, mapOf("op" to "error", "code" to "FORBIDDEN", "topic" to it.value))
                }
            }
            state.lastAuthorization = System.currentTimeMillis()
        } finally {
            state.authorizing.set(false)
        }
    }

    private fun submitInput(state: Connection, action: () -> Unit) {
        synchronized(state) {
            if (state.closed.get()) return
            if (state.input.size >= 32) {
                close(state, CloseStatus(1013, "Try again later"))
                return
            }
            state.input.addLast(action)
            if (state.reading) return
            state.reading = true
        }
        execute(state) {
            while (!state.closed.get()) {
                val action = synchronized(state) {
                    state.input.pollFirst() ?: run { state.reading = false; return@execute }
                }
                try {
                    action()
                } catch (exception: IllegalArgumentException) {
                    send(state, mapOf("op" to "error", "code" to "INVALID_REQUEST"))
                    close(state, CloseStatus.POLICY_VIOLATION)
                } catch (exception: Exception) {
                    logger.warn("Websocket v2 request failed", exception)
                    close(state, CloseStatus.SERVER_ERROR)
                }
            }
        }
    }

    private fun send(state: Connection, fields: Map<String, Any>) {
        val message = TextMessage(mapper.writeValueAsString(fields + ("v" to 2)))
        synchronized(state) {
            if (state.closed.get()) return
            if (state.output.size >= properties.maxPendingFrames ||
                state.outputBytes + message.payloadLength > properties.maxPendingBytes
            ) {
                close(state, CloseStatus(1013, "Try again later"))
                return
            }
            state.output.addLast(message)
            state.outputBytes += message.payloadLength
            if (state.writing) return
            state.writing = true
        }
        execute(state, sending = true) {
            while (!state.closed.get()) {
                val next = synchronized(state) {
                    val item = state.output.pollFirst() ?: run { state.writing = false; return@execute }
                    state.outputBytes -= item.payloadLength
                    item
                }
                try {
                    state.writeStarted = System.currentTimeMillis()
                    state.socket.sendMessage(next)
                } catch (exception: Exception) {
                    close(state, CloseStatus.SESSION_NOT_RELIABLE)
                } finally {
                    state.writeStarted = 0
                }
            }
        }
    }

    private fun execute(state: Connection, sending: Boolean = false, action: () -> Unit) {
        try {
            (if (sending) senders else workers).execute(action)
        } catch (exception: RejectedExecutionException) {
            close(state, CloseStatus(1013, "Try again later"))
        }
    }

    private fun close(state: Connection, status: CloseStatus) {
        if (!state.closed.compareAndSet(false, true)) return
        connections.remove(state.socket.id, state)
        synchronized(state) {
            state.subscriptions.keys.toList().forEach { removeSubscription(state, it) }
            state.input.clear()
            state.output.clear()
            state.dirty.clear()
        }
        try {
            if (state.socket.isOpen) state.socket.close(status)
        } catch (exception: Exception) {
            logger.debug("Websocket v2 close failed", exception)
        }
    }

    @PreDestroy
    fun shutdown() {
        stopping = true
        timer.shutdownNow()
        connections.values.toList().forEach { close(it, CloseStatus.SERVICE_RESTARTED) }
        workers.shutdownNow()
        senders.shutdownNow()
    }

    companion object {
        const val USER_ATTRIBUTE = "bk.websocket.v2.user"
        private val logger = LoggerFactory.getLogger(WsV2Handler::class.java)
    }
}
