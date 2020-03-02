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
import com.tencent.devops.log.client.CurrentLogClient
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.dao.TencentIndexDao
import com.tencent.devops.log.dao.v2.IndexDaoV2
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MultiESLogClient constructor(
    private val clients: List<ESClient>,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val tencentIndexDao: TencentIndexDao,
    private val indexDaoV2: IndexDaoV2
) : LogClient {

    init {
        val names = clients.map { it.name }.toSet()
        if (names.size != clients.size) {
            logger.warn("There are same es names between es cluster")
            throw RuntimeException("There are same es names between es cluster")
        }
    }

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(300000)
        .expireAfterWrite(2, TimeUnit.DAYS)
        .build<String/*buildId*/, String/*ES NAME*/>()

    // The cache store the bad ES
    private val inactiveESCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build<String/*ES Name*/, Boolean>(
                    object: CacheLoader<String, Boolean>() {
                        override fun load(esName: String): Boolean {
                            return getInactiveESFromRedis().contains(esName)
                        }
                    }
            )

    override fun getActiveClients(): List<ESClient> {
        return clients.filter { !inactiveESCache.get(it.name) }
    }

    override fun markESInactive(buildId: String) {
        val client = CurrentLogClient.getClient()
        if (client == null) {
            logger.warn("[$buildId] Fail to get the es client")
            return
        }
        logger.warn("[$buildId|${client.name}] Mark the es as inactive")
        setInactiveES(client.name)
    }

    override fun markESActive(buildId: String) {
        val esName = CurrentLogClient.getInactiveESName()
        logger.info("[$buildId|$esName] Mark the es as active")
        if (!esName.isNullOrBlank()) {
            removeInactiveES(esName!!)
        }
    }

    /**
     * 1. Get es name from local cache
     * 2. If local cache is not exist, then try to get from DB
     * 3. If DB is not exist, then hash the build id to the ESClients
     * 4.
     */
    override fun hashClient(buildId: String): ESClient {
        val inactiveESName = CurrentLogClient.getInactiveESName()
        if (!inactiveESName.isNullOrBlank()) {
            val client = getClientByName(inactiveESName!!)
            if (client == null) {
                logger.warn("[$buildId|$inactiveESName] Fail to find the es client")
                CurrentLogClient.setInactiveESName(null)
            } else {
                return client
            }
        }
        val activeClients = getActiveClients()
        if (activeClients.isEmpty()) {
            logger.warn("All client is deactive, try to use the first one")
            if (clients.isEmpty()) {
                throw RuntimeException("Empty es clients")
            }
            return mainCluster()
        }
        val activeESNames = activeClients.map { it.name }.toSet()
        var esName = cache.getIfPresent(buildId)
        if (!esName.isNullOrBlank()) {
            synchronized(this) {
                if (!activeESNames.contains(esName)) {
                    logger.warn("The es($esName|$buildId) is not be active any more, retry other clients")
                    cache.invalidate(buildId)
                    esName = null
                }
            }
        }
        if (esName.isNullOrBlank()) {
            val redisLock = RedisLock(redisOperation, "$MULTI_LOG_CLIENT_LOCK_KEY:$buildId", 10)
            try {
                redisLock.lock()
                esName = cache.getIfPresent(buildId)
                if (esName.isNullOrBlank()) {
                    // 兼容老的日志， 如果这个日志之前已经被写入了， 那么默认返回mainCluster对应的集群的数据， 要不然就会导致前端查询不到数据
                    val buildIndex = indexDaoV2.getBuild(dslContext, buildId)
                    if (buildIndex == null || (!buildIndex.useCluster)) {
                        val c = mainCluster()
                        cache.put(buildId, c.name)
                        return c
                    }
                    esName = buildIndex.logClusterName
                    if (esName.isNullOrBlank() || (!activeESNames.contains(esName!!))) {
                        // hash from build
                        logger.info("[$buildId|$esName] Rehash the build id")
                        val c = getClient(activeClients, buildId)
                        esName = c.name
                        logger.info("[$buildId] Set the build id to es log cluster: $esName")
                        tencentIndexDao.updateClusterName(dslContext, buildId, esName!!)
                    } else {
                        // set to cache
                        logger.info("[$buildId] The build ID already bind to the ES: ($esName)")
                    }
                    cache.put(buildId, esName!!)
                }

            } finally {
                redisLock.unlock()
            }
        }
        activeClients.forEach {
            if (it.name == esName) {
                return it
            }
        }
        logger.warn("[$buildId|$esName] Fail to get the es name for the build, return the first one")
        return mainCluster()
    }

    private fun getClient(activeClients: List<ESClient>, buildId: String) =
            activeClients[hashBuildId(buildId, activeClients.size)]

    private fun hashBuildId(buildId: String, size: Int): Int {
        if (size == 1) {
            return 0
        }
        return abs(buildId.hashCode()) % size
    }

    private fun getInactiveESFromRedis(): Set<String> {
        val tmp = redisOperation.getSetMembers(MULTI_LOG_CLIENT_BAD_ES_KEY)
        logger.info("Get the inactive es: $tmp")
        return tmp ?: emptySet()
    }
    
    private fun setInactiveES(esName: String) {
        redisOperation.addSetValue(MULTI_LOG_CLIENT_BAD_ES_KEY, esName)
    }

    private fun removeInactiveES(esName: String) {
        redisOperation.removeSetMember(MULTI_LOG_CLIENT_BAD_ES_KEY, esName)
    }

    private fun getClientByName(esName: String): ESClient? {
        clients.forEach {
            if (it.name == esName) {
                return it
            }
        }
        return null
    }

    private fun mainCluster(): ESClient {
        clients.forEach {
            if (it.mainCluster == true) {
                return it
            }
        }
        return clients.first()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MultiESLogClient::class.java)
        private const val MULTI_LOG_CLIENT_LOCK_KEY = "log:multi:log:client:lock:key"
        private const val MULTI_LOG_CLIENT_BAD_ES_KEY = "log::multi::log:client:bad:es:key"
    }
}