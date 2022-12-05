package com.tencent.bkrepo.common.storage.monitor

import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 存储监控助手
 * */
class StorageHealthMonitorHelper(private val monitorMap: ConcurrentHashMap<String, StorageHealthMonitor>) {

    /**
     * 监控健康检查线程池
     * */
    private val executorService = ThreadPoolExecutor(
        0, Runtime.getRuntime().availableProcessors(),
        60L, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(1024)
    )

    /**
     * 获取存储相对应的监控
     * */
    fun getMonitor(properties: StorageProperties, storageCredentials: StorageCredentials): StorageHealthMonitor {
        val location = storageCredentials.upload.location
        return monitorMap[location] ?: synchronized(location.intern()) {
            monitorMap[location]?.let { return it }
            val storageHealthMonitor = StorageHealthMonitor(properties, location, executorService)
            monitorMap.putIfAbsent(
                location,
                storageHealthMonitor
            )
            storageHealthMonitor
        }
    }

    /**
     * 获取目前所有的监控
     * */
    fun all(): List<StorageHealthMonitor> {
        return monitorMap.values.toList()
    }
}
