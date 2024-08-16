package com.tencent.devops.dispatch.macos.util

import com.tencent.devops.dispatch.macos.constant.Constant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 线程池封装类
 */
class MacOSThreadPoolUtils private constructor() {

    @Volatile
    private var threadPoolMap = ConcurrentHashMap<String, ThreadPoolExecutor>()

    fun getThreadPool(poolName: ThreadPoolName): ThreadPoolExecutor {
        var threadPoolExecutor = threadPoolMap[poolName.name]
        if (null == threadPoolExecutor) {
            synchronized(this) {
                if (null == threadPoolExecutor) {
                    threadPoolExecutor = ThreadPoolExecutor(
                        poolName.corePoolSize,
                        poolName.maxPoolSize,
                        poolName.keepAliveTime,
                        TimeUnit.SECONDS,
                        SynchronousQueue()
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

enum class ThreadPoolName(
    val corePoolSize: Int,
    val maxPoolSize: Int,
    val keepAliveTime: Long
) {
    /**
     * 启动构建任务线程池
     */
    STARTUP(Constant.MAX_STARTUP_CONCURRENCY, Constant.MAX_STARTUP_CONCURRENCY, 600),

    /**
     * 启动降级队列构建任务线程池
     */
    DEMOTE_STARTUP(Constant.MAX_DEMOTE_STARTUP_CONCURRENCY, Constant.MAX_DEMOTE_STARTUP_CONCURRENCY, 600),
}
