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

    private var threadPoolMap = hashMapOf<String, ThreadPoolExecutor>()

    /**
     * cpu数量
     * */
    private val cpuCount = Runtime.getRuntime().availableProcessors()

    /**
     * 核心线程数为手机CPU数量+1
     * */
    private val coolPoolSize = cpuCount + 1

    /**
     * 最大线程数为手机CPU数量×2+1
     * */
    private val maxPoolSize = cpuCount * 2 + 1

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

    private fun getThreadPool(tag: String): ThreadPoolExecutor {
        var threadPoolExecutor = threadPoolMap[tag]
        if (threadPoolExecutor == null) {
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
            threadPoolExecutor.allowCoreThreadTimeOut(true)
            threadPoolMap[tag] = threadPoolExecutor
        }
        return threadPoolExecutor
    }

    /**
     *  @param tag 针对每个TAG 获取对应的线程池
     *  @param runnable 对应的 runnable 任务
     * */
    fun removeTask(tag: String, runnable: Runnable) {
        getThreadPool(tag).queue?.remove(runnable)
    }

    /**
     *  @param tag 针对每个TAG 获取对应的线程池
     *  @param runnable 对应的 runnable 任务
     * */
    fun addTask(tag: String, runnable: Runnable) {
        getThreadPool(tag).execute(runnable)
    }

    /**
     *   @param tag 针对每个TAG 获取对应的线程池
     *   取消 移除线程池
     * */
    fun exitThreadPool(tag: String) {
        val threadPoolExecutor = threadPoolMap[tag]
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow()
            threadPoolMap.remove(tag)
        }
    }
}
