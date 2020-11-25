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
package com.tencent.devops.lambda.storage

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.dao.LambdaBuildIndexDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class IndexService @Autowired constructor(
    private val dslContext: DSLContext,
    private val lambdaBuildIndexDao: LambdaBuildIndexDao,
    private val redisOperation: RedisOperation
) {
    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*buildId*/, String>()

    fun getIndex(buildId: String): String {
        var index = indexCache.getIfPresent(buildId)
        if (index != null) {
            return index
        }
        index = getBuildIndexDB(buildId)
        if (index != null) {
            indexCache.put(buildId, index)
            return index
        }
        val lock = RedisLock(redisOperation, "$ES_INDEX_LOCK:$buildId", 10)
        try {
            lock.lock()
            index = getBuildIndexDB(buildId)
            if (index != null) {
                indexCache.put(buildId, index)
                return index
            }

            index = getIndexName()
            lambdaBuildIndexDao.create(dslContext, buildId, index)
            indexCache.put(buildId, index)
            return index
        } finally {
            lock.unlock()
        }
    }

    fun updateTime(buildId: String, beginTime: Long, endTime: Long) {
        lambdaBuildIndexDao.update(dslContext, buildId, beginTime, endTime)
    }

    fun getIndexName(date: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern(LAMBDA_INDEX_DATE_FORMAT)
        return LAMBDA_INDEX_PREFIX + formatter.format(date)
    }

    private fun getBuildIndexDB(buildId: String): String? {
        return lambdaBuildIndexDao.get(dslContext, buildId)?.indexName
    }

    private fun getIndexName(): String {
        val formatter = DateTimeFormatter.ofPattern(LAMBDA_INDEX_DATE_FORMAT)
        return LAMBDA_INDEX_PREFIX + formatter.format(LocalDateTime.now())
    }

    companion object {
        private const val ES_INDEX_LOCK = "lambda:es:index:lock:key"
        private const val LAMBDA_INDEX_PREFIX = "lambda-build-"
        private const val LAMBDA_INDEX_DATE_FORMAT = "YYYY-MM-dd"
    }
}