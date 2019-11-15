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

package com.tencent.devops.log.dao.v2

import com.tencent.devops.model.log.tables.TLogStatusV2
import com.tencent.devops.model.log.tables.records.TLogStatusV2Record
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class LogStatusDaoV2 {

    fun finish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        finish: Boolean
    ) {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            dslContext.insertInto(this,
                BUILD_ID, TAG, EXECUTE_COUNT, FINISHED)
                .values(buildId, tag ?: "", executeCount ?: 1, finish)
                .onDuplicateKeyUpdate()
                .set(FINISHED, finish)
                .execute()
        }
    }

    fun listFinish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): Result<TLogStatusV2Record>? {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            return dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount ?: 1))
                    .fetch()
        }
    }

    fun isFinish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): Boolean {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(TAG.eq(tag ?: ""))
                .and(EXECUTE_COUNT.eq(executeCount ?: 1))
                .fetchOne()?.finished ?: false
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildIds: Set<String>
    ): Int {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }
}