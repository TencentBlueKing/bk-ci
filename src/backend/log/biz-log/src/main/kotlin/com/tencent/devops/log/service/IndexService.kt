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
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.model.LogIndex
import org.apache.commons.lang3.tuple.Pair
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 *
 * Powered By Tencent
 */
@Service
class IndexService @Autowired constructor(
    private val dslContext: DSLContext,
    private val indexDao: IndexDao
) {
    private val logger = LoggerFactory.getLogger(IndexService::class.java)
    private val indexMap = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*buildId*/, String/*indexName*/>(
            object : CacheLoader<String, String>() {
                override fun load(buildId: String): String {
                    try {
                        return dslContext.transactionResult { configuration ->
                            val context = DSL.using(configuration)
                            val optionalIndexName = indexDao.getIndexName(context, buildId)
                            if (optionalIndexName.isPresent) {
                                optionalIndexName.get()
                            } else {
                                saveIndexAndType(context, buildId)
                            }
                        }
                    } catch (ignored: Throwable) {
                        logger.warn("Fail to get the index of build $buildId", ignored)
                        throw ignored
                    }
                }
            }
        )

    fun parseIndexAndType(buildId: String): Pair<String, String> {
        return Pair.of(indexMap.get(buildId), buildId)
    }

    private fun saveIndexAndType(dslContext: DSLContext, buildId: String): String {
        val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd-HH")
        val indexName = "log-" + formatter.format(LocalDateTime.now())
        if (!indexDao.saveIndexName(dslContext, buildId, indexName)) {
            logger.warn("[$buildId|$indexName] Fail to save the index name")
        }
        logger.info(String.format("Create new index/type(%s/%s) in db and cache", indexName, buildId))
        return indexName
    }

    fun getAndAddLineNum(buildId: String, addLineNum: Long): LogIndex? {
        var record = indexDao.getAndAddLineNumOrNull(dslContext, buildId, addLineNum)
        record?.let {
            return LogIndex().apply {
                id = buildId
                indexName = it.indexName
                lastLineNum = it.lastLineNum
            }
        }
        // 记录不存在，需要插入数据
        saveIndexAndType(dslContext, buildId)

        record = indexDao.getAndAddLineNumOrNull(dslContext, buildId, addLineNum)
        record?.let {
            return LogIndex().apply {
                id = buildId
                indexName = it.indexName
                lastLineNum = it.lastLineNum
            }
        } ?: return null
    }

    fun getLineNum(buildId: String): LogIndex? {
        val record = indexDao.getIndex(dslContext, buildId) ?: return null
        return LogIndex(buildId, record.indexName, record.lastLineNum)
    }

    fun isTypeMappingCreate(buildId: String): Boolean {
        return indexDao.getIndex(dslContext, buildId)?.createTypeMapping ?: false
    }

    fun queryIndex(buildId: String): String {
        return indexMap.get(buildId)
    }
}
