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

import com.tencent.devops.model.process.tables.TPipelineView
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线视图
 */
@Suppress("ALL")
@Repository
class PipelineViewDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        isProject: Boolean,
        filterByPipelineName: String,
        filterByCreator: String,
        userId: String,
        id: Long? = null
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
                CREATE_USER,
                ID
            )
                .values(
                    projectId,
                    name,
                    isProject,
                    filterByPipelineName,
                    filterByCreator,
                    now,
                    now,
                    userId,
                    id
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        logic: String,
        isProject: Boolean,
        filters: String,
        userId: String,
        id: Long? = null,
        viewType: Int
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
                CREATE_USER,
                ID,
                VIEW_TYPE
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
                    userId,
                    id,
                    viewType
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
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
                .where(ID.eq(viewId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        name: String,
        logic: String,
        isProject: Boolean,
        filters: String,
        viewType: Int
    ): Boolean {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.update(this)
                .let { if (StringUtils.isNotBlank(name)) it.set(NAME, name) else it }
                .let { if (StringUtils.isNotBlank(logic)) it.set(LOGIC, logic) else it }
                .let { if (filters.contains("@type")) it.set(FILTERS, filters) else it }
                .let { if (viewType != PipelineViewType.UNCLASSIFIED) it.set(VIEW_TYPE, viewType) else it }
                .set(IS_PROJECT, isProject)
                .set(FILTER_BY_PIPEINE_NAME, "")
                .set(FILTER_BY_CREATOR, "")
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(viewId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long
    ): Boolean {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(viewId).and(PROJECT_ID.eq(projectId)))
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

    fun list(dslContext: DSLContext, projectId: String, viewType: Int): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_TYPE.eq(viewType))
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
        userId: String,
        projectId: String,
        isProject: Boolean? = null,
        viewType: Int? = null
    ): List<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .let {
                    if (isProject == null) {
                        it.and(IS_PROJECT.eq(true).or(CREATE_USER.eq(userId)))
                    } else {
                        if (isProject) {
                            it.and(IS_PROJECT.eq(true))
                        } else {
                            it.and(CREATE_USER.eq(userId)).and(IS_PROJECT.eq(false))
                        }
                    }
                }.let {
                    if (viewType == null) {
                        it
                    } else {
                        it.and(VIEW_TYPE.eq(viewType))
                    }
                }.fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String? = null,
        viewIds: Set<Long>
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(viewIds))
                .let { if (projectId == null) it else it.and(PROJECT_ID.eq(projectId)) }
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun listByPage(
        dslContext: DSLContext,
        projectId: String,
        isProject: Boolean,
        viewName: String? = null,
        limit: Int,
        offset: Int
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_PROJECT.eq(isProject))
                .let { if (viewName != null) it.and(NAME.like("%$viewName%")) else it }
                .offset(offset).limit(limit)
                .fetch()
        }
    }

    fun listAll(
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

    fun listDynamicProjectId(
        dslContext: DSLContext
    ): List<String> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.select(PROJECT_ID).from(this)
                .where(VIEW_TYPE.eq(PipelineViewType.DYNAMIC))
                .fetch(0, String::class.java)
                .distinct()
        }
    }

    fun listDynamicViewByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): Result<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_TYPE.eq(PipelineViewType.DYNAMIC))
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
                .and(IS_PROJECT.eq(isProject))
                .let { if (isProject) it else it.and(CREATE_USER.eq(userId)) }
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun get(dslContext: DSLContext, projectId: String, viewId: Long): TPipelineViewRecord? {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(ID.eq(viewId).and(PROJECT_ID.eq(projectId)))
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

    fun countByName(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        creator: String? = null,
        isProject: Boolean,
        excludeIds: Collection<Long> = emptySet()
    ): Int {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.eq(name))
                .and(IS_PROJECT.eq(isProject))
                .let { if (null != creator) it.and(CREATE_USER.eq(creator)) else it }
                .let { if (excludeIds.isNotEmpty()) it.and(ID.notIn(excludeIds)) else it }
                .fetchOne()?.component1() ?: 0
        }
    }

    fun fetchAnyByName(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        isProject: Boolean
    ): TPipelineViewRecord? {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.eq(name))
                .and(IS_PROJECT.eq(isProject))
                .fetchAny()
        }
    }

    fun countForLimit(
        dslContext: DSLContext,
        projectId: String,
        isProject: Boolean,
        userId: String
    ): Int {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_PROJECT.eq(isProject))
                .let { if (isProject) it else it.and(CREATE_USER.eq(userId)) }
                .fetchOne()?.component1() ?: 0
        }
    }
}
