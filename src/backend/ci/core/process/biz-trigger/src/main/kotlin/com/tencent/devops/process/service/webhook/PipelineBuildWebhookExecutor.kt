package com.tencent.devops.process.service.webhook

import com.tencent.devops.common.util.ThreadPoolUtil
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 旧版 Webhook 触发使用的线程池封装
 *
 * - 池大小和队列大小通过配置 `scm.webhook.trigger.pool-size` / `scm.webhook.trigger.queue-size` 控制
 * - 队列满时使用 [LoggingCallerRunsPolicy]，由调用线程兜底执行并打 warn 日志，避免触发任务被静默丢弃
 */
@Component
class PipelineBuildWebhookExecutor {

    /**
     * 线程池大小，建议按以下公式配置：
     *
     *   pool-size = min(DB 连接池预算, MQ 消费者数 × max-concurrent-per-request)
     *
     * - 上限来自 DB 连接池预算，避免本服务把连接池打满（瞬时连接占用 ≈ pool-size）
     * - 下限来自 MQ 消费者数 × max-concurrent-per-request，保证并发触发真正能跑满
     *
     * 默认 10：按最小部署形态的 MQ 消费者数 × max-concurrent-per-request 默认值估算，
     * 保证默认配置下单 webhook 内并发能跑满，且瞬时连接占用可控；
     * 若实际 MQ 消费者数或 max-concurrent-per-request 更大，必须按上述公式显式覆盖，
     * 否则单 webhook 内并发会退化为串行。
     */
    @Value("\${scm.webhook.trigger.pool-size:10}")
    private var poolSize: Int = 10

    @Value("\${scm.webhook.trigger.queue-size:128}")
    private var queueSize: Int = 128

    private val executor: ThreadPoolExecutor by lazy {
        ThreadPoolUtil.getThreadPoolExecutor(
            corePoolSize = poolSize,
            maximumPoolSize = poolSize,
            threadNamePrefix = "old-webhook-trigger-%d",
            queue = LinkedBlockingQueue(queueSize),
            handler = LoggingCallerRunsPolicy()
        )
    }

    /**
     * 在触发 CallerRunsPolicy 兜底执行前打 warn 日志，便于监控线程池打满的情况。
     * 行为与 [ThreadPoolExecutor.CallerRunsPolicy] 一致：
     * - 线程池未 shutdown：交由调用线程亲自执行
     * - 线程池已 shutdown：任务被丢弃
     */
    private class LoggingCallerRunsPolicy : ThreadPoolExecutor.CallerRunsPolicy() {
        override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
            if (executor.isShutdown) {
                logger.warn(
                    "webhook-trigger pool shutdown, task discarded|" +
                        "active(${executor.activeCount})|queue(${executor.queue.size})"
                )
            } else {
                logger.warn(
                    "webhook-trigger pool exhausted, fallback to caller-runs|" +
                        "max(${executor.maximumPoolSize})|active(${executor.activeCount})|" +
                        "queue(${executor.queue.size})|complete(${executor.completedTaskCount})"
                )
            }
            super.rejectedExecution(r, executor)
        }
    }

    fun <T> submit(task: Callable<T>): Future<T> = executor.submit(task)

    @PreDestroy
    fun destroy() {
        // 当线程池内还有线程执行时，阻塞服务的退出，但设置最大等待上限，避免 Pod 无法优雅退出
        logger.warn("Start destroy threadPool")
        executor.shutdown()
        var waited = 0L
        while (waited < EXECUTOR_DESTROY_MAX_WAIT_SECOND &&
            !executor.awaitTermination(EXECUTOR_DESTROY_AWAIT_SECOND, TimeUnit.SECONDS)
        ) {
            waited += EXECUTOR_DESTROY_AWAIT_SECOND
            logger.warn(
                "ThreadPool: old-webhook-trigger still has tasks|" +
                    "waited=${waited}s|max=${EXECUTOR_DESTROY_MAX_WAIT_SECOND}s"
            )
        }
        if (!executor.isTerminated) {
            logger.warn("ThreadPool: old-webhook-trigger force shutdown after ${waited}s")
            executor.shutdownNow()
        }
        logger.warn("Finish destroy threadPool")
    }

    @Scheduled(cron = "0/10 * * * * ?")
    fun monitorTask() {
        logger.info(
            "webhook-trigger: max(${executor.maximumPoolSize})|" +
                    "poolSize(${executor.poolSize})|active(${executor.activeCount})|" +
                    "queue(${executor.queue.size})|complete(${executor.completedTaskCount})"
        )
    }

    companion object {
        private const val EXECUTOR_DESTROY_AWAIT_SECOND = 5L
        private const val EXECUTOR_DESTROY_MAX_WAIT_SECOND = 30L
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookExecutor::class.java)
    }
}
