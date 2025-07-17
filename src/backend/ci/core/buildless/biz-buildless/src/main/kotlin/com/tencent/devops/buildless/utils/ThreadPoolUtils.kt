package com.tencent.devops.buildless.utils

import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
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
    private val corePoolSize = cpuCount + 10

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

    fun getThreadPool(
        poolName: String,
        corePoolSize: Int = 0,
        maxPoolSize: Int = 0
    ): ThreadPoolExecutor {
        val newCorePoolSize = if (corePoolSize == 0) this.corePoolSize else corePoolSize
        val newMaxPoolSize = if (maxPoolSize == 0) this.maxPoolSize else maxPoolSize

        var threadPoolExecutor = threadPoolMap[poolName]
        if (null == threadPoolExecutor) {
            synchronized(SingleHolder::class.java) {
                // 双重检查锁定
                threadPoolExecutor = threadPoolMap[poolName]
                if (null == threadPoolExecutor) {
                    threadPoolExecutor = ThreadPoolExecutor(
                        /* corePoolSize = */ newCorePoolSize,
                        /* maximumPoolSize = */ newMaxPoolSize,
                        /* keepAliveTime = */ keepAliveTime,
                        /* unit = */ TimeUnit.SECONDS,
                        /* workQueue = */ ArrayBlockingQueue(queueSize),
                        // 自定义线程工厂，添加线程池名称前缀
                        /* threadFactory = */ { r ->
                            Thread(r).apply {
                                name = "pool-$poolName-thread-$id"
                            }
                        },
                        // 拒绝策略记录线程池名称
                        /* handler = */ { _, _ ->
                            logger.info("Thread pool $poolName rejected execution")
                        }
                    ).apply {
                        // 允许核心线程超时回收
                        allowCoreThreadTimeOut(true)
                        // 注册到线程池映射
                        threadPoolMap[poolName] = this
                    }
                }
            }
        }

        return threadPoolExecutor ?: throw IllegalStateException("Thread pool $poolName initialization failed")
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
