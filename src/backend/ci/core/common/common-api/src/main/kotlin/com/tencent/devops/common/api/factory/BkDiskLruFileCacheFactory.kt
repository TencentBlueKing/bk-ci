/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.factory

import com.tencent.devops.common.api.cache.BkDiskLruFileCache
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory

object BkDiskLruFileCacheFactory {

    private val logger = LoggerFactory.getLogger(BkDiskLruFileCacheFactory::class.java)
    private const val RETRY_NUM = 3
    private const val RETRY_INTERVAL_TIME = 1000L

    fun getDiskLruFileCache(
        cacheDir: String,
        cacheSize: Long
    ): BkDiskLruFileCache {
        return createBkDiskLruFileCache(cacheDir, cacheSize, RETRY_NUM)
    }

    private fun createBkDiskLruFileCache(
        cacheDir: String,
        cacheSize: Long,
        retryNum: Int
    ): BkDiskLruFileCache {
        return try {
            BkDiskLruFileCache(cacheDir, cacheSize)
        } catch (ignored: Throwable) {
            logger.warn(
                "createBkDiskLruFileCache fail, retryNum: $retryNum, Cause of error: ${ignored.message}", ignored
            )
            if (retryNum == 0) {
                // 达到最大重试次数则抛出异常
                throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
            }
            // 创建磁盘缓存对象出现异常则一秒后进行重试
            Thread.sleep(RETRY_INTERVAL_TIME)
            createBkDiskLruFileCache(cacheDir, cacheSize, retryNum - 1)
        }
    }
}
