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

package com.tencent.devops.repository.dao

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.repository.tables.TRepositoryCommit
import com.tencent.devops.model.repository.tables.records.TRepositoryCommitRecord
import com.tencent.devops.repository.pojo.commit.CommitData
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("ALL")
@Repository
class CommitDao {
    fun getBuildCommit(
        dslContext: DSLContext,
        buildId: String,
        offset: Int = 0,
        limit: Int = 500
    ): Result<TRepositoryCommitRecord>? {
        with(TRepositoryCommit.T_REPOSITORY_COMMIT) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .orderBy(COMMIT_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun addCommit(dslContext: DSLContext, commits: List<CommitData>): IntArray {
        with(TRepositoryCommit.T_REPOSITORY_COMMIT) {
            val query = commits.map {
                dslContext.insertInto(
                    this,
                    BUILD_ID,
                    REPO_ID,
                    REPO_NAME,
                    TYPE,
                    PIPELINE_ID,
                    COMMIT,
                    COMMITTER,
                    COMMIT_TIME,
                    COMMENT,
                    ELEMENT_ID,
                    URL
                )
                    .values(
                        it.buildId,
                        if (it.repoId.isNullOrBlank()) 0L else HashUtil.decodeOtherIdToLong(it.repoId!!),
                        it.repoName,
                        it.type,
                        it.pipelineId,
                        it.commit,
                        it.committer,
                        LocalDateTime.ofInstant(
                            Date(TimeUnit.SECONDS.toMillis(it.commitTime)).toInstant(),
                            ZoneId.systemDefault()
                        ),
                        it.comment,
                        it.elementId,
                        it.url
                    )
            }
            return dslContext.batch(query).execute()
        }
    }

    fun getLatestCommitById(
        dslContext: DSLContext,
        pipelineId: String,
        elementId: String,
        repoId: Long,
        page: Int?,
        pageSize: Int?
    ): Result<TRepositoryCommitRecord>? {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page ?: 1, pageSize ?: 20)
        with(TRepositoryCommit.T_REPOSITORY_COMMIT) {
            return dslContext.selectFrom(this.forceIndex("IDX_PIPE_ELEMENT_REPO_TIME"))
                .where(PIPELINE_ID.eq(pipelineId).and(ELEMENT_ID.eq(elementId)).and(REPO_ID.eq(repoId)))
                .orderBy(COMMIT_TIME.desc())
                .limit(sqlLimit.offset, sqlLimit.limit)
                .fetch()
        }
    }

    fun getLatestCommitByName(
        dslContext: DSLContext,
        pipelineId: String,
        elementId: String,
        repoName: String,
        page: Int?,
        pageSize: Int?
    ): Result<TRepositoryCommitRecord>? {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page ?: 1, pageSize ?: 20)
        with(TRepositoryCommit.T_REPOSITORY_COMMIT) {
            return dslContext.selectFrom(this.forceIndex("IDX_PIPE_ELEMENT_NAME_REPO_TIME"))
                .where(PIPELINE_ID.eq(pipelineId).and(ELEMENT_ID.eq(elementId)).and(REPO_NAME.eq(repoName)))
                .orderBy(COMMIT_TIME.desc())
                .limit(sqlLimit.offset, sqlLimit.limit)
                .fetch()
        }
    }
}
