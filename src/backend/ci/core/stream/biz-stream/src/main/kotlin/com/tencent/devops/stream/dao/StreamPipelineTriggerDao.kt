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

package com.tencent.devops.stream.dao

import com.tencent.devops.model.stream.tables.TStreamPipelineTrigger
import com.tencent.devops.model.stream.tables.records.TStreamPipelineTriggerRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StreamPipelineTriggerDao {

    fun saveOrUpdate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        branch: String,
        ciFileBlobId: String,
        trigger: String
    ) {
        with(TStreamPipelineTrigger.T_STREAM_PIPELINE_TRIGGER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BRANCH,
                CI_FILE_BLOB_ID,
                TRIGGER
            ).values(
                projectId,
                pipelineId,
                branch,
                ciFileBlobId,
                trigger
            ).onDuplicateKeyUpdate()
                .set(CI_FILE_BLOB_ID, ciFileBlobId)
                .set(TRIGGER, trigger)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getTriggers(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        branch: String?,
        ciFileBlobIds: Set<String>
    ): List<TStreamPipelineTriggerRecord> {
        with(TStreamPipelineTrigger.T_STREAM_PIPELINE_TRIGGER) {
            val dsl = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!branch.isNullOrBlank()) {
                dsl.and(BRANCH.eq(branch))
            }

            return dsl.and(CI_FILE_BLOB_ID.`in`(ciFileBlobIds)).groupBy(CI_FILE_BLOB_ID).fetch()
        }
    }

    fun deleteTrigger(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        branch: String?
    ) {
        with(TStreamPipelineTrigger.T_STREAM_PIPELINE_TRIGGER) {
            val dsl = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!branch.isNullOrBlank()) {
                dsl.and(BRANCH.eq(branch))
            }

            dsl.execute()
        }
    }
}
