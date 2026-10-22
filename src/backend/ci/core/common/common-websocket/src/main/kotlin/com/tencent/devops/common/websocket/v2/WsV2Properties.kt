package com.tencent.devops.common.websocket.v2

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("websocket.v2")
class WsV2Properties {
    var enabled: Boolean = false
    var namespace: String = "default"
    var allowedOrigins: List<String> = emptyList()
    var maxConnections: Int = 10000
    var maxTopics: Int = 128
    var maxBatchSize: Int = 32
    var maxBindings: Int = 20000
    var maxFrameBytes: Int = 16384
    var maxPendingFrames: Int = 256
    var maxPendingBytes: Int = 262144
    var sendTimeoutMs: Int = 5000
    var heartbeatMs: Long = 30000
    var authorizationTtlMs: Long = 60000
    var mergeWindowMs: Long = 300
    var queueTtlMs: Int = 30000
    var queueMaxLength: Int = 10000
    var queueMaxBytes: Int = 16777216
    var workerThreads: Int = 8
    var taskCapacity: Int = 2048

    fun validate() {
        require(namespace.matches(Regex("[A-Za-z0-9_.-]{1,80}"))) { "Invalid websocket v2 namespace" }
        require(allowedOrigins.none { it == "*" || it == "null" }) { "Explicit websocket origins are required" }
        require(listOf(maxConnections, maxTopics, maxBatchSize, maxBindings, maxFrameBytes, maxPendingFrames,
            maxPendingBytes, sendTimeoutMs, queueTtlMs, queueMaxLength, queueMaxBytes, workerThreads,
            taskCapacity).all { it > 0 }) { "Websocket limits must be positive" }
        require(maxBatchSize <= maxTopics && heartbeatMs >= 1000 && authorizationTtlMs >= 1000 && mergeWindowMs > 0)
    }

    fun exchangeName(): String = "$namespace.websocket.v2.invalidate"
}
