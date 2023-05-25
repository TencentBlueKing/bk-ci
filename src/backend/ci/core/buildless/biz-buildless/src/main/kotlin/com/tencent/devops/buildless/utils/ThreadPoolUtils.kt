package com.tencent.devops.buildless.utils

import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 线程池封装类
 *
 */
class ThreadPoolUtils private constructor() {

    @Volatile
    private var threadPoolMap = hashMapOf<String, ThreadPoolExecutor>()

    /**
     * cpu数量
     * */
    private val cpuCount = Runtime.getRuntime().availableProcessors()

    /**
     * 核心线程数
     * */
    private val coolPoolSize = cpuCount + 10

    /**
     * 最大线程数
     * */
    private val maxPoolSize = cpuCount * 2 + 10

    /**
     * 线程活跃时间 秒，超时线程会被回收
     * */
    private val keepAliveTime: Long = 60

    /**
     * 等待队列大小
     * */
    private val queueSize = 128

    companion object {
        fun getInstance() = SingleHolder.SINGLE_HOLDER

        private val logger = LoggerFactory.getLogger(ThreadPoolUtils::class.java)
    }

    object SingleHolder {
        val SINGLE_HOLDER = ThreadPoolUtils()
    }

    fun getThreadPool(poolName: String): ThreadPoolExecutor {
        var threadPoolExecutor = threadPoolMap[poolName]
        if (null == threadPoolExecutor) {
            synchronized(SingleHolder::class.java) {
                if (null == threadPoolExecutor) {
                    threadPoolExecutor = ThreadPoolExecutor(
                        coolPoolSize,
                        maxPoolSize,
                        keepAliveTime,
                        TimeUnit.SECONDS,
                        ArrayBlockingQueue(queueSize),
                        Executors.defaultThreadFactory(),
                        RejectedExecutionHandler { _, _ ->
                            logger.info("$ThreadPoolUtils  RejectedExecutionHandler----")
                        }
                    )
                    // 允许核心线程闲置超时时被回收
                    threadPoolExecutor!!.allowCoreThreadTimeOut(true)
                    threadPoolMap[poolName] = threadPoolExecutor!!
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
        val threadPoolExecutor = threadPoolMap[poolName]
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow()
            threadPoolMap.remove(poolName)
        }
    }
}

enum class ThreadPoolName {
    /**
     * 任务认领线程池
     */
    CLAIM_TASK,

    /**
     * 构建结束处理线程池
     */
    BUILD_END,

    /**
     * 创建构建容器线程池
     */
    ADD_CONTAINER
}
