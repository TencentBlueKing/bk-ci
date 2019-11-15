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

package com.tencent.devops.process.plugin.trigger.dao

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.model.process.Tables.T_PIPELINE_TIMER
import com.tencent.devops.model.process.tables.records.TPipelineTimerRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open class PipelineTimerDao {

    open fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        crontabExpression: String,
        channelCode: ChannelCode
    ): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                CREATE_TIME,
                CREATOR,
                CRONTAB,
                CHANNEL
            ).values(projectId, pipelineId, LocalDateTime.now(), userId, crontabExpression, channelCode.name)
                .onDuplicateKeyUpdate()
                .set(CREATE_TIME, LocalDateTime.now())
                .set(CREATOR, userId)
                .set(CRONTAB, crontabExpression)
                .set(CHANNEL, channelCode.name)
                .execute()
        }
    }

    open fun get(dslContext: DSLContext, pipelineId: String): TPipelineTimerRecord? {
        return with(T_PIPELINE_TIMER) {
            dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).fetchAny()
        }
    }

    open fun delete(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.delete(this).where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    open fun list(dslContext: DSLContext, offset: Int, limit: Int): Result<TPipelineTimerRecord> {
        return with(T_PIPELINE_TIMER) {
            dslContext.selectFrom(this).limit(offset, limit).fetch()
        }
    }
}
