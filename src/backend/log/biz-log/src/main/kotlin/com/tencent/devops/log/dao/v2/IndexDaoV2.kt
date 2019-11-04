/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.log.dao.v2

import com.tencent.devops.model.log.tables.TLogIndicesV2
import com.tencent.devops.model.log.tables.records.TLogIndicesV2Record
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import sun.misc.MessageUtils.where
import java.time.LocalDateTime

@Repository
class IndexDaoV2 {

    fun create(
        dslContext: DSLContext,
        buildId: String,
        indexName: String,
        enable: Boolean
    ) {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                BUILD_ID,
                INDEX_NAME,
                LAST_LINE_NUM,
                CREATED_TIME,
                UPDATED_TIME,
                ENABLE
                )
                .values(
                    buildId,
                    indexName,
                    1,
                    now,
                    now,
                    enable
                )
                .onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun getBuild(dslContext: DSLContext, buildId: String): TLogIndicesV2Record? {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }

    fun getIndexName(
        dslContext: DSLContext,
        buildId: String
    ): String? {
        return getBuild(dslContext, buildId)?.indexName
    }

    fun updateLastLineNum(
        dslContext: DSLContext,
        buildId: String,
        size: Int
    ): Long? {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .forUpdate()
                    .fetchOne() ?: return@transactionResult null
                if (record.lastLineNum == null || record.lastLineNum <= 0L) {
                    record.lastLineNum = 1L
                }
                val result = record.lastLineNum + size
                val updated = context.update(this)
                    .set(LAST_LINE_NUM, result)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
                if (updated != 1) {
                    logger.warn("[$buildId|$size|$result|$updated] Fail to update the build last line num")
                }
                record.lastLineNum
            }
        }
    }

    fun listLatestBuilds(
        dslContext: DSLContext,
        limit: Int
    ): Result<TLogIndicesV2Record> {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.selectFrom(this)
                .orderBy(ID.desc())
                .limit(limit)
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildIds: Set<String>
    ): Int {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexDaoV2::class.java)
    }
}