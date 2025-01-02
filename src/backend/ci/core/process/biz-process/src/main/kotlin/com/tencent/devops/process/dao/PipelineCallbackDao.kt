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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineCallback
import com.tencent.devops.model.process.tables.records.TPipelineCallbackRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineCallbackDao {

    /**
     * 可直接更新或插入
     */
    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        name: String,
        event: String,
        userId: String,
        url: String,
        region: String,
        secretToken: String?,
    ) {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                EVENT_TYPE,
                USER_ID,
                URL,
                REGION,
                SECRET_TOKEN,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                pipelineId,
                name,
                event,
                userId,
                url,
                region,
                secretToken,
                now,
                now,
            ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, now)
                .set(URL, url)
                .set(SECRET_TOKEN, secretToken)
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        event: String?
    ): Result<TPipelineCallbackRecord> {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .let {
                    if (!event.isNullOrBlank()) {
                        it.and(EVENT_TYPE.eq(event))
                    } else it
                }
                .fetch()
        }
    }
}
