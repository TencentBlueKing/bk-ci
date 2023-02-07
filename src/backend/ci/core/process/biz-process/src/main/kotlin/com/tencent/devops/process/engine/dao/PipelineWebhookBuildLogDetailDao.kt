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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL
import com.tencent.devops.model.process.tables.records.TPipelineWebhookBuildLogDetailRecord
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLogDetail
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
@Suppress("ALL")
class PipelineWebhookBuildLogDetailDao {

    fun save(
        dslContext: DSLContext,
        logId: Long,
        webhookBuildLogDetails: List<PipelineWebhookBuildLogDetail>
    ) {
        if (webhookBuildLogDetails.isEmpty()) {
            return
        }
        webhookBuildLogDetails.map {
            with(T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL) {
                dslContext.insertInto(
                    this,
                    LOG_ID,
                    CODE_TYPE,
                    REPO_NAME,
                    COMMIT_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    TASK_ID,
                    TASK_NAME,
                    SUCCESS,
                    TRIGGER_RESULT,
                    CREATED_TIME,
                    ID
                ).values(
                    logId,
                    it.codeType,
                    it.repoName,
                    it.commitId,
                    it.projectId,
                    it.pipelineId,
                    it.taskId,
                    it.taskName,
                    it.success,
                    it.triggerResult,
                    Timestamp(it.createdTime).toLocalDateTime(),
                    it.id
                ).execute()
            }
        }
    }

    fun listByPage(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoName: String?,
        commitId: String?,
        offset: Int,
        limit: Int
    ): List<PipelineWebhookBuildLogDetail> {
        return with(T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL) {
            val where = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
            if (commitId != null) {
                where.and(COMMIT_ID.eq(commitId))
            }
            if (repoName != null) {
                where.and(REPO_NAME.eq(repoName))
            }
            where.orderBy(CREATED_TIME.desc())
                .limit(offset, limit).fetch()
        }.map { convert(it) }
    }

    fun countByPage(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoName: String?,
        commitId: String?
    ): Long {
        return with(T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL) {
            val where = dslContext.selectCount()
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
            if (commitId != null) {
                where.and(COMMIT_ID.eq(commitId))
            }
            if (repoName != null) {
                where.and(REPO_NAME.eq(repoName))
            }
            where.fetchOne(0, Long::class.java)!!
        }
    }

    fun convert(record: TPipelineWebhookBuildLogDetailRecord): PipelineWebhookBuildLogDetail {
        return with(record) {
            PipelineWebhookBuildLogDetail(
                id = id,
                logId = logId,
                codeType = codeType,
                repoName = repoName,
                commitId = commitId,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId,
                taskName = taskName,
                success = success,
                triggerResult = triggerResult,
                createdTime = createdTime.timestampmilli()
            )
        }
    }
}
