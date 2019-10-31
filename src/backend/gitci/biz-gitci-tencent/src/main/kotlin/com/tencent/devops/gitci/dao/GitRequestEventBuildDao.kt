package com.tencent.devops.gitci.dao

import com.tencent.devops.gitci.pojo.BranchBuilds
import com.tencent.devops.model.gitci.tables.TGitRequestEventBuild
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventBuildRecord
import org.jooq.DSLContext
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
