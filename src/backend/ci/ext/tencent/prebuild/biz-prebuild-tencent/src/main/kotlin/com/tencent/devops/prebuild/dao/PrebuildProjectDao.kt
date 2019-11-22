package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildProject
import com.tencent.devops.model.prebuild.tables.records.TPrebuildProjectRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PrebuildProjectDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        prebuildProjectId: String,
        projectId: String,
        owner: String,
        yaml: String,
        pipelineId: String,
        workspace: String
    ) {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            dslContext.insertInto(
                    this,
                    PREBUILD_PROJECT_ID,
                    PROJECT_ID,
                    OWNER,
                    DESC,
                    CREATE_TIME,
                    CREATOR,
                    UPDATE_TIME,
                    LAST_MODIFY_USER,
                    YAML,
                    PIPELINE_ID,
                    WORKSPACE
            ).values(
                    prebuildProjectId,
                    projectId,
                    owner,
                    "",
                    LocalDateTime.now(),
                    owner,
                    LocalDateTime.now(),
                    owner,
                    yaml,
                    pipelineId,
                    workspace
            ).onDuplicateKeyUpdate()
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_MODIFY_USER, owner)
                    .set(YAML, yaml)
                    .set(PIPELINE_ID, pipelineId)
                    .set(WORKSPACE, workspace)
                    .execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        prebuildProjectId: String,
        projectId: String,
        owner: String,
        yaml: String,
        pipelineId: String,
        workspace: String
    ) {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            dslContext.insertInto(
                this,
                PREBUILD_PROJECT_ID,
                PROJECT_ID,
                OWNER,
                DESC,
                CREATE_TIME,
                CREATOR,
                UPDATE_TIME,
                LAST_MODIFY_USER,
                YAML,
                PIPELINE_ID,
                WORKSPACE
            ).values(
                prebuildProjectId,
                projectId,
                owner,
                "",
                LocalDateTime.now(),
                owner,
                LocalDateTime.now(),
                owner,
                yaml,
                pipelineId,
                workspace
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        prebuildProjectId: String,
        workspace: String,
        userId: String,
        yaml: String,
        pipelineId: String
    ) {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .set(YAML, yaml)
                .set(PIPELINE_ID, pipelineId)
                .set(WORKSPACE, workspace)
                .where(PREBUILD_PROJECT_ID.eq(prebuildProjectId))
                .and(OWNER.eq(userId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        prebuildProjectId: String,
        userId: String
    ): TPrebuildProjectRecord? {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PREBUILD_PROJECT_ID.eq(prebuildProjectId))
                .and(OWNER.eq(userId))
                .fetchAny()
        }
    }

    fun get(
        dslContext: DSLContext,
        prebuildProjectId: String,
        userId: String,
        workspace: String
    ): TPrebuildProjectRecord? {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            return dslContext.selectFrom(this)
                    .where(PREBUILD_PROJECT_ID.eq(prebuildProjectId))
                    .and(OWNER.eq(userId))
                    .and(WORKSPACE.eq(workspace))
                    .fetchAny()
        }
    }

    fun list(dslContext: DSLContext, userId: String, projectId: String): List<TPrebuildProjectRecord> {
        with(TPrebuildProject.T_PREBUILD_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(OWNER.eq(userId))
                .fetch()
        }
    }
}
