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

package com.tencent.devops.log.dao

import com.tencent.devops.model.log.tables.TLogIndices
import com.tencent.devops.model.log.tables.records.TLogIndicesRecord
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 *
 * Powered By Tencent
 */
@Repository
class IndexDao {

    private val logger = LoggerFactory.getLogger(IndexDao::class.java)

    fun getIndexName(dslContext: DSLContext, buildId: String): Optional<String> {
        with(TLogIndices.T_LOG_INDICES) {
            return try {
                val record = dslContext.selectFrom(TLogIndices.T_LOG_INDICES)
                    .where(TLogIndices.T_LOG_INDICES.ID.eq(buildId))
                    .forUpdate()
                    .fetchOne()
                if (record == null)
                    Optional.empty()
                else
                    Optional.of(record.indexName)
            } catch (e: DataAccessException) {
                logger.error("Query db failure", e)
                Optional.empty()
            }
        }
    }

    fun saveIndexName(dslContext: DSLContext, buildId: String, indexName: String): Boolean {
        with(TLogIndices.T_LOG_INDICES) {
            return try {
                dslContext.insertInto(this, ID, INDEX_NAME, CREATE_TYPE_MAPPING)
                    .values(buildId, indexName, true)
                    .onDuplicateKeyIgnore()
                    .execute()
                true
            } catch (e: DataAccessException) {
                logger.error("Save db failure", e)
                false
            }
        }
    }

    fun getIndex(dslContext: DSLContext, buildId: String): TLogIndicesRecord? {
        with(TLogIndices.T_LOG_INDICES) {
            return dslContext.selectFrom(this)
                .where(ID.eq(buildId))
                .fetchOne()
        }
    }

    fun getAndAddLineNumOrNull(dslContext: DSLContext, buildId: String, addLineNum: Long): TLogIndicesRecord? {

        with(TLogIndices.T_LOG_INDICES) {
            return dslContext.transactionResult { configuration ->
                val record = DSL.using(configuration)
                    .selectFrom(this)
                    .where(ID.eq(buildId))
                    .forUpdate()
                    .fetchOne()

                if (record != null) {
                    if (record.lastLineNum == null || record.lastLineNum <= 0) {
                        record.lastLineNum = 1L
                    }
                    val updated = DSL.using(configuration)
                        .update(this)
                        .set(LAST_LINE_NUM, record.lastLineNum!! + addLineNum)
                        .where(ID.eq(buildId))
                        .execute()

                    if (updated == 0) {
                        logger.error("Update table LogIndices lastLineNum failed, buildId: $buildId")
                    }
                }

                record
            }
        }
    }
}