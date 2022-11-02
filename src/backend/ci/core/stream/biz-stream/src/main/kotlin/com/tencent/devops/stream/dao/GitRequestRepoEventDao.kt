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

package com.tencent.devops.stream.dao

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.model.stream.tables.TGitRequestRepoEvent
import com.tencent.devops.stream.pojo.GitRequestRepoEvent
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GitRequestRepoEventDao {

    fun saveGitRequest(
        dslContext: DSLContext,
        event: GitRequestRepoEvent
    ): Long {
        with(TGitRequestRepoEvent.T_GIT_REQUEST_REPO_EVENT) {
            val record = dslContext.insertInto(
                this,
                EVENT_ID,
                PIPELINE_ID,
                BUILD_ID,
                TARGET_GIT_PROJECT_ID,
                SOURCE_GIT_PROJECT_ID
            ).values(
                event.eventId,
                event.pipelineId,
                event.buildId,
                event.targetGitProjectId,
                event.sourceGitProjectId
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun updateBuildId(
        dslContext: DSLContext,
        eventId: Long,
        pipelineId: String,
        buildId: String
    ) {
        with(TGitRequestRepoEvent.T_GIT_REQUEST_REPO_EVENT) {
            dslContext.update(this)
                .set(BUILD_ID, buildId)
                .where(EVENT_ID.eq(eventId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun getRepoEventInfo(
        dslContext: DSLContext,
        eventId: Long,
        pipelineId: String,
        buildId: String?
    ): GitRequestRepoEvent? {
        with(TGitRequestRepoEvent.T_GIT_REQUEST_REPO_EVENT) {
            val dsl = dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!buildId.isNullOrBlank()) {
                dsl.and(BUILD_ID.eq(buildId))
            }
            return dsl.fetchOne()?.let {
                GitRequestRepoEvent(
                    it.eventId,
                    it.pipelineId,
                    it.buildId,
                    it.targetGitProjectId,
                    it.sourceGitProjectId,
                    it.createTime.timestamp()
                )
            }
        }
    }
}
