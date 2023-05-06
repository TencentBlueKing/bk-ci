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

import com.tencent.devops.model.process.tables.TPipelineViewGroup
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineViewGroupDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        pipelineId: String,
        userId: String
    ) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                VIEW_ID,
                PIPELINE_ID,
                CREATE_TIME,
                CREATOR
            ).values(
                projectId,
                viewId,
                pipelineId,
                LocalDateTime.now(),
                userId
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        viewId: Long,
        offset: Int,
        limit: Int
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(VIEW_ID.eq(viewId))
                .offset(offset).limit(limit)
                .fetch()
        }
    }

    fun listByViewIds(
        dslContext: DSLContext,
        projectId: String,
        viewIds: List<Long>
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.`in`(viewIds))
                .fetch()
        }
    }

    fun listByViewId(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .fetch()
        }
    }

    fun listByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun listByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun listByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Collection<String>
    ): List<TPipelineViewGroupRecord> {
        return with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun countByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) = with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
        dslContext.selectCount().from(this)
            .where(PROJECT_ID.eq(projectId))
            .and(PIPELINE_ID.eq(pipelineId))
            .fetchOne()?.component1() ?: 0
    }

    fun remove(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        pipelineId: String
    ) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun remove(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long
    ) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .execute()
        }
    }

    fun batchRemove(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        pipelineIds: List<String>
    ) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun countByViewId(
        dslContext: DSLContext,
        projectId: String,
        viewIds: Collection<Long>
    ): Map<Long, Int> {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            return dslContext.select(VIEW_ID, count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.`in`(viewIds))
                .groupBy(VIEW_ID)
                .fetch().map { it.value1() to it.value2() }.toMap()
        }
    }

    fun distinctPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        viewIds: Collection<Long>
    ): List<String> {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            return dslContext.selectDistinct(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.`in`(viewIds))
                .fetch().getValues(0, String::class.java)
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String): Boolean {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute() > 0
        }
    }
}
