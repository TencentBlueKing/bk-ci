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
import com.google.common.cache.CacheLoader
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

    // The cache store the bad ES
    private val disconnectESCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build<String/*ES Name*/, Boolean>(
                    object: CacheLoader<String, Boolean>() {
                        override fun load(esName: String): Boolean {
                            return getDisconnectESFromRedis().contains(esName)
                        }
                    }
            )

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
        val activeClients = client.filter { disconnectESCache.get(it.name) }
        val activeESNames = activeClients.map { it.name }.toSet()
        if (activeClients.isEmpty()) {
            logger.warn("All clients(${client.map { it.name }}) are not active, return the first one")
            return client.first()
        }
        var esName = cache.getIfPresent(buildId)
        if (!activeESNames.contains(esName)) {
            logger.warn("The es($esName|$buildId) is not be active any more, retry other clients")
            cache.invalidate(buildId)
            esName = null
        }
        if (esName.isNullOrBlank()) {
            val redisLock = RedisLock(redisOperation, "$MULTI_LOG_CLIENT_LOCK_KEY:$buildId", 10)
            try {
                redisLock.lock()
                esName = cache.getIfPresent(buildId)
                if (esName.isNullOrBlank()) {
                    esName = tencentIndexDao.getClusterName(dslContext, buildId)
                    if (esName.isNullOrBlank()) {
                        // hash from build
                        val c = activeClients[hashBuildId(buildId, activeClients.size)]
                        esName = c.name
                    } else {
                        // set to cache
                        logger.info("[$buildId] The build ID already bind to the ES: ($esName)")
                    }
                }

                if (!activeESNames.contains(esName)) {
                    esName = activeClients[hashBuildId(buildId, activeClients.size)].name
                }
                tencentIndexDao.updateClusterName(dslContext, buildId, esName!!)
                cache.put(buildId, esName!!)
            } finally {
                redisLock.unlock()
            }
        }
        activeClients.forEach {
            if (it.name == esName) {
                return it
            }
        }
        logger.warn("[$buildId] Fail to get the es name for the build, return the first one")
        return client.first()
    }

    private fun hashBuildId(buildId: String, size: Int): Int {
        if (size == 1) {
            return 0
        }
        return abs(buildId.hashCode()) % size
    }

    private fun getDisconnectESFromRedis(): Set<String> {
        return redisOperation.getSetMembers(MULTI_LOG_CLIENT_BAD_ES_KEY) ?: emptySet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MultiESLogClient::class.java)
        private const val MULTI_LOG_CLIENT_LOCK_KEY = "log:multi:log:client:lock:key"
        private const val MULTI_LOG_CLIENT_BAD_ES_KEY = "log::multi::log:client:bad:es:key"
    }
}