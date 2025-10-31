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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK
import com.tencent.devops.model.process.tables.records.TPipelineWebhookRecord
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class PipelineWebhookDao {

    fun save(dslContext: DSLContext, pipelineWebhook: PipelineWebhook): Int {
        logger.info("save the pipeline webhook=$pipelineWebhook")
        return with(pipelineWebhook) {
            with(T_PIPELINE_WEBHOOK) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    REPOSITORY_TYPE,
                    REPO_TYPE,
                    REPO_HASH_ID,
                    REPO_NAME,
                    PROJECT_NAME,
                    TASK_ID,
                    EVENT_TYPE,
                    REPOSITORY_HASH_ID,
                    EXTERNAL_ID,
                    EXTERNAL_NAME
                )
                    .values(
                        projectId,
                        pipelineId,
                        repositoryType.name,
                        repoType?.name,
                        repoHashId,
                        repoName,
                        projectName,
                        taskId,
                        eventType,
                        repositoryHashId,
                        externalId,
                        externalName
                    )
                    .onDuplicateKeyUpdate()
                    .set(REPO_TYPE, repoType?.name)
                    .set(REPO_HASH_ID, repoHashId)
                    .set(REPO_NAME, repoName)
                    .set(PROJECT_NAME, projectName)
                    .set(EVENT_TYPE, eventType)
                    .set(REPOSITORY_HASH_ID, repositoryHashId)
                    .set(EXTERNAL_ID, externalId)
                    .set(EXTERNAL_NAME, externalName)
                    .execute()
            }
        }
    }

    fun update(
        dslContext: DSLContext,
        pipelineWebhook: PipelineWebhook
    ): Int {
        return with(pipelineWebhook) {
            with(T_PIPELINE_WEBHOOK) {
                dslContext.update(this)
                    .set(REPOSITORY_TYPE, repositoryType.name)
                    .set(REPO_TYPE, repoType?.name)
                    .set(REPO_HASH_ID, repoHashId)
                    .set(REPO_NAME, repoName)
                    .set(PROJECT_NAME, projectName)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(TASK_ID.eq(taskId))
                    .execute()
            }
        }
    }

    fun getPipelineWebHooksByRepositoryType(
        dslContext: DSLContext,
        repositoryType: String,
        offset: Int,
        limit: Int
    ): Result<TPipelineWebhookRecord> {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.selectFrom(this).where(REPOSITORY_TYPE.eq(repositoryType)).limit(offset, limit).fetch()
        }
    }

    fun deleteByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteByTaskId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): Int {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(TASK_ID.eq(taskId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun convert(it: TPipelineWebhookRecord): PipelineWebhook {
        return with(it) {
            PipelineWebhook(
                projectId = projectId,
                pipelineId = pipelineId,
                repositoryType = ScmType.valueOf(repositoryType),
                repoType = convertRepoType(repoType),
                repoHashId = repoHashId,
                repoName = repoName,
                id = id,
                projectName = projectName,
                taskId = taskId,
                repositoryHashId = repositoryHashId
            )
        }
    }

    fun getByProjectNamesAndType(
        dslContext: DSLContext,
        projectNames: Set<String>,
        repositoryType: String,
        yamlPipelineIds: List<String>?
    ): List<WebhookTriggerPipeline>? {
        with(T_PIPELINE_WEBHOOK) {
            return dslContext.select(PROJECT_ID, PIPELINE_ID).from(this)
                .where(PROJECT_NAME.`in`(projectNames))
                .and(REPOSITORY_TYPE.eq(repositoryType))
                .and(DELETE.eq(false))
                .let {
                    if (yamlPipelineIds.isNullOrEmpty()) {
                        it
                    } else {
                        it.and(PIPELINE_ID.notIn(yamlPipelineIds))
                    }
                }
                .groupBy(PROJECT_ID, PIPELINE_ID)
                .fetch().map {
                    WebhookTriggerPipeline(
                        projectId = it.value1(),
                        pipelineId = it.value2()
                    )
                }
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repositoryHashId: String,
        eventType: String
    ): PipelineWebhook? {
        with(T_PIPELINE_WEBHOOK) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(REPOSITORY_HASH_ID.eq(repositoryHashId))
                .and(EVENT_TYPE.eq(eventType))
                .fetchAny()
            return record?.map { convert(record) }
        }
    }

    fun listTriggerPipeline(
        dslContext: DSLContext,
        projectId: String,
        repositoryHashId: String,
        eventType: String
    ): List<WebhookTriggerPipeline>? {
        with(T_PIPELINE_WEBHOOK) {
            return dslContext.select(PROJECT_ID, PIPELINE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_HASH_ID.eq(repositoryHashId))
                .and(EVENT_TYPE.eq(eventType))
                .and(DELETE.eq(false))
                .groupBy(PROJECT_ID, PIPELINE_ID)
                .fetch().map {
                    WebhookTriggerPipeline(
                        projectId = it.value1(),
                        pipelineId = it.value2()
                    )
                }
        }
    }

    fun updateProjectNameAndTaskId(
        dslContext: DSLContext,
        projectId: String,
        projectName: String,
        taskId: String,
        id: Long
    ) {
        with(T_PIPELINE_WEBHOOK) {
            dslContext.update(this)
                .set(PROJECT_NAME, projectName)
                .set(TASK_ID, taskId)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ): Int {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.update(this)
                .set(DELETE, true)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    private fun convertRepoType(repoType: String?): RepositoryType? {
        if (repoType.isNullOrBlank()) {
            return null
        }
        return try {
            RepositoryType.valueOf(repoType)
        } catch (e: Exception) {
            logger.warn("Fail to convert the repo type - ($repoType)")
            null
        }
    }

    fun listWebhook(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String?,
        ignoredRepoTypes: Set<String>? = null,
        offset: Int,
        limit: Int
    ): List<PipelineWebhook>? {
        return with(T_PIPELINE_WEBHOOK) {
            val conditions = mutableListOf(DELETE.eq(false))
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!ignoredRepoTypes.isNullOrEmpty()) {
                conditions.add(REPOSITORY_TYPE.notIn(ignoredRepoTypes))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(PIPELINE_ID.desc())
                .limit(offset, limit)
                .fetch()
        }?.map { convert(it) }
    }

    fun groupPipelineList(
        dslContext: DSLContext,
        projectId: String?,
        offset: Int,
        limit: Int
    ): List<WebhookTriggerPipeline> {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.select(PROJECT_ID, PIPELINE_ID).from(this)
                .where(DELETE.eq(false))
                .let {
                    if (projectId.isNullOrBlank()) {
                        it
                    } else {
                        it.and(PROJECT_ID.eq(projectId))
                    }
                }
                .groupBy(PROJECT_ID, PIPELINE_ID)
                .limit(offset, limit)
                .fetch().map {
                    WebhookTriggerPipeline(
                        projectId = it.value1(),
                        pipelineId = it.value2()
                    )
                }
        }
    }

    fun updateWebhookEventInfo(
        dslContext: DSLContext,
        repositoryHashId: String?,
        eventType: String,
        externalId: String?,
        externalName: String?,
        pipelineId: String,
        projectId: String,
        taskId: String
    ) {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.update(this)
                .set(REPOSITORY_HASH_ID, repositoryHashId)
                .set(EVENT_TYPE, eventType)
                .set(EXTERNAL_ID, externalId)
                .set(EXTERNAL_NAME, externalName)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    fun updateProjectName(
        dslContext: DSLContext,
        projectName: String,
        pipelineId: String,
        projectId: String,
        taskId: String
    ): Int {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.update(this)
                    .set(PROJECT_NAME, projectName)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebhookDao::class.java)
    }
}
