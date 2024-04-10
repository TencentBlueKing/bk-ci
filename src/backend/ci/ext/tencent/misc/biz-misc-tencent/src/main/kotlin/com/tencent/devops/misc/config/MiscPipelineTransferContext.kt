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

package com.tencent.devops.misc.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MiscPipelineTransferContext @Autowired constructor(private val redisOperation: RedisOperation) {

    companion object {
        private const val LOCK_KEY = "misc:pipeline:transfer:lock"
        private const val FINISH_PROJECT_SET_KEY = "misc:pipeline:transfer:finishSet"
        private const val TRANSFER_PROJECT_CHANNELS_KEY = "misc:pipeline:transfer:channels"
        private const val TRANSFER_PROJECT_LIST_KEY = "misc:pipeline:transfer:project:list"
        private const val TRANSFER_PROJECT_ID_KEY = "misc:pipeline:transfer:project:id"
        private const val TRANSFER_PROJECT_BATCH_SIZE_KEY = "misc:pipeline:transfer:project:batchSize"
        private const val expiredTimeInSeconds: Long = 360000
    }

    private val finishSetKey = redisOperation.getRedisName() + ":" + FINISH_PROJECT_SET_KEY
    private val channelKey = redisOperation.getRedisName() + ":" + TRANSFER_PROJECT_CHANNELS_KEY
    private val projectListKey = redisOperation.getRedisName() + ":" + TRANSFER_PROJECT_LIST_KEY
    private val projectIdKey = redisOperation.getRedisName() + ":" + TRANSFER_PROJECT_ID_KEY
    private val batchSizeKey = redisOperation.getRedisName() + ":" + TRANSFER_PROJECT_BATCH_SIZE_KEY

    @Value("\${misc.pipeline.transfer.enable:false}")
    private val enable: String = "false"

    private val lock = RedisLock(redisOperation, LOCK_KEY, expiredTimeInSeconds)

    private val intCache: LoadingCache<String, Int> = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(100)
        .build(object : CacheLoader<String, Int>() {
            override fun load(key: String): Int {
                val str = redisOperation.get(key) ?: "0"
                if (!str.matches("^[0-9]+$".toRegex())) {
                    return 0
                }
                return str.toInt()
            }
        })

    private val channelCache: LoadingCache<String, Boolean> = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10)
        .build(object : CacheLoader<String, Boolean>() {
            override fun load(key: String): Boolean {
                val channelSet = mutableSetOf<String>()
                val channels = redisOperation.get(channelKey) ?: "CODECC"
                channelSet.addAll(channels.split(","))
                return channelSet.contains(key)
            }
        })

    fun enable(): Boolean = enable.toBoolean()

    fun tryLock(): Boolean = lock.tryLock()

    fun unLock(): Boolean = lock.unlock()

    /**
     * return p1,p2,p3
     */
    fun needTransferProjectIdList(): List<String> {
        val projectIdListConfig = redisOperation.get(projectListKey)
        if (!projectIdListConfig.isNullOrBlank()) {
            return projectIdListConfig.split(",")
        }
        return emptyList()
    }

    /**
     * return Long
     */
    fun getLastTransferProjectSeqId(): Long? = redisOperation.get(projectIdKey)?.toLong()

    fun dealProjectBatchSize(): Int = intCache.get(batchSizeKey).coerceAtLeast(1)

    fun setLastProjectSeqId(maxHandleProjectPrimaryId: Long) {
        redisOperation.set(projectIdKey, maxHandleProjectPrimaryId.toString())
    }

    fun checkTransferChannel(channel: String): Boolean = channelCache.get(channel)

    fun addFinishProject(projectId: String) {
        redisOperation.sadd(finishSetKey, projectId)
    }

    fun isFinishProject(projectId: String): Boolean = redisOperation.isMember(finishSetKey, projectId)
}
