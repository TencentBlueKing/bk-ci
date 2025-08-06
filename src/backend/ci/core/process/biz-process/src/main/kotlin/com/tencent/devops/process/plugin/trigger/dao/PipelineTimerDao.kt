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

package com.tencent.devops.process.plugin.trigger.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.model.process.Tables.T_PIPELINE_TIMER
import com.tencent.devops.model.process.tables.records.TPipelineTimerRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
open class PipelineTimerDao {

    open fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        crontabExpression: String,
        channelCode: ChannelCode,
        repoHashId: String?,
        branchs: String?,
        noScm: Boolean?,
        startParam: String?,
        taskId: String
    ): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                CREATE_TIME,
                CREATOR,
                CRONTAB,
                CHANNEL,
                REPO_HASH_ID,
                BRANCHS,
                NO_SCM,
                START_PARAM,
                TASK_ID
            ).values(
                projectId,
                pipelineId,
                LocalDateTime.now(),
                userId,
                crontabExpression,
                channelCode.name,
                repoHashId,
                branchs,
                noScm,
                startParam,
                taskId
            )
                .onDuplicateKeyUpdate()
                .set(TASK_ID, taskId)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(CREATOR, userId)
                .set(CRONTAB, crontabExpression)
                .set(CHANNEL, channelCode.name)
                .set(REPO_HASH_ID, repoHashId)
                .set(BRANCHS, branchs)
                .set(NO_SCM, noScm)
                .set(START_PARAM, startParam)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Result<TPipelineTimerRecord> {
        return with(T_PIPELINE_TIMER) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId), PIPELINE_ID.eq(pipelineId))
            dslContext.selectFrom(this)
                    .where(conditions)
                    .orderBy(CREATE_TIME)
                    .fetch()
        }
    }

    open fun get(dslContext: DSLContext, projectId: String, pipelineId: String, taskId: String): TPipelineTimerRecord? {
        return with(T_PIPELINE_TIMER) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId).and(TASK_ID.eq(taskId))))
                .fetchAny()
        }
    }

    open fun delete(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.delete(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    open fun delete(dslContext: DSLContext, projectId: String, pipelineId: String, taskId: String): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.delete(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId).and(TASK_ID.eq(taskId))))
                .execute()
        }
    }

    open fun list(dslContext: DSLContext, offset: Int, limit: Int): Result<TPipelineTimerRecord> {
        return with(T_PIPELINE_TIMER) {
            dslContext.selectFrom(this).limit(offset, limit).skipCheck().fetch()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, pipelineId: String): Result<TPipelineTimerRecord> {
        return with(T_PIPELINE_TIMER) {
            dslContext.selectFrom(this).where(
                PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId))
            ).fetch()
        }
    }

    fun listPipeline(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String?,
        offset: Int,
        limit: Int
    ): List<Pair<String, String>> {
        return with(T_PIPELINE_TIMER) {
            val conditions = mutableListOf<Condition>()
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            dslContext.select(PROJECT_ID, PIPELINE_ID)
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, PIPELINE_ID)
                .orderBy(PROJECT_ID, PIPELINE_ID)
                .limit(offset, limit)
                .skipCheck()
                .fetch()
                .map {
                    it.value1() to it.value2()
                }
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String,
        startParam: String?
    ): Int {
        return with(T_PIPELINE_TIMER) {
            dslContext.update(this)
                .set(TASK_ID, taskId)
                .let {
                    if (!startParam.isNullOrBlank()) {
                        it.set(START_PARAM, startParam)
                    }
                    it
                }
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }
}
