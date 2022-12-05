/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.artifact.stream.ZeroInputStream
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.message.HealthCheckFailedException
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 健康检查操作实现类
 */
abstract class HealthCheckSupport : AbstractStorageSupport() {

    private val healthCheckExecutor = Executors.newSingleThreadExecutor()

    override fun checkHealth(storageCredentials: StorageCredentials?) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val future = healthCheckExecutor.submit(Callable { doCheckHealth(credentials) })
        try {
            future.get(storageProperties.monitor.timeout.seconds, TimeUnit.SECONDS)
        } catch (ignored: TimeoutException) {
            throw HealthCheckFailedException(StorageHealthMonitor.IO_TIMEOUT_MESSAGE)
        } catch (ignored: Exception) {
            throw HealthCheckFailedException(ignored.message.orEmpty())
        }
    }

    /**
     * 健康检查实现
     */
    open fun doCheckHealth(credentials: StorageCredentials) {
        val filename = System.nanoTime().toString()
        val size = storageProperties.monitor.dataSize.toBytes()
        val inputStream = ZeroInputStream(size)
        fileStorage.store(HEALTH_CHECK_PATH, filename, inputStream, size, credentials)
        fileStorage.delete(HEALTH_CHECK_PATH, filename, credentials)
    }

    companion object {
        private const val HEALTH_CHECK_PATH = "/health-check"
    }
}
