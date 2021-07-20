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
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK_BUILD_LOG
import com.tencent.devops.model.process.tables.records.TPipelineWebhookBuildLogRecord
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLog
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class PipelineWebhookBuildLogDao {

    fun save(
        dslContext: DSLContext,
        webhookBuildLog: PipelineWebhookBuildLog
    ): Long {
        return with(webhookBuildLog) {
            with(T_PIPELINE_WEBHOOK_BUILD_LOG) {
                dslContext.insertInto(
                    this,
                    CODE_TYPE,
                    REPO_NAME,
                    COMMIT_ID,
                    REQUEST_CONTENT,
                    CREATED_TIME,
                    RECEIVED_TIME,
                    FINISHED_TIME
                ).values(
                    codeType,
                    repoName,
                    commitId,
                    requestContent,
                    Timestamp(createdTime).toLocalDateTime(),
                    Timestamp(receivedTime).toLocalDateTime(),
                    LocalDateTime.now()
                ).returning(ID)
                    .fetchOne()!!.id
            }
        }
    }

    fun get(
        dslContext: DSLContext,
        id: Long
    ): PipelineWebhookBuildLog? {
        return with(T_PIPELINE_WEBHOOK_BUILD_LOG) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }?.map { convert(it as TPipelineWebhookBuildLogRecord) }
    }

    fun listByPage(
        dslContext: DSLContext,
        repoName: String,
        commitId: String,
        offset: Int,
        limit: Int
    ): List<PipelineWebhookBuildLog> {
        return with(T_PIPELINE_WEBHOOK_BUILD_LOG) {
            dslContext.selectFrom(this)
                .where(REPO_NAME.eq(repoName))
                .and(COMMIT_ID.eq(commitId))
                .limit(offset, limit)
                .fetch()
        }?.map { convert(it) } ?: emptyList()
    }

    fun countByPage(
        dslContext: DSLContext,
        repoName: String,
        commitId: String
    ): Long {
        return with(T_PIPELINE_WEBHOOK_BUILD_LOG) {
            dslContext.selectCount()
                .from(this)
                .where(REPO_NAME.eq(repoName))
                .and(COMMIT_ID.eq(commitId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun convert(record: TPipelineWebhookBuildLogRecord): PipelineWebhookBuildLog {
        return with(record) {
            PipelineWebhookBuildLog(
                id = id,
                codeType = codeType,
                repoName = repoName,
                commitId = commitId,
                requestContent = requestContent,
                createdTime = createdTime.timestampmilli(),
                receivedTime = receivedTime.timestampmilli(),
                finishedTime = finishedTime.timestampmilli()
            )
        }
    }
}
