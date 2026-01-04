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

import com.tencent.devops.model.stream.Tables.T_STREAM_TIMER_BRANCH
import com.tencent.devops.model.stream.tables.records.TStreamTimerBranchRecord
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimerBranch
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StreamTimerBranchDao {

    fun save(
        dslContext: DSLContext,
        streamTimerBranch: StreamTimerBranch
    ): Int {
        return with(streamTimerBranch) {
            with(T_STREAM_TIMER_BRANCH) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    GIT_PROJECT_ID,
                    BRANCH,
                    REVISION,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    projectId,
                    pipelineId,
                    gitProjectId,
                    branch,
                    revision,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).onDuplicateKeyUpdate()
                    .set(BRANCH, branch)
                    .set(REVISION, revision)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .execute()
            }
        }
    }

    fun get(
        dslContext: DSLContext,
        pipelineId: String,
        gitProjectId: Long,
        branch: String
    ): StreamTimerBranch? {
        val record = with(T_STREAM_TIMER_BRANCH) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BRANCH.eq(branch))
                .fetchAny()
        } ?: return null
        return convert(record)
    }

    private fun convert(record: TStreamTimerBranchRecord): StreamTimerBranch {
        return with(record) {
            StreamTimerBranch(
                projectId = projectId,
                pipelineId = pipelineId,
                gitProjectId = gitProjectId,
                branch = branch,
                revision = revision
            )
        }
    }
}
