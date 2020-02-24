/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.client.impl

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.dao.TencentIndexDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MultiESLogClient constructor(
    private val clients: List<ESClient>,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val tencentIndexDao: TencentIndexDao
) : LogClient {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(300000)
        .expireAfterWrite(2, TimeUnit.DAYS)
        .build<String/*buildId*/, String/*ES NAME*/>()

    override fun getClients(): List<ESClient> {
        return clients
    }

    /**
     * 1. Get es name from local cache
     * 2. If local cache is not exist, then try to get from DB
     * 3. If DB is not exist, then hash the build id to the ESClients
     * 4.
     */
    override fun hashClient(buildId: String, client: List<ESClient>): ESClient {
        if (client.isEmpty()) {
            throw RuntimeException("Fail to get the log client")
        }
        var esName = cache.getIfPresent(buildId)
        if (esName.isNullOrBlank()) {
            val redisLock = RedisLock(redisOperation, "$MULTI_LOG_CLIENT_LOCK_KEY:$buildId", 10)
            try {
                redisLock.lock()
                esName = tencentIndexDao.getClusterName(dslContext, buildId)
                if (esName.isNullOrBlank()) {
                    // hash from build
                } else {
                    // set to cache
                    logger.info("[$buildId] The build ID already bind to the ES: ($esName)")
                    cache.put(buildId, esName!!)
                }


            } finally {
                redisLock.unlock()
            }
        }
        return client.first()
    }

    private fun hashBuildId(buildId: String, size: Int): Int {
        if (size == 1) {
            return 0
        }
        return abs(buildId.hashCode()) % size
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MultiESLogClient::class.java)
        private const val MULTI_LOG_CLIENT_LOCK_KEY = "log:multi:log:client:lock:key"
    }
}