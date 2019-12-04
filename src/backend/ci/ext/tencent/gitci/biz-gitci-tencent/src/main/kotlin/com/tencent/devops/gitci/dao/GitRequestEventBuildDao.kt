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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.dao

import com.tencent.devops.gitci.pojo.BranchBuilds
import com.tencent.devops.model.gitci.tables.TGitRequestEvent
import com.tencent.devops.model.gitci.tables.TGitRequestEventBuild
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventBuildRecord
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitRequestEventBuildDao {

    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String,
        normalizedYaml: String,
        gitProjectId: Long,
        branch: String,
        objectKind: String,
        description: String?
    ): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val record = dslContext.insertInto(this,
                    EVENT_ID,
                    ORIGIN_YAML,
                    NORMALIZED_YAML,
                    GIT_PROJECT_ID,
                    BRANCH,
                    OBJECT_KIND,
                    DESCRIPTION,
                    CREATE_TIME
                ).values(
                    eventId,
                    originYaml,
                    normalizedYaml,
                    gitProjectId,
                    branch,
                    objectKind,
                    description,
                    LocalDateTime.now()
            ).returning(ID)
            .fetchOne()
            return record.id
        }
    }

    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String,
        normalizedYaml: String,
        pipelineId: String,
        buildId: String,
        gitProjectId: Long,
        branch: String,
        objectKind: String,
        description: String?
    ): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val record = dslContext.insertInto(this,
                    EVENT_ID,
                    ORIGIN_YAML,
                    NORMALIZED_YAML,
                    PIPELINE_ID,
                    BUILD_ID,
                    GIT_PROJECT_ID,
                    BRANCH,
                    OBJECT_KIND,
                    DESCRIPTION,
                    CREATE_TIME
            ).values(
                    eventId,
                    originYaml,
                    normalizedYaml,
                    pipelineId,
                    buildId,
                    gitProjectId,
                    branch,
                    objectKind,
                    description,
                    LocalDateTime.now()
            ).returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun update(
        dslContext: DSLContext,
        eventId: Long,
        pipelineId: String,
        buildId: String
    ) {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            dslContext.update(this)
                    .set(PIPELINE_ID, pipelineId)
                    .set(BUILD_ID, buildId)
                    .where(EVENT_ID.eq(eventId))
                    .execute()
        }
    }

    fun getByBuildId(
        dslContext: DSLContext,
        buildId: String
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .fetchOne()
        }
    }

    fun getEventByBuildId(
        dslContext: DSLContext,
        buildId: String
    ): Record? {
        val t1 = TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD.`as`("t1")
        val t2 = TGitRequestEvent.T_GIT_REQUEST_EVENT.`as`("t2")
        return dslContext.select(t2.OBJECT_KIND, t2.COMMIT_ID, t2.GIT_PROJECT_ID, t2.MERGE_REQUEST_ID, t2.DESCRIPTION, t2.EVENT)
            .from(t2).leftJoin(t1).on(t1.EVENT_ID.eq(t2.ID))
            .where(t1.BUILD_ID.eq(buildId))
            .fetchOne()
    }

    fun getByEventIds(
        dslContext: DSLContext,
        eventIds: Set<Long>
    ): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                    .where(EVENT_ID.`in`(eventIds))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .fetch()
        }
    }

    fun getByEventId(
        dslContext: DSLContext,
        eventId: Long
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                    .where(EVENT_ID.eq(eventId))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .fetchAny()
        }
    }

    fun getLatestBuild(
        dslContext: DSLContext,
        gitProjectId: Long
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .and(BUILD_ID.isNotNull)
                    .and(BUILD_ID.notEqual(""))
                    .orderBy(EVENT_ID.desc())
                    .fetchAny()
        }
    }

    fun getBranchBuildList(
        dslContext: DSLContext,
        gitProjectId: Long
    ): List<BranchBuilds> {
        val sql = "SELECT BRANCH, SUBSTRING_INDEX(GROUP_CONCAT(BUILD_ID ORDER BY EVENT_ID DESC), ',', 5) as BUILD_IDS, SUBSTRING_INDEX(GROUP_CONCAT(EVENT_ID ORDER BY EVENT_ID DESC), ',', 5) as EVENT_IDS, COUNT(BUILD_ID) as BUILD_TOTAL\n" +
                "FROM T_GIT_REQUEST_EVENT_BUILD\n" +
                "WHERE BUILD_ID IS NOT NULL AND GIT_PROJECT_ID = $gitProjectId \n" +
                "GROUP BY BRANCH\n" +
                "order by EVENT_ID desc"
        val result = dslContext.fetch(sql)
        return if (null == result || result.isEmpty()) {
            emptyList()
        } else {
            val branchBuildsList = mutableListOf<BranchBuilds>()
            result.forEach {
                val branchBuilds = BranchBuilds(
                        it.getValue("BRANCH") as String,
                        it.getValue("BUILD_TOTAL") as Long,
                        it.getValue("BUILD_IDS") as String,
                        it.getValue("EVENT_IDS") as String
                )
                branchBuildsList.add(branchBuilds)
            }
            branchBuildsList
        }
    }

    fun getMergeRequestBuildList(dslContext: DSLContext, gitProjectId: Long, page: Int, pageSize: Int): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .and(OBJECT_KIND.eq("merge_request"))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun getMergeRequestBuildCount(dslContext: DSLContext, gitProjectId: Long): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectCount()
                    .from(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .and(OBJECT_KIND.eq("merge_request"))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .fetchOne(0, Long::class.java)
        }
    }

    fun getRequestEventBuildCount(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectCount()
                    .from(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .fetchOne(0, Long::class.java)
        }
    }

    fun getRequestEventBuildList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int,
        pageSize: Int
    ): List<TGitRequestEventBuildRecord> {
        return with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            dslContext.selectFrom(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .and(BUILD_ID.isNotNull)
                    .orderBy(EVENT_ID.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }
}
