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
 *
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineWebhookVersion
import com.tencent.devops.process.pojo.webhook.PipelineWebhookVersion
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineWebhookVersionDao {

    fun batchSave(dslContext: DSLContext, webhooks: List<PipelineWebhookVersion>) {
        if (webhooks.isEmpty()) {
            return
        }
        with(TPipelineWebhookVersion.T_PIPELINE_WEBHOOK_VERSION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                TASK_ID,
                TASK_PARAMS,
                TASK_REPO_TYPE,
                TASK_REPO_HASH_ID,
                TASK_REPO_NAME,
                REPOSITORY_TYPE,
                REPOSITORY_HASH_ID,
                EVENT_TYPE
            ).also { insert ->
                webhooks.forEach { webhook ->
                    insert.values(
                        webhook.projectId,
                        webhook.pipelineId,
                        webhook.version,
                        webhook.taskId,
                        webhook.taskParams,
                        webhook.taskRepoType?.name,
                        webhook.taskRepoHashId,
                        webhook.taskRepoName,
                        webhook.repositoryType.name,
                        webhook.repositoryHashId,
                        webhook.eventType
                    ).onDuplicateKeyUpdate()
                        .set(TASK_PARAMS, webhook.taskParams)
                        .set(TASK_REPO_TYPE, webhook.taskRepoType?.name)
                        .set(TASK_REPO_HASH_ID, webhook.taskRepoHashId)
                        .set(TASK_REPO_NAME, webhook.taskRepoName)
                        .set(REPOSITORY_TYPE, webhook.repositoryType.name)
                        .set(REPOSITORY_HASH_ID, webhook.repositoryHashId)
                        .set(EVENT_TYPE, webhook.eventType)
                }
            }.execute()
        }
    }

    fun getTaskIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        repoHashId: String,
        eventType: String
    ): List<String>? {
        return with(TPipelineWebhookVersion.T_PIPELINE_WEBHOOK_VERSION) {
            dslContext.select(TASK_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(REPOSITORY_HASH_ID.eq(repoHashId))
                .and(EVENT_TYPE.eq(eventType))
                .fetch(0, String::class.java)
        }
    }
}
