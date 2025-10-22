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

package com.tencent.devops.process.dao

import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
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
        userId: String,
        list: List<PipelineCallbackEvent>
    ) {
        if (list.isEmpty()) return
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            val now = LocalDateTime.now()
            list.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    NAME,
                    EVENT_TYPE,
                    USER_ID,
                    URL,
                    SECRET_TOKEN,
                    REGION,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    projectId,
                    pipelineId,
                    it.callbackName,
                    it.callbackEvent.name,
                    userId,
                    it.callbackUrl,
                    it.secretToken,
                    (it.region ?: CallBackNetWorkRegionType.IDC).name,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                    .set(UPDATE_TIME, now)
                    .set(URL, it.callbackUrl)
                    .set(SECRET_TOKEN, it.secretToken)
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        event: String? = null
    ): Result<TPipelineCallbackRecord> {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .let {
                    if (!event.isNullOrBlank()) {
                        it.and(EVENT_TYPE.eq(event))
                    } else it
                }
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        names: Set<String>
    ) {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(NAME.`in`(names))
                .execute()
        }
    }
}
