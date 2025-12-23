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

/**
 * RabbitMQ 匿名队列消费者健康检查单例
 *
 * 功能说明：
 * 1. 监听 SimpleMessageListenerContainer 的致命错误事件
 * 2. 当检测到队列消费者发生致命错误（如队列不存在）时，将容器标记为不健康
 * 3. 只在服务使用了匿名队列时才生效
 *
 * 使用场景：
 * 当 Pod 网络异常导致 RabbitMQ 连接断开后，匿名队列会被自动删除。
 * 重连时消费者尝试声明已删除的队列会失败，此时需要通过健康检查让 K8s 重启 Pod。
 */
class AnonymousQueueHealthIndicator : HealthIndicator {

    companion object {
        private val logger = LoggerFactory.getLogger(AnonymousQueueHealthIndicator::class.java)
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /**
         * 全局单例实例
         */
        @Volatile
        private var INSTANCE: AnonymousQueueHealthIndicator? = null

        /**
         * 是否已注册匿名队列（只有注册了匿名队列才启用健康检查）
         */
        private val hasAnonymousQueue = AtomicBoolean(false)

        /**
         * 获取单例实例
         */
        @JvmStatic
        fun getInstance(): AnonymousQueueHealthIndicator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnonymousQueueHealthIndicator().also { INSTANCE = it }
            }
        }

        /**
         * 注册匿名队列，标记服务使用了匿名队列
         * 应在 StreamBindingEnvironmentPostProcessor 中调用
         */
        @JvmStatic
        fun registerAnonymousQueue(queueName: String) {
            hasAnonymousQueue.set(true)
            logger.info("[AnonymousQueueHealthIndicator] Registered anonymous queue: $queueName")
        }

        /**
         * 检查服务是否使用了匿名队列
         */
        @JvmStatic
        fun hasAnonymousQueue(): Boolean = hasAnonymousQueue.get()
    }

    /**
     * 健康状态标志
     */
    private val healthy = AtomicBoolean(true)

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
        val isFatal: Boolean
    )

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
            container.queueNames?.joinToString(",") ?: "unknown"
        } else {
            "unknown"
        }

        // 获取容器状态信息
        val containerState = if (container is SimpleMessageListenerContainer) {
            buildString {
                append("isRunning=${container.isRunning}, ")
                append("isActive=${container.isActive}, ")
                append("activeConsumerCount=${container.activeConsumerCount}, ")
                append("queueNames=${container.queueNames}")
            }
        } else {
            "unknown"
        }

        // 记录详细日志
        logger.error(
            "[AnonymousQueueHealthIndicator] Consumer container failed event received!\n" +
                "========== Error Details ==========\n" +
                "Queue Names: $queueNames\n" +
                "Is Fatal: $isFatal\n" +
                "Reason: $reason\n" +
                "Container State: $containerState\n" +
                "Exception Type: ${throwable?.javaClass?.name}\n" +
                "Exception Message: ${throwable?.message}\n" +
                "Root Cause: ${getRootCause(throwable)?.message}\n" +
                "===================================",
            throwable
        )

        // 只处理致命错误队列的情况
        if (isFatal) {
            val errorInfo = FatalErrorInfo(
                queueName = queueNames,
                errorMessage = throwable?.message ?: "Unknown error",
                exceptionClass = throwable?.javaClass?.name ?: "Unknown",
                timestamp = LocalDateTime.now().format(DATE_FORMATTER),
                containerState = containerState,
                isFatal = true
            )
            fatalErrors[queueNames] = errorInfo

            // 将健康状态设置为不健康
            healthy.set(false)

            logger.error(
                "[AnonymousQueueHealthIndicator] FATAL ERROR DETECTED!\n" +
                    "Container marked as UNHEALTHY due to fatal error on anonymous queue.\n" +
                    "Queue: $queueNames\n" +
                    "The pod should be killed and restarted by Kubernetes.\n" +
                    "Total fatal errors recorded: ${fatalErrors.size}"
            )
        }
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
                .withDetail("fatalErrorCount", fatalErrors.size)
                .build()
        } else {
            val builder = Health.down()
                .withDetail("message", "Fatal error detected in anonymous queue consumer(s)")
                .withDetail("anonymousQueueEnabled", true)
                .withDetail("fatalErrorCount", fatalErrors.size)
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
                        "isFatal" to errorInfo.isFatal
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
     * 重置健康状态（仅用于测试）
     */
    fun reset() {
        healthy.set(true)
        fatalErrors.clear()
        logger.info("[AnonymousQueueHealthIndicator] Health status reset to healthy")
    }
}
