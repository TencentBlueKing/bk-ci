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

package com.tencent.devops.log.service.v2

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.v2.IndexDaoV2
import com.tencent.devops.log.dao.v2.LogStatusDaoV2
import com.tencent.devops.log.model.v2.IndexAndType
import com.tencent.devops.log.util.IndexNameUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class IndexServiceV2 @Autowired constructor(
    private val dslContext: DSLContext,
    private val indexDaoV2: IndexDaoV2,
    private val logStatusDaoV2: LogStatusDaoV2,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(IndexServiceV2::class.java)
    }

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*IndexName*/>(
            object : CacheLoader<String, String>() {
                override fun load(buildId: String): String {
                    return dslContext.transactionResult { configuration ->
                        val context = DSL.using(configuration)
                        var indexName = indexDaoV2.getIndexName(context, buildId)
                        if (indexName.isNullOrBlank()) {
                            val redisLock = RedisLock(redisOperation, "log:build:enable:lock:key", 10)
                            redisLock.lock()
                            try {
                                indexName = indexDaoV2.getIndexName(context, buildId)
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
        indexDaoV2.create(dslContext, buildId, indexName, true)
        logger.info("[$buildId|$indexName] Create new index/type in db and cache")
        return indexName
    }

    fun getIndexAndType(buildId: String): IndexAndType {
        val index = indexCache.get(buildId)
        if (index.isNullOrBlank()) {
            throw OperationException("Fail to get the index of build $buildId")
        }
        return IndexAndType(index!!, index)
    }

    fun getAndAddLineNum(buildId: String, size: Int): Long? {
        val startLineNum = indexDaoV2.updateLastLineNum(dslContext, buildId, size)
        if (startLineNum == null) {
            logger.warn("[$buildId|$size] Fail to get and add the line num")
            return null
        }
        return startLineNum
    }

    fun finish(buildId: String, tag: String?, jobId: String?, executeCount: Int?, finish: Boolean) {
        logStatusDaoV2.finish(dslContext, buildId, tag, jobId, executeCount, finish)
    }

    fun isFinish(buildId: String, tag: String?, jobId: String?, executeCount: Int?): Boolean {
        return if (jobId.isNullOrBlank()) {
            logStatusDaoV2.isFinish(dslContext, buildId, tag, executeCount)
        } else {
            val logStatusList = logStatusDaoV2.listFinish(dslContext, buildId, tag, executeCount)
            logStatusList?.firstOrNull { it.jobId == jobId && it.tag.startsWith("stopVM-") }?.finished == true
        }
    }
}