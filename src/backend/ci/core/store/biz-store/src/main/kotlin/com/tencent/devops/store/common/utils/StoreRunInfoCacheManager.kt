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

package com.tencent.devops.store.common.utils

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 组件运行时信息 Redis 缓存。
 * Key 由 [StoreUtils.getStoreRunInfoKey] 生成，写入后按配置设置过期时间。
 */
@Component
class StoreRunInfoCacheManager @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    @Value("\${store.atom.runInfo.cacheExpireSeconds:300}")
    private val cacheExpireSeconds: Long = DEFAULT_EXPIRE_SECONDS

    /**
     * 写入插件运行时缓存并刷新 Key 过期时间。
     */
    fun setAtomRunInfo(atomCode: String, version: String, values: String) {
        val key = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
        redisOperation.hset(key, version, values)
        expire(key)
    }

    /**
     * 按插件编码精确删除运行时缓存，不扫描 Redis。
     * @return 删除的 key 数量
     */
    fun deleteAtomRunInfoCache(atomCodes: Collection<String>): Int {
        val keys = atomCodes.filter { it.isNotBlank() }
            .distinct()
            .map { StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, it) }
        deleteKeys(keys)
        logger.info("delete atom run info cache, size=${keys.size}")
        return keys.size
    }

    private fun deleteKeys(keys: Collection<String>) {
        if (keys.isEmpty()) {
            return
        }
        keys.chunked(DELETE_BATCH_SIZE).forEach { batch ->
            redisOperation.delete(batch)
        }
    }

    private fun expire(key: String) {
        if (cacheExpireSeconds > 0) {
            redisOperation.expire(key, cacheExpireSeconds)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreRunInfoCacheManager::class.java)
        private const val DEFAULT_EXPIRE_SECONDS = 300L
        private const val DELETE_BATCH_SIZE = 200
    }
}
