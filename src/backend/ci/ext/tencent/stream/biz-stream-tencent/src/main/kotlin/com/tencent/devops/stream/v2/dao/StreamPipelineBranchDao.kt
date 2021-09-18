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

package com.tencent.devops.stream.v2.dao

import com.tencent.devops.model.stream.tables.TStreamPipelineBranch
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StreamPipelineBranchDao {

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
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .count()
        }
    }
}
