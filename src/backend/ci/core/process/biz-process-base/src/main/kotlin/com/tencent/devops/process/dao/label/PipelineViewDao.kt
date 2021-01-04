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

package com.tencent.devops.process.dao.label

import com.tencent.devops.model.process.tables.TPipelineView
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线视图
 */
@Repository
class PipelineViewDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        isProject: Boolean,
        filterByPipelineName: String,
        filterByCreator: String,
        userId: String
    ): Long {
        with(TPipelineView.T_PIPELINE_VIEW) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                IS_PROJECT,
                FILTER_BY_PIPEINE_NAME,
                FILTER_BY_CREATOR,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER
            )
                .values(
                    projectId,
                    name,
                    isProject,
                    filterByPipelineName,
                    filterByCreator,
                    now,
                    now,
                    userId
                )
                .returning(ID)
                .fetchOne().id
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        logic: String,
        isProject: Boolean,
        filters: String,
        userId: String
    ): Long {
        with(TPipelineView.T_PIPELINE_VIEW) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                IS_PROJECT,
                LOGIC,
                FILTER_BY_PIPEINE_NAME,
                FILTER_BY_CREATOR,
                FILTERS,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER
            )
                .values(
                    projectId,
                    name,
                    isProject,
                    logic,
                    "",
                    "",
                    filters,
                    now,
                    now,
                    userId
                )
                .returning(ID)
                .fetchOne().id
        }
    }

    fun update(
        dslContext: DSLContext,
        viewId: Long,
        name: String,
        isProject: Boolean,
        filterByPipelineName: String,
        filterByCreator: String
    ): Boolean {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.update(this)
                .set(NAME, name)
                .set(IS_PROJECT, isProject)
                .set(FILTER_BY_PIPEINE_NAME, filterByPipelineName)
                .set(FILTER_BY_CREATOR, filterByCreator)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(viewId))
                .execute() == 1
        }
    }

    fun update(
        dslContext: DSLContext,
        viewId: Long,
        name: String,
        logic: String,
        isProject: Boolean,
        filters: String
    ): Boolean {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.update(this)
                .set(NAME, name)
                .set(LOGIC, logic)
                .set(IS_PROJECT, isProject)
                .set(FILTER_BY_PIPEINE_NAME, "")
                .set(FILTER_BY_CREATOR, "")
                .set(FILTERS, filters)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(viewId))
                .execute() == 1
        }
    }

    fun delete(
        dslContext: DSLContext,
        viewId: Long
    ): Boolean {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(viewId))
                .execute() == 1
        }
    }

    fun list(dslContext: DSLContext, projectId: String): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, isProject: Boolean): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_PROJECT.eq(isProject))
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREATE_USER.eq(userId))
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        isProject: Boolean
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_PROJECT.eq(isProject))
                .and(CREATE_USER.eq(userId))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        viewIds: Set<Long>
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(viewIds))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun listProjectOrUser(
        dslContext: DSLContext,
        projectId: String,
        isProject: Boolean,
        userId: String
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_PROJECT.eq(isProject).or(CREATE_USER.eq(userId)))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun get(dslContext: DSLContext, viewId: Long): TPipelineViewRecord? {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(ID.eq(viewId))
                .fetchOne()
        }
    }

    fun get(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        viewId: Long
    ): TPipelineViewRecord? {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(ID.eq(viewId))
                .and(CREATE_USER.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }
}