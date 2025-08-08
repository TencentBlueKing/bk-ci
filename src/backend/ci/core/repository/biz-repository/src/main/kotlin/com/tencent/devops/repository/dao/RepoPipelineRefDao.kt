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

package com.tencent.devops.repository.dao

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.repository.tables.TRepositoryPipelineRef
import com.tencent.devops.model.repository.tables.records.TRepositoryPipelineRefRecord
import com.tencent.devops.repository.pojo.RepoPipelineRef
import com.tencent.devops.repository.pojo.RepoPipelineRefVo
import com.tencent.devops.repository.pojo.enums.RepoAtomCategoryEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class RepoPipelineRefDao {

    fun batchAdd(
        dslContext: DSLContext,
        repoPipelineRefs: Collection<RepoPipelineRef>
    ) {
        if (repoPipelineRefs.isEmpty()) {
            return
        }
        val now = LocalDateTime.now()
        dslContext.batch(repoPipelineRefs.map {
            with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    REPOSITORY_ID,
                    TASK_ID,
                    TASK_NAME,
                    ATOM_CODE,
                    ATOM_CATEGORY,
                    TASK_PARAMS,
                    TASK_REPO_TYPE,
                    TASK_REPO_HASH_ID,
                    TASK_REPO_NAME,
                    TRIGGER_TYPE,
                    EVENT_TYPE,
                    TRIGGER_CONDITION,
                    TRIGGER_CONDITION_MD5,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CHANNEL
                ).values(
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.repositoryId,
                    it.taskId,
                    it.taskName,
                    it.atomCode,
                    it.atomCategory,
                    JsonUtil.toJson(it.taskParams),
                    it.taskRepoType,
                    it.taskRepoHashId,
                    it.taskRepoRepoName,
                    it.triggerType,
                    it.eventType,
                    it.triggerCondition?.let { JsonUtil.toJson(it) },
                    it.triggerConditionMd5,
                    now,
                    now,
                    it.channel
                ).onDuplicateKeyUpdate()
                    .set(TASK_NAME, it.taskName)
                    .set(PIPELINE_NAME, it.pipelineName)
                    .set(REPOSITORY_ID, it.repositoryId)
                    .set(ATOM_CODE, it.atomCode)
                    .set(TRIGGER_TYPE, it.triggerType)
                    .set(EVENT_TYPE, it.eventType)
                    .set(TASK_PARAMS, JsonUtil.toJson(it.taskParams))
                    .set(TASK_REPO_TYPE, it.taskRepoType)
                    .set(TASK_REPO_HASH_ID, it.taskRepoHashId)
                    .set(TASK_REPO_NAME, it.taskRepoRepoName)
                    .set(TRIGGER_CONDITION, it.triggerCondition?.let { JsonUtil.toJson(it) })
                    .set(TRIGGER_CONDITION_MD5, it.triggerConditionMd5)
                    .set(UPDATE_TIME, now)
                    .set(CHANNEL, it.channel)
            }
        }).execute()
    }

    fun batchDelete(dslContext: DSLContext, ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }

        with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(ids))
                .execute()
        }
    }

    fun listByPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): List<TRepositoryPipelineRefRecord> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun listByRepo(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        eventType: String? = null,
        triggerConditionMd5: String? = null,
        taskRepoType: RepositoryType? = null,
        channel: String?,
        limit: Int,
        offset: Int
    ): List<RepoPipelineRefVo> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                REPOSITORY_ID.eq(repositoryId)
            )
            if (!channel.isNullOrBlank()) {
                conditions.add(CHANNEL.eq(channel))
            }
            if (!triggerConditionMd5.isNullOrBlank()) {
                conditions.add(TRIGGER_CONDITION_MD5.eq(triggerConditionMd5))
            }
            if (!eventType.isNullOrBlank()) {
                conditions.add(EVENT_TYPE.eq(eventType))
            }
            if (taskRepoType != null) {
                conditions.add(TASK_REPO_TYPE.eq(taskRepoType.name))
            }
            dslContext.select(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .orderBy(PIPELINE_NAME.desc())
                .limit(limit).offset(offset)
                .fetch {
                    RepoPipelineRefVo(
                        projectId = it.value1(),
                        pipelineId = it.value2(),
                        pipelineName = it.value3()
                    )
                }
        }
    }

    fun countByRepo(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        eventType: String? = null,
        triggerConditionMd5: String? = null,
        taskRepoType: RepositoryType? = null,
        channel: String?
    ): Long {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                REPOSITORY_ID.eq(repositoryId)
            )
            if (!channel.isNullOrBlank()) {
                conditions.add(CHANNEL.eq(channel))
            }
            if (!triggerConditionMd5.isNullOrBlank()) {
                conditions.add(TRIGGER_CONDITION_MD5.eq(triggerConditionMd5))
            }
            if (!eventType.isNullOrBlank()) {
                conditions.add(EVENT_TYPE.eq(eventType))
            }
            if (taskRepoType != null) {
                conditions.add(TASK_REPO_TYPE.eq(taskRepoType.name))
            }
            dslContext.select()
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .fetchGroups(PIPELINE_ID).size.toLong()
        }
    }

    fun listTriggerRefIds(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        triggerType: String?,
        eventType: String?,
        limit: Int,
        offset: Int
    ): Map<Long /*ID*/, Int /*pipelineCount*/> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.select(DSL.max(ID), DSL.count()).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .and(ATOM_CATEGORY.eq(RepoAtomCategoryEnum.TRIGGER.name))
                .let {
                    if (triggerType.isNullOrBlank()) {
                        it
                    } else {
                        it.and(TRIGGER_TYPE.eq(triggerType))
                    }
                }
                .let {
                    if (eventType.isNullOrBlank()) {
                        it
                    } else {
                        it.and(EVENT_TYPE.eq(eventType))
                    }
                }.groupBy(PROJECT_ID, REPOSITORY_ID, EVENT_TYPE, TRIGGER_CONDITION_MD5)
                .limit(limit).offset(offset)
                .fetch().associate { Pair(it.value1(), it.value2()) }
        }
    }

    fun countTriggerRef(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        triggerType: String?,
        eventType: String?
    ): Long {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.select(DSL.countDistinct(PROJECT_ID, REPOSITORY_ID, EVENT_TYPE, TRIGGER_CONDITION_MD5))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .and(ATOM_CATEGORY.eq(RepoAtomCategoryEnum.TRIGGER.name))
                .let {
                    if (triggerType.isNullOrBlank()) {
                        it
                    } else {
                        it.and(TRIGGER_TYPE.eq(triggerType))
                    }
                }
                .let {
                    if (eventType.isNullOrBlank()) {
                        it
                    } else {
                        it.and(EVENT_TYPE.eq(eventType))
                    }
                }
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countPipelineRefs(
        dslContext: DSLContext,
        projectId: String,
        repositoryIds: List<Long>
    ): Map<Long, Int> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.select(PROJECT_ID, REPOSITORY_ID, DSL.count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.`in`(repositoryIds))
                .and(ATOM_CATEGORY.eq(RepoAtomCategoryEnum.TRIGGER.name))
                .groupBy(PROJECT_ID, REPOSITORY_ID)
                .fetch().map { it.value2() to it.value3() }.toMap()
        }
    }

    fun listByIds(
        dslContext: DSLContext,
        ids: List<Long>
    ): List<TRepositoryPipelineRefRecord> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.selectFrom(this).where(ID.`in`(ids)).orderBy(EVENT_TYPE.asc()).fetch()
        }
    }

    fun removeRepositoryPipelineRefsById(
        dslContext: DSLContext,
        repoId: Long
    ): Int {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.deleteFrom(this).where(REPOSITORY_ID.eq(repoId)).execute()
        }
    }
}
