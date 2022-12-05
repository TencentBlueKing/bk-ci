package com.tencent.bkrepo.common.storage.monitor

import com.tencent.bkrepo.common.storage.config.UploadProperties
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class StorageHealthMonitorHelperTest {
    private val monitorHelper = StorageHealthMonitorHelper(ConcurrentHashMap())

    @Test
    fun concurrentGet() {
        val storageProperties = StorageProperties()
        val storageCredentials = storageProperties.defaultStorageCredentials()
        val countDownLatch = CountDownLatch(10)
        repeat(10) {
            thread {
                monitorHelper.getMonitor(storageProperties, storageCredentials)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()
        Assertions.assertEquals(1, monitorHelper.all().size)
        val otherStorageCredentials = StorageCredentials(
            upload = UploadProperties(location = storageCredentials.upload.location.plus("temp")),
            cache = storageCredentials.cache
        )
        val m1 = monitorHelper.getMonitor(storageProperties, otherStorageCredentials)
        val m2 = monitorHelper.getMonitor(storageProperties, otherStorageCredentials)
        Assertions.assertEquals(2, monitorHelper.all().size)
        Assertions.assertEquals(true, m1 == m2)
    }
}
