package com.tencent.devops.dispatch.macos.util

import com.tencent.devops.common.redis.RedisLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 线程池封装类
 *
 */
@Component
class MacOSThreadPoolUtils private constructor() {

    @Volatile
    private var threadPoolMap = hashMapOf<String, ThreadPoolExecutor>()

    private val coolPoolSize = 30
    private val maxPoolSize = 30
    private val keepAliveTime: Long = 600
    private val queueSize = 10

    fun getThreadPool(poolName: ThreadPoolName): ThreadPoolExecutor {
        var threadPoolExecutor = threadPoolMap[poolName.name]
        if (null == threadPoolExecutor) {
            synchronized(this) {
                if (null == threadPoolExecutor) {
                    threadPoolExecutor = ThreadPoolExecutor(
                        coolPoolSize,
                        maxPoolSize,
                        keepAliveTime,
                        TimeUnit.SECONDS,
                        LinkedBlockingQueue(queueSize)
                    ).apply { allowCoreThreadTimeOut(true) }
                    threadPoolMap[poolName.name] = threadPoolExecutor!!
                }
            }
        }

        return threadPoolExecutor!!
    }

    /**
     *   @param poolName 针对每个POOL_NAME 获取对应的线程池
     *   取消 移除线程池
     * */
    fun exitThreadPool(poolName: String) {
        threadPoolMap[poolName]?.let {
            it.shutdownNow()
            threadPoolMap.remove(poolName)
        }
    }

    fun destroy() {
        // 当线程池内还有线程执行时，阻塞服务的退出
        ThreadPoolName.values().forEach { poolName ->
            threadPoolMap[poolName.name]?.let {
                it.shutdown()
                while (!it.awaitTermination(EXECUTOR_DESTROY_AWAIT_SECOND, TimeUnit.SECONDS)) {
                    logger.warn("SignTaskBean still has sign tasks.")
                }
            }
        }
    }

    @Scheduled(cron = "0 0/3 * * * ?")
    fun monitorTask() {
        ThreadPoolName.values().forEach { poolName ->
            threadPoolMap[poolName.name]?.let {
                logger.info("=================$poolName==================");
                logger.info("线程池大小（Pool Size）: ${it.poolSize}}")
                logger.info("活动线程数（Active Threads）: ${it.activeCount}")
                logger.info("已完成任务数（Completed Task Count）: ${it.completedTaskCount}")
                logger.info("待执行任务队列长度（Queue Length）: ${it.queue.size}")
                logger.info("最大允许的线程数（Max Pool Size）: ${it.maximumPoolSize}")
                logger.info("=================$poolName==================")
            }
        }
    }

    companion object {
        val instance = MacOSThreadPoolUtils()

        private const val EXECUTOR_DESTROY_AWAIT_SECOND = 5L
        private val logger = LoggerFactory.getLogger(MacOSThreadPoolUtils::class.java)
    }
}

enum class ThreadPoolName {
    /**
     * 启动构建任务线程池
     */
    STARTUP,

    /**
     * 结束构建任务线程池
     */
    SHUTDOWN
}
