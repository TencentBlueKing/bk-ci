package com.tencent.devops.dispatch.macos.util

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 线程池封装类
 */
class MacOSThreadPoolUtils private constructor() {

    @Volatile
    private var threadPoolMap = hashMapOf<String, ThreadPoolExecutor>()

    private val corePoolSize = 30
    private val maxPoolSize = 30
    private val keepAliveTime: Long = 600
    private val queueSize = 10

    fun getThreadPool(poolName: ThreadPoolName): ThreadPoolExecutor {
        var threadPoolExecutor = threadPoolMap[poolName.name]
        if (null == threadPoolExecutor) {
            synchronized(this) {
                if (null == threadPoolExecutor) {
                    threadPoolExecutor = ThreadPoolExecutor(
                        corePoolSize,
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

    companion object {
        val instance = MacOSThreadPoolUtils()
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
