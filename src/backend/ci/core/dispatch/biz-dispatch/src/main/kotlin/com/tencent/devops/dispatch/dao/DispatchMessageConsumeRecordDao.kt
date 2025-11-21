/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.DispatchMessageStatus
import com.tencent.devops.model.dispatch.tables.TDispatchMessageConsumeRecord
import com.tencent.devops.model.dispatch.tables.records.TDispatchMessageConsumeRecordRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DispatchMessageConsumeRecordDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        executeCount: Int,
        dispatchType: String
    ) {
        with(TDispatchMessageConsumeRecord.T_DISPATCH_MESSAGE_CONSUME_RECORD) {
            val now = LocalDateTime.now()
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                EXECUTE_COUNT,
                DISPATCH_TYPE,
                CONSUME_STATUS,
                START_TIME
            ).values(
                projectId,
                pipelineId,
                buildId,
                vmSeqId,
                executeCount,
                dispatchType,
                DispatchMessageStatus.MESSAGE_RECEIVED.status,
                now
            ).onDuplicateKeyUpdate()
                .set(UPDATED_TIME, now)
                .set(RETRY_COUNT, RETRY_COUNT.plus(1))
                .returning(ID)
                .fetchOne()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        executeCount: Int,
        newStatus: DispatchMessageStatus,
        statusMsg: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null,
        errorType: String? = null
    ): Boolean {
        with(TDispatchMessageConsumeRecord.T_DISPATCH_MESSAGE_CONSUME_RECORD) {
            val update = dslContext.update(this)
                .set(CONSUME_STATUS, newStatus.status)
                .set(UPDATED_TIME, LocalDateTime.now())

            errorCode?.let { update.set(ERROR_CODE, it) }
            errorMessage?.let { update.set(ERROR_MESSAGE, it) }
            errorType?.let { update.set(ERROR_TYPE, it) }

            if (DispatchMessageStatus.isFinalStatus(newStatus)) {
                val now = LocalDateTime.now()
                update.set(END_TIME, now)

                val record = get(dslContext, buildId, vmSeqId, executeCount)
                record?.startTime?.let { startTime ->
                    val duration = java.time.Duration.between(startTime, now).toMillis()
                    update.set(TOTAL_TIME_COST, duration)
                }
            }

            return update.where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute() > 0
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        executeCount: Int
    ): TDispatchMessageConsumeRecordRecord? {
        with(TDispatchMessageConsumeRecord.T_DISPATCH_MESSAGE_CONSUME_RECORD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetchOne()
        }
    }
}