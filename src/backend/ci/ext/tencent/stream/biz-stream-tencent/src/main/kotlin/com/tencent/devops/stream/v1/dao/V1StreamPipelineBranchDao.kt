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

package com.tencent.devops.stream.v1.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.model.stream.tables.TStreamPipelineBranch
import com.tencent.devops.model.stream.tables.records.TStreamPipelineBranchRecord
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class V1StreamPipelineBranchDao {

    fun saveOrUpdate(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        branch: String
    ) {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            dslContext.insertInto(
                this,
                GIT_PROJECT_ID,
                PIPELINE_ID,
                BRANCH
            ).values(
                gitProjectId,
                pipelineId,
                branch
            ).onDuplicateKeyUpdate().set(UPDATE_TIME, LocalDateTime.now()).execute()
        }
    }

    fun deletePipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String
    ): Int {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.delete(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteBranch(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        branch: String
    ): Int {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.delete(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BRANCH.eq(branch))
                .execute()
        }
    }

    fun pipelineBranchCount(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String
    ): Int {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getMaxGitProjectId(
        dslContext: DSLContext
    ): Long {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.select(DSL.max(GIT_PROJECT_ID)).from(this).fetchOne(0, Long::class.java)!!
        }
    }

    fun getMinGitProjectId(
        dslContext: DSLContext
    ): Long {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.select(DSL.min(GIT_PROJECT_ID)).from(this).fetchOne(0, Long::class.java)!!
        }
    }

    fun isGitProjectExist(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Boolean {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId)).fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun getBranches(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String?,
        pageSize: Long
    ): List<String> {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            val conditions = mutableListOf<Condition>()
            conditions.add(GIT_PROJECT_ID.eq(gitProjectId))
            if (pipelineId != null) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            val records = dslContext.select(BRANCH).from(this)
                .where(conditions)
                .limit(pageSize)
                .fetch()
            return if (records.isEmpty()) {
                emptyList()
            } else {
                records.map { result -> result.getValue(0) as String }
            }
        }
    }

    fun deleteBranches(
        dslContext: DSLContext,
        gitProjectId: Long,
        branches: Set<String>
    ): Int {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.delete(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BRANCH.`in`(branches))
                .execute()
        }
    }

    fun getBranchPipelines(
        dslContext: DSLContext,
        gitProjectId: Long,
        branches: String
    ): List<TStreamPipelineBranchRecord> {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BRANCH.eq(branches))
                .fetch()
        }
    }

    fun getProjectPipelineCount(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        search: String?,
        limit: SQLLimit,
        orderBy: GitCodeBranchesOrder,
        sort: GitCodeBranchesSort
    ): Int {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return projectPipelineDslCount(dslContext, gitProjectId, pipelineId, search)
        }
    }

    fun getProjectPipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        search: String?,
        limit: SQLLimit,
        orderBy: GitCodeBranchesOrder,
        sort: GitCodeBranchesSort
    ): List<TStreamPipelineBranchRecord> {
        with(TStreamPipelineBranch.T_STREAM_PIPELINE_BRANCH) {
            return projectPipelineDsl(dslContext, gitProjectId, pipelineId, search, orderBy, sort)
                .groupBy(BRANCH)
                .offset(limit.offset)
                .limit(limit.limit)
                .fetch()
        }
    }

    private fun TStreamPipelineBranch.projectPipelineDsl(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        search: String?,
        orderBy: GitCodeBranchesOrder,
        sort: GitCodeBranchesSort
    ): SelectConditionStep<TStreamPipelineBranchRecord> {
        val dsl = dslContext.selectFrom(this)
            .where(GIT_PROJECT_ID.eq(gitProjectId))
            .and(PIPELINE_ID.eq(pipelineId))
        if (!search.isNullOrBlank()) {
            dsl.and(BRANCH.like("%$search%"))
        }
        val order = when (orderBy) {
            GitCodeBranchesOrder.NAME -> {
                BRANCH
            }
            GitCodeBranchesOrder.UPDATE -> {
                UPDATE_TIME
            }
        }
        when (sort) {
            GitCodeBranchesSort.ASC -> {
                dsl.orderBy(order.asc())
            }
            GitCodeBranchesSort.DESC -> {
                dsl.orderBy(order.desc())
            }
        }
        return dsl
    }

    private fun TStreamPipelineBranch.projectPipelineDslCount(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        search: String?
    ): Int {
        val dsl = dslContext.selectCount().from(this)
            .where(GIT_PROJECT_ID.eq(gitProjectId))
            .and(PIPELINE_ID.eq(pipelineId))
        if (!search.isNullOrBlank()) {
            dsl.and(BRANCH.like("%$search%"))
        }
        return dsl.fetchOne(0, Int::class.java)!!
    }
}
