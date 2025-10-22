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

package com.tencent.devops.stream.dao

import com.tencent.devops.model.stream.tables.TGitPipelineRepoResource
import com.tencent.devops.model.stream.tables.records.TGitPipelineRepoResourceRecord
import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitPipelineRepoResourceDao {

    fun create(
        dslContext: DSLContext,
        streamRepoHookEvent: StreamRepoHookEvent
    ): Int {
        with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            return dslContext.insertInto(
                this,
                SOURCE_GIT_PROJECT_PATH,
                TARGET_GIT_PROJECT_ID,
                PIPELINE_ID,
                CREATE_TIME
            ).values(
                streamRepoHookEvent.sourceGitProjectPath,
                streamRepoHookEvent.targetGitProjectId,
                streamRepoHookEvent.pipelineId,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updatePipeline(
        dslContext: DSLContext,
        sourceGitProjectPath: String,
        targetGitProjectId: Long,
        pipelineId: String
    ): Int {
        with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            return dslContext.update(this)
                .set(SOURCE_GIT_PROJECT_PATH, sourceGitProjectPath)
                .set(TARGET_GIT_PROJECT_ID, targetGitProjectId)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun getRepoList(dslContext: DSLContext, pipelineId: String): Result<TGitPipelineRepoResourceRecord> {
        return with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).fetch()
        }
    }

    fun getRepo(
        dslContext: DSLContext,
        pipelineId: String,
        sourceGitProjectPath: String
    ): TGitPipelineRepoResourceRecord? {
        return with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(SOURCE_GIT_PROJECT_PATH.eq(sourceGitProjectPath))).fetchOne()
        }
    }

    /*
    * 需要将sourceGitProjectPath分解成所有能匹配上的字段
    */
    fun getPipelineBySourcePath(
        dslContext: DSLContext,
        sourceGitProjectPathList: List<String>
    ): List<Record3<String, String, Long>> {
        with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            return dslContext.select(PIPELINE_ID, SOURCE_GIT_PROJECT_PATH, TARGET_GIT_PROJECT_ID).from(this)
                .where(SOURCE_GIT_PROJECT_PATH.`in`(sourceGitProjectPathList))
                .fetch()
        }
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        pipelineId: String
    ): Int {
        with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteByPipelineIdAndSourcePath(
        dslContext: DSLContext,
        pipelineId: String,
        sourceGitProjectPath: String
    ): Int {
        with(TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(SOURCE_GIT_PROJECT_PATH.eq(sourceGitProjectPath)))
                .execute()
        }
    }
}
