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

package com.tencent.devops.log.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.util.IndexNameUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.concurrent.TimeUnit

@Suppress("NestedBlockDepth")
@Service
class IndexService @Autowired constructor(
    private val dslContext: DSLContext,
    private val indexDao: IndexDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(IndexService::class.java)
        private const val LOG_INDEX_LOCK = "log:build:enable:lock:key"
        private const val LOG_LINE_NUM = "log:build:line:num:"
        private const val LOG_LINE_NUM_LOCK = "log:build:line:num:distribute:lock"
        private const val INDEX_CACHE_MAX_SIZE = 100000L
        private const val INDEX_CACHE_EXPIRE_MINUTES = 30L
        private const val INDEX_LOCK_EXPIRE_SECONDS = 10L
        fun getLineNumRedisKey(buildId: String) = LOG_LINE_NUM + buildId
    }

    private val indexCache = Caffeine.newBuilder()
        .maximumSize(INDEX_CACHE_MAX_SIZE)
        .expireAfterAccess(INDEX_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*IndexName*/> { buildId ->
            dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                var indexName = indexDao.getIndexName(context, buildId)
                if (indexName.isNullOrBlank()) {
                    val redisLock = RedisLock(
                        redisOperation = redisOperation,
                        lockKey = "$LOG_INDEX_LOCK:$buildId",
                        expiredTimeInSeconds = INDEX_LOCK_EXPIRE_SECONDS
                    )
                    redisLock.lock()
                    try {
                        indexName = indexDao.getIndexName(context, buildId)
                        if (indexName.isNullOrBlank()) {
                            logger.info("[$buildId] Add the build record")
                            indexName = saveIndex(buildId)
                        }
                    } finally {
                        redisLock.unlock()
                    }
                }
                indexName!!
            }
        }

    private fun saveIndex(buildId: String): String {
        val indexName = IndexNameUtils.getIndexName()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            indexDao.create(context, buildId, indexName, true)
            redisOperation.set(
                getLineNumRedisKey(buildId), 1.toString(), TimeUnit.DAYS.toSeconds(2)
            )
        }
        logger.info("[$buildId|$indexName] Create new index in db and cache")
        return indexName
    }

    fun getIndexName(buildId: String): String {
        val index = indexCache.get(buildId)
        if (index.isNullOrBlank()) {
            throw OperationException("Fail to get the index of build $buildId")
        }
        return index
    }

    fun getAndAddLineNum(buildId: String, size: Int): Long? {
        RedisLock(redisOperation, "$LOG_LINE_NUM_LOCK:$buildId", 10).use { lock ->
            // 获得并发锁时才能读取db或修改redis缓存
            lock.lock()
            var lineNum = redisOperation.get(getLineNumRedisKey(buildId))?.toLong()
            // 缓存命中则直接进行自增，缓存未命中则从db中取值，自增后再刷新缓存
            if (lineNum == null) {
                logger.warn("[$buildId|$size] Fail to get and add the line num, get from db")
                val lastLineNum = indexDao.getBuild(dslContext, buildId)?.lastLineNum ?: run {
                    logger.warn("[$buildId|$size] The build is not exist in db")
                    return null
                }
                logger.warn("[$buildId|$size] Got from db, lastLineNum: $lastLineNum")
                lineNum = lastLineNum + size.toLong()
                redisOperation.set(getLineNumRedisKey(buildId), lineNum.toString(), TimeUnit.DAYS.toSeconds(2))
            } else {
                lineNum = redisOperation.increment(getLineNumRedisKey(buildId), size.toLong())
            }
            return lineNum!! - size
        }
    }

    fun getBuildIndexName(buildId: String): String? {
        return indexDao.getBuild(dslContext, buildId)?.indexName
    }

    fun getLastLineNum(buildId: String): Long {
        return redisOperation.get(getLineNumRedisKey(buildId))?.toLong()
            ?: indexDao.getBuild(dslContext, buildId)?.lastLineNum ?: 0
    }

    fun flushLineNum2DB(buildId: String) {
        val lineNum = redisOperation.get(getLineNumRedisKey(buildId))
        if (lineNum.isNullOrBlank()) {
            logger.warn("[$buildId] Fail to get lineNum from redis")
            return
        }
        val latestLineNum = try {
            lineNum.toLong()
        } catch (ignore: Exception) {
            logger.warn("[$buildId|$lineNum] Fail to convert line num to long", ignore)
            return
        }
        val updateCount = indexDao.updateLastLineNum(dslContext, buildId, latestLineNum)
        if (updateCount == 1) {
            redisOperation.delete(getLineNumRedisKey(buildId))
        } else {
            logger.warn("[$buildId|$latestLineNum] Fail to update the build latest line num")
        }
    }
}
