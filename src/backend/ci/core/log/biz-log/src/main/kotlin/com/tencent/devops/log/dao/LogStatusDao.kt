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

package com.tencent.devops.log.dao

import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.model.log.tables.TLogStatus
import com.tencent.devops.model.log.tables.records.TLogStatusRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.springframework.stereotype.Repository

@Suppress("LongParameterList")
@Repository
class LogStatusDao {

    fun finish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        subTags: String?,
        jobId: String?,
        executeCount: Int,
        logStorageMode: LogStorageMode,
        finish: Boolean
    ) {
        with(TLogStatus.T_LOG_STATUS) {
            dslContext.insertInto(this)
                .set(BUILD_ID, buildId)
                .set(TAG, tag)
                .set(SUB_TAG, subTags)
                .set(EXECUTE_COUNT, executeCount)
                .set(JOB_ID, jobId)
                .set(FINISHED, finish)
                .set(MODE, logStorageMode.name)
                .onDuplicateKeyUpdate()
                .set(FINISHED, finish)
                .set(MODE, logStorageMode.name)
                .execute()
        }
    }

    fun updateStorageMode(
        dslContext: DSLContext,
        buildId: String,
        executeCount: Int,
        modeMap: Map<String, LogStorageMode>
    ) {
        with(TLogStatus.T_LOG_STATUS) {
            val records =
                mutableListOf<UpdateConditionStep<TLogStatusRecord>>()
            modeMap.forEach { (tag, mode) ->
                records.add(
                    dslContext.update(this)
                        .set(MODE, mode.name)
                        .where(BUILD_ID.eq(buildId))
                        .and(TAG.eq(tag))
                        .and(EXECUTE_COUNT.eq(executeCount))
                )
            }
            dslContext.batch(records).execute()
        }
    }

    fun listFinish(
        dslContext: DSLContext,
        buildId: String,
        executeCount: Int?
    ): Result<TLogStatusRecord>? {
        with(TLogStatus.T_LOG_STATUS) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(EXECUTE_COUNT.eq(executeCount ?: 1))
                .fetch()
        }
    }

    fun isFinish(
        dslContext: DSLContext,
        buildId: String,
        jobId: String?,
        tag: String?,
        subTags: String?,
        executeCount: Int?
    ): Boolean {
        with(TLogStatus.T_LOG_STATUS) {
            val select = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(EXECUTE_COUNT.eq(executeCount))
            if (!jobId.isNullOrBlank()) {
                select.and(JOB_ID.eq(jobId))
                    .and(TAG.eq(""))
                    .and(SUB_TAG.eq(""))
            } else {
                select.and(TAG.eq(tag))
                    .and(SUB_TAG.eq(subTags))
            }
            return select.fetchOne()?.finished == true
        }
    }

    fun getStorageMode(
        dslContext: DSLContext,
        buildId: String,
        tag: String,
        executeCount: Int?
    ): TLogStatusRecord? {
        with(TLogStatus.T_LOG_STATUS) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(TAG.eq(tag))
                .and(EXECUTE_COUNT.eq(executeCount ?: 1))
                .fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildIds: Set<String>
    ): Int {
        with(TLogStatus.T_LOG_STATUS) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }
}
