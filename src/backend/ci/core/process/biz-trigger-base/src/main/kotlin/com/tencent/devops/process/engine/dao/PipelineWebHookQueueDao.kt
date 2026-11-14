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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.model.process.tables.records.TPipelineWebhookQueueRecord
import com.tencent.devops.process.engine.pojo.PipelineWebHookQueue
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineWebHookQueueDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        sourceProjectId: Long,
        sourceRepoName: String,
        sourceBranch: String,
        targetProjectId: Long,
        targetRepoName: String,
        targetBranch: String,
        buildId: String,
        id: Long? = null
    ) {
        with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                SOURCE_PROJECT_ID,
                SOURCE_REPO_NAME,
                SOURCE_BRANCH,
                TARGET_PROJECT_ID,
                TARGET_REPO_NAME,
                TARGET_BRANCH,
                BUILD_ID,
                CREATE_TIME,
                ID
            ).values(
                projectId,
                pipelineId,
                sourceProjectId,
                sourceRepoName,
                sourceBranch,
                targetProjectId,
                targetRepoName,
                targetBranch,
                buildId,
                LocalDateTime.now(),
                id
            ).execute()
        }
    }

    fun getWebHookBuildHistory(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        sourceProjectId: Long,
        sourceBranch: String,
        targetProjectId: Long,
        targetBranch: String
    ): List<PipelineWebHookQueue>? {
        return with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(SOURCE_PROJECT_ID.eq(sourceProjectId))
                .and(SOURCE_BRANCH.eq(sourceBranch))
                .and(TARGET_PROJECT_ID.eq(targetProjectId))
                .and(TARGET_BRANCH.eq(targetBranch))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }.map {
            convert(it)
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): PipelineWebHookQueue? {
        return with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }?.let { convert(it) }
    }

    fun deleteByBuildIds(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ) {
        with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun convert(record: TPipelineWebhookQueueRecord): PipelineWebHookQueue {
        return with(record) {
            PipelineWebHookQueue(
                id = id,
                pipelineId = pipelineId,
                sourceProjectId = sourceProjectId,
                sourceRepoName = sourceRepoName,
                sourceBranch = sourceBranch,
                targetProjectId = targetProjectId,
                targetRepoName = targetRepoName,
                targetBranch = targetBranch,
                buildId = buildId,
                createTime = createTime.timestampmilli()
            )
        }
    }
}
