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

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.stream.tables.TGitPipelineRepoResource
import com.tencent.devops.model.stream.tables.TGitRequestEvent
import com.tencent.devops.model.stream.tables.TGitRequestEventBuild
import com.tencent.devops.model.stream.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.stream.pojo.BranchBuilds
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Suppress("ComplexCondition", "ComplexMethod")
@Repository
class GitRequestEventBuildDao {
    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String,
        parsedYaml: String,
        normalizedYaml: String,
        gitProjectId: Long,
        branch: String,
        objectKind: String,
        triggerUser: String,
        commitMsg: String?,
        sourceGitProjectId: Long?,
        buildStatus: BuildStatus,
        version: String?
    ): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val record = dslContext.insertInto(
                this,
                EVENT_ID,
                ORIGIN_YAML,
                PARSED_YAML,
                NORMALIZED_YAML,
                GIT_PROJECT_ID,
                BRANCH,
                OBJECT_KIND,
                COMMIT_MESSAGE,
                TRIGGER_USER,
                CREATE_TIME,
                SOURCE_GIT_PROJECT_ID,
                BUILD_STATUS,
                VERSION
            ).values(
                eventId,
                originYaml,
                parsedYaml,
                normalizedYaml,
                gitProjectId,
                branch,
                objectKind,
                commitMsg,
                triggerUser,
                LocalDateTime.now(),
                sourceGitProjectId,
                buildStatus.name,
                version
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun retryUpdate(
        dslContext: DSLContext,
        gitBuildId: Long
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
                return dslContext.update(this)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(gitBuildId))
                    .execute()
            }
        }
    }

    fun removeBuild(
        dslContext: DSLContext,
        gitBuildId: Long
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
                return dslContext.delete(this)
                    .where(ID.eq(gitBuildId))
                    .execute()
            }
        }
    }

    fun update(
        dslContext: DSLContext,
        gitBuildId: Long,
        pipelineId: String,
        buildId: String,
        version: String?
    ) {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            dslContext.update(this)
                .set(PIPELINE_ID, pipelineId)
                .set(BUILD_ID, buildId)
                .set(VERSION, version)
                .where(ID.eq(gitBuildId))
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
                .fetchAny()
        }
    }

    fun getEventByBuildId(
        dslContext: DSLContext,
        buildId: String
    ): Record? {
        val t1 = TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD.`as`("t1")
        val t2 = TGitRequestEvent.T_GIT_REQUEST_EVENT.`as`("t2")
        return dslContext.select(
            t2.OBJECT_KIND, t2.COMMIT_ID, t2.GIT_PROJECT_ID, t2.MERGE_REQUEST_ID,
            t2
                .COMMIT_MESSAGE,
            t2.EVENT, t2.SOURCE_GIT_PROJECT_ID, t1.PIPELINE_ID, t1.ID
        )
            .from(t2).leftJoin(t1).on(t1.EVENT_ID.eq(t2.ID))
            .where(t1.BUILD_ID.eq(buildId))
            .fetchAny()
    }

    fun getByEventIds(
        dslContext: DSLContext,
        eventIds: Collection<Long>
    ): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .and(BUILD_ID.isNotNull)
                .fetch()
        }
    }

    fun getIdByEventIds(
        dslContext: DSLContext,
        eventIds: Collection<Long>
    ): List<Long> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.select(EVENT_ID).from(this)
                .where(EVENT_ID.`in`(eventIds)).fetch(EVENT_ID)
        }
    }

    fun getByGitBuildId(
        dslContext: DSLContext,
        gitBuildId: Long
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(ID.eq(gitBuildId))
                .fetchAny()
        }
    }

    fun getLatestBuild(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String?
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val query = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.notEqual(""))
            if (!pipelineId.isNullOrBlank()) query.and(PIPELINE_ID.eq(pipelineId))
            return query.orderBy(EVENT_ID.desc())
                .fetchAny()
        }
    }

    fun getBuildInLastDays(
        dslContext: DSLContext,
        buildDays: Long,
        offset: Int,
        limit: Int
    ): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(CREATE_TIME.gt(LocalDateTime.now().minusDays(buildDays)))
                .and(VERSION.eq("v2.0"))
                .limit(offset, limit)
                .fetch()
        }
    }

    fun getBranchBuildList(
        dslContext: DSLContext,
        gitProjectId: Long
    ): List<BranchBuilds> {
        val sql = "SELECT BRANCH, GIT_PROJECT_ID, SOURCE_GIT_PROJECT_ID, \n" +
            "SUBSTRING_INDEX(GROUP_CONCAT(BUILD_ID ORDER BY EVENT_ID DESC), ',', 5) " +
            "as BUILD_IDS, SUBSTRING_INDEX(GROUP_CONCAT(EVENT_ID ORDER BY EVENT_ID DESC), ',', 5) " +
            "as EVENT_IDS, COUNT(BUILD_ID) as BUILD_TOTAL\n" +
            "FROM T_GIT_REQUEST_EVENT_BUILD\n" +
            "WHERE BUILD_ID IS NOT NULL AND GIT_PROJECT_ID = $gitProjectId \n" +
            "GROUP BY BRANCH, SOURCE_GIT_PROJECT_ID\n" +
            "order by EVENT_ID desc"
        val result = dslContext.fetch(sql)
        return if (result.isEmpty()) {
            emptyList()
        } else {
            val branchBuildsList = mutableListOf<BranchBuilds>()
            result.forEach {
                val branchBuilds = BranchBuilds(
                    it.getValue("BRANCH") as String,
                    it.getValue("BUILD_TOTAL") as Long,
                    it.getValue("BUILD_IDS") as String,
                    it.getValue("EVENT_IDS") as String,
                    it.getValue("GIT_PROJECT_ID") as Long,
                    if (it.getValue("SOURCE_GIT_PROJECT_ID") == null) {
                        null
                    } else {
                        it.getValue("SOURCE_GIT_PROJECT_ID") as Long
                    }
                )
                branchBuildsList.add(branchBuilds)
            }
            branchBuildsList
        }
    }

    fun getAllBuildBranchList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): List<TGitRequestEventBuildRecord> {
        var buildRecords: List<TGitRequestEventBuildRecord>
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.isNotNull)
                .and(GIT_PROJECT_ID.eq(gitProjectId))
            if (!keyword.isNullOrBlank()) {
                // 针对fork库的特殊分支名 namespace:branchName 进行查询
                if (keyword.contains(":")) {
                    dsl.and(BRANCH.like("%${keyword.split(":")[1]}%"))
                        .and(SOURCE_GIT_PROJECT_ID.isNotNull)
                        .and(SOURCE_GIT_PROJECT_ID.notEqual(gitProjectId))
                } else {
                    dsl.and(BRANCH.like("%$keyword%"))
                }
            }
            dsl.groupBy(BRANCH)
            dsl.groupBy(SOURCE_GIT_PROJECT_ID)
            dsl.orderBy(EVENT_ID.desc())
            buildRecords = if (null != page && page > 0 && null != pageSize && pageSize > 0) {
                dsl.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                dsl.fetch()
            }
        }
        if (buildRecords.isEmpty()) {
            return emptyList()
        }
        return buildRecords
    }

    fun getRequestBuildsByEventId(dslContext: DSLContext, eventId: Long): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .fetch()
        }
    }

    fun getRequestEventBuildListCount(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: String?,
        sourceGitProjectId: Long?,
        triggerUser: String?,
        pipelineId: String?,
        event: String?
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BUILD_ID.isNotNull)
            if (!branchName.isNullOrBlank()) {
                // 针对fork库的特殊分支名 namespace:branchName 进行查询
                if (sourceGitProjectId != null && branchName.contains(":")) {
                    dsl.and(BRANCH.eq(branchName.split(":")[1]))
                        .and(SOURCE_GIT_PROJECT_ID.eq(sourceGitProjectId))
                } else {
                    dsl.and(BRANCH.eq(branchName))
                }
            }
            if (!triggerUser.isNullOrBlank()) {
                dsl.and(TRIGGER_USER.eq(triggerUser))
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!event.isNullOrBlank()) {
                dsl.and(OBJECT_KIND.eq(event))
            }
            return dsl.fetchOne(0, Int::class.java)!!
        }
    }

    fun getRequestEventBuildList(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: String? = null,
        sourceGitProjectId: Long? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        event: String? = null,
        limit: Int,
        offset: Int
    ): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return getRequestEventBuildRecords(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                branchName = branchName,
                sourceGitProjectId = sourceGitProjectId,
                triggerUser = triggerUser,
                pipelineId = pipelineId,
                event = event
            ).orderBy(EVENT_ID.desc()).limit(limit).offset(offset)
                .fetch()
        }
    }

    private fun getRequestEventBuildRecords(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: String? = null,
        sourceGitProjectId: Long? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        event: String? = null
    ): SelectConditionStep<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BUILD_ID.isNotNull)
            if (!branchName.isNullOrBlank()) {
                // 针对fork库的特殊分支名 namespace:branchName 进行查询
                if (sourceGitProjectId != null && branchName.contains(":")) {
                    dsl.and(BRANCH.eq(branchName.split(":")[1]))
                        .and(SOURCE_GIT_PROJECT_ID.eq(sourceGitProjectId))
                } else {
                    dsl.and(BRANCH.eq(branchName))
                }
            }
            if (!triggerUser.isNullOrBlank()) {
                dsl.and(TRIGGER_USER.eq(triggerUser))
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!event.isNullOrBlank()) {
                dsl.and(OBJECT_KIND.eq(event))
            }
            return dsl
        }
    }

    fun getTriggerUserByGitProjectID(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<Record1<String>> {
        val offset = (page - 1) * pageSize
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val requestEventBuildTriggerUsers = dslContext.select(TRIGGER_USER)
                .from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .groupBy(TRIGGER_USER)
                .orderBy(CREATE_TIME.desc())
                .asTable()
            return dslContext.selectFrom(requestEventBuildTriggerUsers).limit(pageSize).offset(offset).fetch()
        }
    }

    fun getLastEventByPipelineId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        branch: String? = null,
        objectKind: String? = null
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!branch.isNullOrBlank()) {
                dsl.and(BRANCH.eq(branch))
            }
            if (!objectKind.isNullOrBlank()) {
                dsl.and(OBJECT_KIND.eq(objectKind))
            }
            return dsl.orderBy(ID.desc())
                .limit(1)
                .fetchAny()
        }
    }

    fun getLastBuildEventByPipelineId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        branch: String? = null,
        objectKind: String? = null
    ): TGitRequestEventBuildRecord? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_STATUS.eq(BuildStatus.FAILED.name).or(BUILD_STATUS.eq(BuildStatus.SUCCEED.name)))
            if (!branch.isNullOrBlank()) {
                dsl.and(BRANCH.eq(branch))
            }
            if (!objectKind.isNullOrBlank()) {
                dsl.and(OBJECT_KIND.eq(objectKind))
            }
            return dsl.orderBy(ID.desc())
                .limit(1)
                .fetchAny()
        }
    }

    fun updateBuildStatusById(
        dslContext: DSLContext,
        id: Long,
        buildStatus: BuildStatus
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.update(this)
                .set(BUILD_STATUS, buildStatus.name)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getRequestEventBuildListMultiple(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: Set<String>?,
        sourceGitProjectId: Set<String>?,
        triggerUser: Set<String>?,
        pipelineId: String?,
        event: Set<String>?,
        commitMsg: String?,
        buildStatus: Set<String>?,
        limit: Int,
        offset: Int,
        pipelineIds: Set<String>?,
        buildIds: Set<String>?
    ): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return getRequestEventBuildListMultiple(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                branchName = branchName,
                sourceGitProjectId = sourceGitProjectId,
                triggerUser = triggerUser,
                pipelineId = pipelineId,
                event = event,
                commitMsg = commitMsg,
                buildStatus = buildStatus,
                pipelineIds = pipelineIds,
                buildIds = buildIds
            ).orderBy(EVENT_ID.desc(), CREATE_TIME.desc()).limit(limit).offset(offset).fetch()
        }
    }

    fun getRequestEventBuildListMultipleCount(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: Set<String>?,
        sourceGitProjectId: Set<String>?,
        triggerUser: Set<String>?,
        pipelineId: String?,
        event: Set<String>?,
        commitMsg: String?,
        buildStatus: Set<String>?,
        pipelineIds: Set<String>?,
        buildIds: Set<String>?
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BUILD_ID.isNotNull)
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!branchName.isNullOrEmpty()) {
                val branchList = branchName.map {
                    // 针对fork库的特殊分支名 namespace:branchName 进行查询
                    if (it.contains(":")) {
                        it.split(":")[1]
                    } else {
                        it
                    }
                }.toSet()
                if (!sourceGitProjectId.isNullOrEmpty()) {
                    dsl.and(BRANCH.`in`(branchList))
                        .and(SOURCE_GIT_PROJECT_ID.`in`(sourceGitProjectId).or(SOURCE_GIT_PROJECT_ID.isNull))
                } else {
                    dsl.and(BRANCH.`in`(branchList))
                }
            }
            if (!triggerUser.isNullOrEmpty()) {
                dsl.and(TRIGGER_USER.`in`(triggerUser))
            }
            if (!event.isNullOrEmpty()) {
                dsl.and(OBJECT_KIND.`in`(event))
            }
            if (!commitMsg.isNullOrBlank()) {
                dsl.and(COMMIT_MESSAGE.like("%$commitMsg%"))
            }
            if (!buildStatus.isNullOrEmpty()) {
                dsl.and(BUILD_STATUS.`in`(buildStatus))
            }
            if (!pipelineIds.isNullOrEmpty()) {
                dsl.and(PIPELINE_ID.`in`(pipelineIds))
            }
            if (!buildIds.isNullOrEmpty()) {
                dsl.and(BUILD_ID.`in`(buildIds))
            }
            return dsl.fetchOne(0, Int::class.java)!!
        }
    }

    private fun getRequestEventBuildListMultiple(
        dslContext: DSLContext,
        gitProjectId: Long,
        branchName: Set<String>?,
        sourceGitProjectId: Set<String>?,
        triggerUser: Set<String>?,
        pipelineId: String?,
        event: Set<String>?,
        commitMsg: String?,
        buildStatus: Set<String>?,
        pipelineIds: Set<String>?,
        buildIds: Set<String>?
    ): SelectConditionStep<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(BUILD_ID.isNotNull)
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!branchName.isNullOrEmpty()) {
                val branchList = branchName.map {
                    // 针对fork库的特殊分支名 namespace:branchName 进行查询
                    if (it.contains(":")) {
                        it.split(":")[1]
                    } else {
                        it
                    }
                }.toSet()
                if (!sourceGitProjectId.isNullOrEmpty()) {
                    dsl.and(BRANCH.`in`(branchList))
                        .and(SOURCE_GIT_PROJECT_ID.`in`(sourceGitProjectId).or(SOURCE_GIT_PROJECT_ID.isNull))
                } else {
                    dsl.and(BRANCH.`in`(branchList))
                }
            }
            if (!triggerUser.isNullOrEmpty()) {
                dsl.and(TRIGGER_USER.`in`(triggerUser))
            }
            if (!event.isNullOrEmpty()) {
                dsl.and(OBJECT_KIND.`in`(event))
            }
            if (!commitMsg.isNullOrBlank()) {
                dsl.and(COMMIT_MESSAGE.like("%$commitMsg%"))
            }
            if (!buildStatus.isNullOrEmpty()) {
                dsl.and(BUILD_STATUS.`in`(buildStatus))
            }
            if (!pipelineIds.isNullOrEmpty()) {
                dsl.and(PIPELINE_ID.`in`(pipelineIds))
            }
            if (!buildIds.isNullOrEmpty()) {
                dsl.and(BUILD_ID.`in`(buildIds))
            }
            return dsl
        }
    }

    fun getProjectLocalBranches(
        dslContext: DSLContext,
        projectId: Long,
        branchName: String?,
        pageNotNull: Int,
        pageSizeNotNull: Int
    ): List<String> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = pageNotNull, pageSize = pageSizeNotNull)
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.select(BRANCH).from(this)
                .where(GIT_PROJECT_ID.eq(projectId))
            if (!branchName.isNullOrBlank()) {
                dsl.and(BRANCH.like("%$branchName%"))
            }
            return dsl.groupBy(BRANCH).orderBy(ID).limit(sqlLimit.limit).offset(sqlLimit.offset).fetch()
                .map { it.value1() }
        }
    }

    fun deleteBuildByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun getProjectAfterId(dslContext: DSLContext, startId: Long, limit: Int): List<TGitRequestEventBuildRecord> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(ID.gt(startId))
                .limit(limit)
                .fetch()
        }
    }

    fun batchUpdateBuild(dslContext: DSLContext, builds: List<TGitRequestEventBuildRecord>) {
        dslContext.batchUpdate(builds).execute()
    }

    fun lastBuildByProject(dslContext: DSLContext, gitProjectIds: Set<Long>?): List<TGitRequestEventBuildRecord> {
        if (gitProjectIds.isNullOrEmpty()) {
            return emptyList()
        }
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(
                    UPDATE_TIME.`in`(
                        dslContext.select(DSL.max(UPDATE_TIME))
                            .from(this)
                            .groupBy(GIT_PROJECT_ID)
                            .having(GIT_PROJECT_ID.`in`(gitProjectIds))
                    )
                ).fetch()
        }
    }

    fun isBuildExist(dslContext: DSLContext, buildId: String): Boolean {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectCount().from(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun getBuildCountByPipelineId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String
    ): Long {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectCount().from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getBuildIdAndEventIdByPipelineId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        handlePageSize: Int
    ): Result<out Record>? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val conditions = mutableListOf<Condition>()
            conditions.add(GIT_PROJECT_ID.eq(gitProjectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            val baseStep = dslContext.select(BUILD_ID, EVENT_ID)
                .from(this)
                .where(conditions)
                .orderBy(ID.asc())
            return baseStep.limit(handlePageSize).fetch()
        }
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.deleteFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun getPipelinesLastBuild(
        dslContext: DSLContext,
        gitProjectId: Long,
        ids: List<String>
    ): List<TGitRequestEventBuildRecord>? {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(ID.`in`(ids))
                .fetch()
        }
    }

    fun getLastEventBuildIds(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): List<String> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val records = dslContext.select(DSL.max(ID))
                .from(this)
                .groupBy(PIPELINE_ID).having(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
            return if (records.isEmpty()) {
                emptyList()
            } else {
                records.map { result -> result.getValue(0).toString() }
            }
        }
    }

    fun getLatestPipelineByDuration(
        dslContext: DSLContext,
        startTime: Long,
        endTime: Long
    ): Map<String, String> {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            return dslContext
                .select(ORIGIN_YAML)
                .distinctOn(this.PIPELINE_ID)
                .from(this)
                .where(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
                .and(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
                .fetch().map {
                    it.getValue(PIPELINE_ID) to it.getValue(ORIGIN_YAML)
                }.toMap()
        }
    }

    // 获取指定日期的日活跃项目数
    fun getBuildActiveProjectCount(
        dslContext: DSLContext,
        startTime: Long,
        endTime: Long
    ): Int {
        with(TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD) {
            val dsl = dslContext.select(DSL.countDistinct(GIT_PROJECT_ID)).from(this)
                .where(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
                .and(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
                .and(BUILD_ID.isNotNull)
                .and(PARSED_YAML.like("%v2.0%"))
            return dsl.fetchOne(0, Int::class.java)!!
        }
    }

    // 获取指定日期返回内的repo-hook监听库活跃
    fun getBuildRepoHookActiveProjectCount(
        dslContext: DSLContext,
        startTime: Long,
        endTime: Long
    ): Int {
        val repoProjectTable = TGitPipelineRepoResource.T_GIT_PIPELINE_REPO_RESOURCE
        val eventBuildTable = TGitRequestEventBuild.T_GIT_REQUEST_EVENT_BUILD
        val eventTable = TGitRequestEvent.T_GIT_REQUEST_EVENT
        val targetProjectResult = dslContext.selectDistinct(repoProjectTable.TARGET_GIT_PROJECT_ID)
            .from(repoProjectTable)

        val eventResult = dslContext
            .select(targetProjectResult.field("TARGET_GIT_PROJECT_ID", String::class.java), eventBuildTable.EVENT_ID)
            .from(targetProjectResult)
            .leftJoin(eventBuildTable)
            .on(eventBuildTable.GIT_PROJECT_ID.eq(targetProjectResult.field("TARGET_GIT_PROJECT_ID", Long::class.java)))

        val countResult = dslContext.select(DSL.countDistinct(eventTable.GIT_PROJECT_ID))
            .from(eventResult)
            .leftJoin(eventTable).on(eventTable.ID.eq(eventResult.field("EVENT_ID", Long::class.java)))
            .where(eventTable.GIT_PROJECT_ID.ne(eventResult.field("TARGET_GIT_PROJECT_ID", Long::class.java)))
            .and(eventTable.CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
            .and(eventTable.CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))

        return countResult.fetchOne(0, Int::class.java)!!
    }
}
