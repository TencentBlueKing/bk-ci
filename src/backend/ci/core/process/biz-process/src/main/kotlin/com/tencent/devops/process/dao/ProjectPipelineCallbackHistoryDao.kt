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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TProjectPipelineCallbackHistory
import com.tencent.devops.model.process.tables.records.TProjectPipelineCallbackHistoryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class ProjectPipelineCallbackHistoryDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        callBackUrl: String,
        events: String,
        status: String,
        requestHeader: String,
        requestBody: String,
        response: String,
        errorMsg: String?
    ) {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                EVENTS,
                CALLBACK_URL,
                STATUS,
                ERROR_MSG,
                REQUEST_HEADER,
                REQUEST_BODY,
                RESPONSE,
                CREATED_TIME
            ).values(
                projectId,
                events,
                callBackUrl,
                status,
                errorMsg,
                requestHeader,
                requestBody,
                response,
                now
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        id: Long
    ): TProjectPipelineCallbackHistoryRecord? {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): Result<TProjectPipelineCallbackHistoryRecord> {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            val where = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (startTime != null) {
                where.and(CREATED_TIME.ge(Timestamp(startTime).toLocalDateTime()))
            }
            if (endTime != null) {
                where.and(CREATED_TIME.le(Timestamp(endTime).toLocalDateTime()))
            }
            return where.orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        startTime: Long?,
        endTime: Long?
    ): Long {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            val where = dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
            if (startTime != null) {
                where.and(CREATED_TIME.ge(Timestamp(startTime).toLocalDateTime()))
            }
            if (endTime != null) {
                where.and(CREATED_TIME.le(Timestamp(endTime).toLocalDateTime()))
            }
            return where.fetchOne(0, Long::class.java)
        }
    }
}