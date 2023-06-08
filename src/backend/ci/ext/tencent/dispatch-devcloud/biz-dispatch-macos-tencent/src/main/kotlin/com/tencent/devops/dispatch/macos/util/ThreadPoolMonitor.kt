package com.tencent.devops.dispatch.macos.util

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

/**
 * 线程池监控类
 */
@Component
class ThreadPoolMonitor {

    @PreDestroy
    fun destroy() {
        // 当线程池内还有线程执行时，阻塞服务的退出
        logger.warn("Start destroy threadPool")
        ThreadPoolName.values().forEach { poolName ->
            MacOSThreadPoolUtils.instance.getThreadPool(poolName).let {
                it.shutdown()
                while (!it.awaitTermination(EXECUTOR_DESTROY_AWAIT_SECOND, TimeUnit.SECONDS)) {
                    logger.warn("ThreadPool: $poolName still has tasks.")
                }
            }
        }
        logger.warn("Finish destroy threadPool")
    }

    @Scheduled(cron = "0/10 * * * * ?")
    fun monitorTask() {
        ThreadPoolName.values().forEach { poolName ->
            MacOSThreadPoolUtils.instance.getThreadPool(poolName).let {
                logger.info("$poolName: max(${it.maximumPoolSize})|poolSize(${it.poolSize})|active(${it.activeCount})" +
                                "|queue(${it.queue.size})|complete(${it.completedTaskCount})")
            }
        }
    }

    companion object {
        private const val EXECUTOR_DESTROY_AWAIT_SECOND = 5L
        private val logger = LoggerFactory.getLogger(MacOSThreadPoolUtils::class.java)
    }
}
