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

import com.tencent.devops.model.stream.tables.TGitRequestEventNotBuild
import com.tencent.devops.model.stream.tables.records.TGitRequestEventNotBuildRecord
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class GitRequestEventNotBuildDao {

    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String?,
        reasonDetail: String?,
        pipelineId: String?,
        filePath: String?,
        gitProjectId: Long,
        version: String?,
        branch: String?
    ): Long {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val record = dslContext.insertInto(
                this,
                EVENT_ID,
                ORIGIN_YAML,
                PIPELINE_ID,
                FILE_PATH,
                PARSED_YAML,
                NORMALIZED_YAML,
                REASON,
                REASON_DETAIL,
                GIT_PROJECT_ID,
                CREATE_TIME,
                VERSION,
                BRANCH
            ).values(
                eventId,
                originYaml,
                pipelineId,
                filePath,
                parsedYaml,
                normalizedYaml,
                reason,
                reasonDetail,
                gitProjectId,
                LocalDateTime.now(),
                version,
                branch
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun getRequestNoBuildsByEventId(
        dslContext: DSLContext,
        eventId: Long
    ): List<TGitRequestEventNotBuildRecord> {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .fetch()
        }
    }

    fun getListByEventIds(
        dslContext: DSLContext,
        eventIds: Set<Long>
    ): List<TGitRequestEventNotBuildRecord> {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .fetch()
        }
    }

    fun deleteNoBuildsById(
        dslContext: DSLContext,
        recordId: Long
    ): Boolean {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.delete(this)
                .where(ID.eq(recordId))
                .execute() == 1
        }
    }

    fun updateNoBuildReasonByRecordId(
        dslContext: DSLContext,
        recordId: Long,
        reason: String,
        reasonDetail: String
    ): Boolean {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.update(this)
                .set(REASON, reason)
                .set(REASON_DETAIL, reasonDetail)
                .where(ID.eq(recordId))
                .execute() == 1
        }
    }

    fun deleteNotBuildByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): Int {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun getProjectAfterId(dslContext: DSLContext, startId: Long, limit: Int): List<TGitRequestEventNotBuildRecord> {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectFrom(this)
                .where(ID.gt(startId))
                .limit(limit)
                .fetch()
        }
    }

    fun batchUpdateBuild(dslContext: DSLContext, builds: List<TGitRequestEventNotBuildRecord>) {
        dslContext.batchUpdate(builds).execute()
    }

    fun deleteByEventId(
        dslContext: DSLContext,
        gitProjectId: Long,
        eventId: Long
    ): Int {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.deleteFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId)).and(EVENT_ID.eq(eventId))
                .execute()
        }
    }

    fun getCountByProjectId(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Int {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getIdByProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        handlePageSize: Int
    ): Result<out Record>? {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val conditions = mutableListOf<Condition>()
            conditions.add(GIT_PROJECT_ID.eq(gitProjectId))
            val baseStep = dslContext.select(ID)
                .from(this)
                .where(conditions)
                .orderBy(ID.asc())
            return baseStep.limit(handlePageSize).fetch()
        }
    }

    fun deleteByIds(
        dslContext: DSLContext,
        ids: Set<Long>
    ): Int {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.delete(this)
                .where(ID.`in`(ids)).execute()
        }
    }

    fun getLatestBuild(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String?
    ): TGitRequestEventNotBuildRecord? {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val query = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!pipelineId.isNullOrBlank()) query.and(PIPELINE_ID.eq(pipelineId))
            return query.orderBy(EVENT_ID.desc())
                .fetchAny()
        }
    }

    fun getPipelinesLastBuild(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): List<Pair<String, String>>? {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val t2 = dslContext.select(DSL.max(ID).`as`("iid"))
                .from(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .groupBy(PIPELINE_ID).asTable("t2")
            return dslContext.select(PIPELINE_ID, BRANCH).from(this)
                .innerJoin(
                    t2
                ).on(ID.eq(t2.field("iid", Long::class.java)))
                .fetch {
                    Pair(it.value1(), it.value2())
                }
        }
    }
}
