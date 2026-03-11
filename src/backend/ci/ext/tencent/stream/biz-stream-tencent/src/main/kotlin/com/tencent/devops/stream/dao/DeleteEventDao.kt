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

import com.tencent.devops.model.stream.Tables.T_STREAM_DELETE_EVENT
import com.tencent.devops.model.stream.tables.records.TStreamDeleteEventRecord
import com.tencent.devops.stream.pojo.StreamDeleteEvent
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DeleteEventDao {

    fun save(
        dslContext: DSLContext,
        streamDeleteEvent: StreamDeleteEvent
    ) {
        with(streamDeleteEvent) {
            with(T_STREAM_DELETE_EVENT) {
                dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    GIT_PROJECT_ID,
                    EVENT_ID,
                    ORIGIN_YAML,
                    CREATE_TIME,
                    CREATOR
                ).values(
                    pipelineId,
                    gitProjectId,
                    eventId,
                    originYaml,
                    LocalDateTime.now(),
                    userId
                ).onDuplicateKeyUpdate()
                    .set(CREATE_TIME, LocalDateTime.now())
                    .set(CREATOR, userId)
                    .set(EVENT_ID, eventId)
                    .set(ORIGIN_YAML, originYaml)
                    .execute()
            }
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String): TStreamDeleteEventRecord? {
        return with(T_STREAM_DELETE_EVENT) {
            dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).fetchAny()
        }
    }

    fun list(dslContext: DSLContext, gitProjectId: Long): List<TStreamDeleteEventRecord> {
        return with(T_STREAM_DELETE_EVENT) {
            dslContext.selectFrom(this).where(GIT_PROJECT_ID.eq(gitProjectId)).fetch()
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_STREAM_DELETE_EVENT) {
            dslContext.delete(this).where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }
}
