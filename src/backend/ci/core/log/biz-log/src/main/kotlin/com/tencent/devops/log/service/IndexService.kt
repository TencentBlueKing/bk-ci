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

package com.tencent.devops.log.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.model.IndexAndType
import com.tencent.devops.log.util.IndexNameUtils
import com.tencent.devops.log.util.IndexNameUtils.getTypeByIndex
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.concurrent.TimeUnit

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
        private const val LOG_LINE_NUM_LOCK = "log:build:line:num:distribute:lock:"
        fun getLineNumRedisKey(buildId: String) = LOG_LINE_NUM + buildId
    }

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*IndexName*/>(
            object : CacheLoader<String, String>() {
                override fun load(buildId: String): String {
                    return dslContext.transactionResult { configuration ->
                        val context = DSL.using(configuration)
                        var indexName = indexDao.getIndexName(context, buildId)
                        if (indexName.isNullOrBlank()) {
                            val redisLock = RedisLock(redisOperation, LOG_INDEX_LOCK, 10)
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
            }
        )

    private fun saveIndex(buildId: String): String {
        val indexName = IndexNameUtils.getIndexName()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            indexDao.create(context, buildId, indexName, true)
            redisOperation.set(getLineNumRedisKey(buildId), 1.toString(), TimeUnit.DAYS.toSeconds(2))
        }
        logger.info("[$buildId|$indexName] Create new index/type in db and cache")
        return indexName
    }

    fun getIndexAndType(buildId: String): IndexAndType {
        val index = indexCache.get(buildId)
        if (index.isNullOrBlank()) {
            throw OperationException("Fail to get the index of build $buildId")
        }
        return IndexAndType(index!!, getTypeByIndex(index))
    }

    fun getAndAddLineNum(buildId: String, size: Int): Long? {
        var lineNum = redisOperation.increment(getLineNumRedisKey(buildId), size.toLong())
        // val startLineNum = indexDaoV2.updateLastLineNum(dslContext, buildId, size)
        if (lineNum == null) {
            val redisLock = RedisLock(redisOperation, LOG_LINE_NUM_LOCK + buildId, 10)
            try {
                redisLock.lock()
                lineNum = redisOperation.increment(getLineNumRedisKey(buildId), size.toLong())
                if (lineNum == null) {
                    logger.warn("[$buildId|$size] Fail to get and add the line num, get from db")
                    val build = indexDao.getBuild(dslContext, buildId)
                    if (build == null) {
                        logger.warn("[$buildId|$size] The build is not exist in db")
                        return null
                    }
                    lineNum = build.lastLineNum + size.toLong()
                    redisOperation.set(getLineNumRedisKey(buildId), lineNum.toString(), TimeUnit.DAYS.toSeconds(2))
                }
            } finally {
                redisLock.unlock()
            }
        }
        return lineNum!! - size
    }

    fun flushLineNum2DB(buildId: String) {
        val lineNum = redisOperation.get(getLineNumRedisKey(buildId))
        if (lineNum.isNullOrBlank()) {
            logger.warn("[$buildId] Fail to get lineNum from redis")
            return
        }
        val latestLineNum = try {
            lineNum!!.toLong()
        } catch (e: Exception) {
            logger.warn("[$buildId|$lineNum] Fail to convert line num to long", e)
            return
        }
        val updateCount = indexDao.updateLastLineNum(dslContext, buildId, latestLineNum)
        if (updateCount != 1) {
            logger.warn("[$buildId|$latestLineNum] Fail to update the build latest line num")
        }
    }
}