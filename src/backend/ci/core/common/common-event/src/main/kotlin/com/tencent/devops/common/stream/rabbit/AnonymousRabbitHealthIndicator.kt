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

package com.tencent.devops.common.stream.rabbit

import com.tencent.devops.common.event.annotation.Event
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.listener.ListenerContainerConsumerFailedEvent
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.event.EventListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * RabbitMQ 匿名队列消费者健康检查单例
 *
 * 功能说明：
 * 1. 监听 SimpleMessageListenerContainer 的致命错误事件
 * 2. 当检测到队列消费者发生致命错误（如队列不存在）时，将容器标记为不健康
 * 3. 只在服务使用了匿名队列时才生效
 * 4. 只关心匿名队列的异常事件，忽略持久化队列的事件
 *
 * 使用场景：
 * 当 Pod 网络异常导致 RabbitMQ 连接断开后，匿名队列会被自动删除。
 * 重连时消费者尝试声明已删除的队列会失败，此时需要通过健康检查让 K8s 重启 Pod。
 */
class AnonymousRabbitHealthIndicator : HealthIndicator {

    companion object {
        private val logger = LoggerFactory.getLogger(AnonymousRabbitHealthIndicator::class.java)
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /**
         * 非致命错误的连续失败阈值，超过此阈值标记为不健康
         */
        private const val CONSECUTIVE_FAILURE_THRESHOLD = 3

        /**
         * 需要检测的致命异常类型（这些异常即使 isFatal=false 也应该标记为不健康）
         */
        private val FATAL_EXCEPTION_TYPES = setOf(
            "org.springframework.amqp.rabbit.listener.QueuesNotAvailableException",
            "org.springframework.amqp.rabbit.listener.BlockingQueueConsumer\$DeclarationException"
        )

        /**
         * 需要检测的致命异常消息关键词
         */
        private val FATAL_MESSAGE_KEYWORDS = listOf(
            "queue doesn't exist",
            "no queue",
            "NOT_FOUND",
            "Cannot prepare queue",
            "Failed to declare queue"
        )

        /**
         * 全局单例实例
         */
        @Volatile
        private var INSTANCE: AnonymousRabbitHealthIndicator? = null

        /**
         * 是否已注册匿名队列（只有注册了匿名队列才启用健康检查）
         */
        private val hasAnonymousQueue = AtomicBoolean(false)

        /**
         * 已注册的匿名队列前缀集合
         * 匿名队列的命名规则：{destination}.{groupName}-{randomSuffix}
         * 例如：e.engine.stream.timer.change.stream.process-aAhym6ZnSsClHmoVYlMTMQ
         *
         * 这里存储的是 groupName 前缀（如 "process-"），用于匹配匿名队列
         */
        private val registeredAnonymousQueuePrefixes = ConcurrentHashMap.newKeySet<String>()

        /**
         * 获取单例实例
         */
        @JvmStatic
        fun getInstance(): AnonymousRabbitHealthIndicator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnonymousRabbitHealthIndicator().also { INSTANCE = it }
            }
        }

        /**
         * 注册匿名队列，标记服务使用了匿名队列
         * 应在 StreamBindingEnvironmentPostProcessor 中调用
         *
         * @param event MQ事件定义
         *
         * 匿名队列实际名称格式：{destination}.{groupName}-{randomSuffix}
         * 例如：e.engine.stream.timer.change.stream.process-aAhym6ZnSsClHmoVYlMTMQ
         *
         * 我们从 queueName 中提取 groupName 作为前缀来匹配
         */
        @JvmStatic
        fun registerAnonymousQueue(event: Event) {
            hasAnonymousQueue.set(true)
            val prefix = event.destination
            registeredAnonymousQueuePrefixes.add(prefix)
            logger.info(
                "[AnonymousQueueHealthIndicator] Registered anonymous queue: $prefix, " +
                    "extracted prefix: $prefix"
            )
        }

        /**
         * 检查服务是否使用了匿名队列
         */
        @JvmStatic
        fun hasAnonymousQueue(): Boolean = hasAnonymousQueue.get()

        /**
         * 获取已注册的匿名队列前缀
         */
        @JvmStatic
        fun getRegisteredPrefixes(): Set<String> = registeredAnonymousQueuePrefixes.toSet()
    }

    /**
     * 健康状态标志
     */
    private val healthy = AtomicBoolean(true)

    /**
     * 记录每个队列的连续失败次数
     * Key: 队列名
     * Value: 连续失败次数
     */
    private val consecutiveFailures = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * 记录的致命错误信息，用于详细的健康检查报告
     * Key: 容器标识（队列名）
     * Value: 错误详情
     */
    private val fatalErrors = ConcurrentHashMap<String, FatalErrorInfo>()

    /**
     * 致命错误信息
     */
    data class FatalErrorInfo(
        val queueName: String,
        val errorMessage: String,
        val exceptionClass: String,
        val timestamp: String,
        val containerState: String,
        val isFatal: Boolean,
        val consecutiveFailures: Int,
        val reason: String
    )

    /**
     * 过滤匿名队列
     */
    private fun filterAnonymousQueue(queueNames: List<String>): List<String> {
        if (!hasAnonymousQueue.get()) {
            return emptyList()
        }
        val anonymousQueueNames = mutableListOf<String>()
        // 检查队列名称是否包含已注册的匿名队列前缀
        for (prefix in registeredAnonymousQueuePrefixes) {
            // 进一步检查是否为匿名队列（通过检查后缀是否为随机字符串）
            for (queueName in queueNames) {
                if (queueName.contains(prefix)) {
                    logger.info(
                        "[AnonymousQueueHealthIndicator] Queue '$queueNames' is an anonymous queue " +
                            "(registered prefixes: $registeredAnonymousQueuePrefixes)"
                    )
                    anonymousQueueNames.add(queueName)
                }
            }
        }
        return anonymousQueueNames
    }

    /**
     * 监听 RabbitMQ 消费者容器失败事件
     */
    @EventListener
    fun onListenerContainerConsumerFailed(event: ListenerContainerConsumerFailedEvent) {
        val container = event.source
        val throwable = event.throwable
        val isFatal = event.isFatal
        val reason = event.reason

        // 获取队列名信息
        val queueNames = if (container is SimpleMessageListenerContainer) {
            container.queueNames?.toList() ?: emptyList()
        } else {
            emptyList()
        }

        // 检查是否为匿名队列，如果不是则忽略
        val anonymousQueueNames = filterAnonymousQueue(queueNames)
        if (anonymousQueueNames.isEmpty()) {
            logger.info(
                "[AnonymousQueueHealthIndicator] Ignoring event for non-anonymous queue: $queueNames\n" +
                    "Exception: ${throwable?.javaClass?.name}: ${throwable?.message}"
            )
            return
        }

        // 获取容器状态信息
        val containerState = if (container is SimpleMessageListenerContainer) {
            buildString {
                append("isRunning=${container.isRunning}, ")
                append("isActive=${container.isActive}, ")
                append("activeConsumerCount=${container.activeConsumerCount}, ")
                append("queueNames=$anonymousQueueNames")
            }
        } else {
            "unknown"
        }

        // 检查是否为致命异常类型（即使 isFatal=false）
        val isFatalException = isFatalException(throwable)

        // 取最大的一个失败次数
        var failureCount = 0
        for (queueName in anonymousQueueNames) {
            val tmp = consecutiveFailures
                .computeIfAbsent(queueName) { AtomicInteger(0) }
                .incrementAndGet()
            if (tmp > failureCount) { failureCount = tmp }
        }

        // 判断是否应该标记为不健康
        val shouldMarkUnhealthy = isFatal || isFatalException || failureCount >= CONSECUTIVE_FAILURE_THRESHOLD

        // 记录详细日志
        logger.error(
            "BKSystemErrorMonitor|[AnonymousQueueHealthIndicator] Consumer container failed event received!\n" +
                "========== Error Details ==========\n" +
                "Queue Names: $queueNames\n" +
                "Is Anonymous Queue: true\n" +
                "Is Fatal (from event): $isFatal\n" +
                "Is Fatal Exception: $isFatalException\n" +
                "Consecutive Failures: $failureCount (threshold: $CONSECUTIVE_FAILURE_THRESHOLD)\n" +
                "Should Mark Unhealthy: $shouldMarkUnhealthy\n" +
                "Reason: $reason\n" +
                "Container State: $containerState\n" +
                "Exception Type: ${throwable?.javaClass?.name}\n" +
                "Exception Message: ${throwable?.message}\n" +
                "Root Cause: ${getRootCause(throwable)?.let { "${it.javaClass.name}: ${it.message}" }}\n" +
                "===================================",
            throwable
        )

        // 如果满足不健康条件，记录错误并标记
        if (shouldMarkUnhealthy) {
            val markReason = when {
                isFatal -> "Event marked as fatal"
                isFatalException -> "Fatal exception type detected: ${throwable?.javaClass?.name}"
                else -> "Consecutive failures reached threshold: $failureCount >= $CONSECUTIVE_FAILURE_THRESHOLD"
            }
            anonymousQueueNames.forEach { queueName ->
                val errorInfo = FatalErrorInfo(
                    queueName = queueName,
                    errorMessage = throwable?.message ?: "Unknown error",
                    exceptionClass = throwable?.javaClass?.name ?: "Unknown",
                    timestamp = LocalDateTime.now().format(DATE_FORMATTER),
                    containerState = containerState,
                    isFatal = isFatal || isFatalException,
                    consecutiveFailures = failureCount,
                    reason = markReason
                )
                fatalErrors[queueName] = errorInfo
            }
            // 将健康状态设置为不健康
            healthy.set(false)

            logger.error(
                "[AnonymousQueueHealthIndicator] MARKING CONTAINER AS UNHEALTHY!\n" +
                    "Reason: $markReason\n" +
                    "Queue: $queueNames\n" +
                    "The pod should be killed and restarted by Kubernetes.\n" +
                    "Total fatal errors recorded: ${fatalErrors.size}"
            )
        }
    }

    /**
     * 检查异常是否为致命类型
     * 即使 Spring AMQP 没有标记为 fatal，这些异常也应该触发不健康状态
     */
    private fun isFatalException(throwable: Throwable?): Boolean {
        if (throwable == null) return false

        // 检查异常类型
        var current: Throwable? = throwable
        while (current != null) {
            val className = current.javaClass.name
            if (FATAL_EXCEPTION_TYPES.any { className.contains(it) }) {
                logger.warn("[AnonymousQueueHealthIndicator] Detected fatal exception type: $className")
                return true
            }

            // 检查异常消息中的关键词
            val message = current.message ?: ""
            for (keyword in FATAL_MESSAGE_KEYWORDS) {
                if (message.contains(keyword, ignoreCase = true)) {
                    logger.warn(
                        "[AnonymousQueueHealthIndicator] Detected fatal keyword '$keyword' " +
                            "in exception message: $message"
                    )
                    return true
                }
            }

            current = current.cause
            if (current == throwable) break // 防止循环引用
        }

        return false
    }

    /**
     * 健康检查方法
     */
    override fun health(): Health {
        // 如果服务没有使用匿名队列，直接返回健康
        if (!hasAnonymousQueue.get()) {
            return Health.up()
                .withDetail("message", "No anonymous queue registered, health check skipped")
                .build()
        }

        return if (healthy.get()) {
            Health.up()
                .withDetail("message", "All anonymous queue consumers are healthy")
                .withDetail("anonymousQueueEnabled", true)
                .withDetail("registeredPrefixes", registeredAnonymousQueuePrefixes.toList())
                .withDetail("fatalErrorCount", fatalErrors.size)
                .withDetail("consecutiveFailureThreshold", CONSECUTIVE_FAILURE_THRESHOLD)
                .withDetail("currentFailureCounts", consecutiveFailures.mapValues { it.value.get() })
                .build()
        } else {
            val builder = Health.down()
                .withDetail("message", "Fatal error detected in anonymous queue consumer(s)")
                .withDetail("anonymousQueueEnabled", true)
                .withDetail("registeredPrefixes", registeredAnonymousQueuePrefixes.toList())
                .withDetail("fatalErrorCount", fatalErrors.size)
                .withDetail("consecutiveFailureThreshold", CONSECUTIVE_FAILURE_THRESHOLD)
                .withDetail("recommendation", "Pod needs to be restarted to recover anonymous queue consumers")

            // 添加每个错误的详细信息
            fatalErrors.forEach { (queueName, errorInfo) ->
                builder.withDetail(
                    "error_$queueName",
                    mapOf(
                        "queueName" to errorInfo.queueName,
                        "errorMessage" to errorInfo.errorMessage,
                        "exceptionClass" to errorInfo.exceptionClass,
                        "timestamp" to errorInfo.timestamp,
                        "containerState" to errorInfo.containerState,
                        "isFatal" to errorInfo.isFatal,
                        "consecutiveFailures" to errorInfo.consecutiveFailures,
                        "reason" to errorInfo.reason
                    )
                )
            }

            builder.build()
        }
    }

    /**
     * 获取根本异常
     */
    private fun getRootCause(throwable: Throwable?): Throwable? {
        var cause = throwable
        while (cause?.cause != null && cause.cause != cause) {
            cause = cause.cause
        }
        return cause
    }

    /**
     * 获取当前健康状态
     */
    fun isHealthy(): Boolean = healthy.get()

    /**
     * 获取所有致命错误信息
     */
    fun getFatalErrors(): Map<String, FatalErrorInfo> = fatalErrors.toMap()

    /**
     * 获取当前连续失败计数
     */
    fun getConsecutiveFailures(): Map<String, Int> = consecutiveFailures.mapValues { it.value.get() }

    /**
     * 重置指定队列的连续失败计数（当消费者恢复成功时调用）
     */
    fun resetConsecutiveFailures(queueName: String) {
        consecutiveFailures[queueName]?.set(0)
        logger.info("[AnonymousQueueHealthIndicator] Reset consecutive failures for queue: $queueName")
    }

    /**
     * 重置健康状态（仅用于测试）
     */
    fun reset() {
        healthy.set(true)
        fatalErrors.clear()
        consecutiveFailures.clear()
        logger.info("[AnonymousQueueHealthIndicator] Health status reset to healthy")
    }
}
