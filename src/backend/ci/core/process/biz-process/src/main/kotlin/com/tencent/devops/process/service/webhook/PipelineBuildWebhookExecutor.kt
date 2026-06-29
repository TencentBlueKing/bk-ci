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
 * - 队列满时使用 CallerRunsPolicy，由调用线程兜底执行，避免触发任务被丢弃
 */
@Component
class PipelineBuildWebhookExecutor {

    @Value("\${scm.webhook.trigger.pool-size:32}")
    private var poolSize: Int = 32

    @Value("\${scm.webhook.trigger.queue-size:512}")
    private var queueSize: Int = 512

    private val executor: ThreadPoolExecutor by lazy {
        ThreadPoolUtil.getThreadPoolExecutor(
            corePoolSize = poolSize,
            maximumPoolSize = poolSize,
            threadNamePrefix = "old-webhook-trigger-%d",
            queue = LinkedBlockingQueue(queueSize),
            handler = ThreadPoolExecutor.CallerRunsPolicy()
        )
    }

    fun <T> submit(task: Callable<T>): Future<T> = executor.submit(task)

    @PreDestroy
    fun destroy() {
        // 当线程池内还有线程执行时，阻塞服务的退出
        logger.warn("Start destroy threadPool")
        executor.shutdown()
        while (!executor.awaitTermination(EXECUTOR_DESTROY_AWAIT_SECOND, TimeUnit.SECONDS)) {
            logger.warn("ThreadPool: old-webhook-trigger still has tasks.")
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
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookExecutor::class.java)
    }
}
