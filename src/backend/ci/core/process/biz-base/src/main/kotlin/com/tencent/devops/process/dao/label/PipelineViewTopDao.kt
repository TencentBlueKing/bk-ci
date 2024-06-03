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

package com.tencent.devops.process.dao.label

import com.tencent.devops.model.process.tables.TPipelineViewTop
import com.tencent.devops.model.process.tables.records.TPipelineViewTopRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineViewTopDao {
    fun add(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        userId: String
    ) {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                VIEW_ID,
                CREATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                viewId,
                userId,
                now,
                now
            ).execute()
        }
    }

    fun remove(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        userId: String
    ) {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .and(CREATOR.eq(userId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): List<TPipelineViewTopRecord> {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREATOR.eq(userId))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }
}
