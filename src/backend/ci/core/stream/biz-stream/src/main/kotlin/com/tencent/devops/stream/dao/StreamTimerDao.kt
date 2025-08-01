/*
 * Tencent is pleased to support the source community by making BK-CI 蓝鲸持续集成平台 available.
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

import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.model.stream.Tables.T_STREAM_TIMER
import com.tencent.devops.model.stream.tables.records.TStreamTimerRecord
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimer
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StreamTimerDao {

    fun save(
        dslContext: DSLContext,
        streamTimer: StreamTimer
    ): Int {
        return with(streamTimer) {
            with(T_STREAM_TIMER) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    CREATE_TIME,
                    CREATOR,
                    CRONTAB,
                    GIT_PROJECT_ID,
                    BRANCHS,
                    ALWAYS,
                    CHANNEL,
                    EVENT_ID,
                    ORIGIN_YAML
                ).values(
                    projectId,
                    pipelineId,
                    LocalDateTime.now(),
                    userId,
                    JsonUtil.toJson(crontabExpressions),
                    gitProjectId,
                    if (branchs.isNullOrEmpty()) {
                        null
                    } else {
                        JsonUtil.toJson(branchs)
                    },
                    always,
                    channelCode.name,
                    eventId,
                    originYaml
                )
                    .onDuplicateKeyUpdate()
                    .set(CREATE_TIME, LocalDateTime.now())
                    .set(CREATOR, userId)
                    .set(CRONTAB, JsonUtil.toJson(crontabExpressions))
                    .set(GIT_PROJECT_ID, gitProjectId)
                    .set(
                        BRANCHS,
                        if (branchs.isNullOrEmpty()) {
                            null
                        } else {
                            JsonUtil.toJson(branchs)
                        }
                    )
                    .set(ALWAYS, always)
                    .set(CHANNEL, channelCode.name)
                    .set(EVENT_ID, eventId)
                    .set(ORIGIN_YAML, originYaml)
                    .execute()
            }
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String): TStreamTimerRecord? {
        return with(T_STREAM_TIMER) {
            dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).fetchAny()
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_STREAM_TIMER) {
            dslContext.delete(this).where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun list(dslContext: DSLContext, offset: Int, limit: Int): Result<TStreamTimerRecord> {
        return with(T_STREAM_TIMER) {
            dslContext.selectFrom(this).limit(offset, limit).fetch()
        }
    }
}
