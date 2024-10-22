package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TProjectTgitIdLink
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitIdLinkRecord
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitCredType
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectTGitLinkDao {

    // 注：OAUTH_USER 这个字段的改做和操作者一种用发，运来记录当前绑定是谁操作的
    fun add(
        dslContext: DSLContext,
        projectId: String,
        tgitId: Long,
        status: TGitRepoStatus,
        oauthUser: String,
        gitType: String,
        url: String,
        cred: String,
        credType: TGitCredType
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                TGIT_ID,
                STATUS,
                OAUTH_USER,
                GIT_TYPE,
                URL,
                CRED,
                CRED_TYPE
            ).values(
                projectId,
                tgitId,
                status.name,
                oauthUser,
                gitType,
                url,
                cred,
                credType.name
            ).onDuplicateKeyUpdate()
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(OAUTH_USER, oauthUser)
                .set(CRED, cred)
                .set(CRED_TYPE, credType.name)
                .set(URL, url)
                .execute()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        data: List<TGitRepoDaoData>
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.batch(
                data.map {
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        TGIT_ID,
                        STATUS,
                        OAUTH_USER,
                        GIT_TYPE,
                        URL,
                        CRED,
                        CRED_TYPE
                    ).values(
                        projectId,
                        it.tgitId,
                        it.status.name,
                        it.oauthUser,
                        it.gitType,
                        it.url,
                        it.cred,
                        it.credType.name
                    ).onDuplicateKeyUpdate()
                        .set(STATUS, it.status.name)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .set(OAUTH_USER, it.oauthUser)
                        .set(CRED, it.cred)
                        .set(CRED_TYPE, it.credType.name)
                        .set(URL, it.url)
                }
            ).execute()
        }
    }

    fun deleteUrl(
        dslContext: DSLContext,
        projectId: String,
        tgitId: Long
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(TGIT_ID.eq(tgitId)).execute()
        }
    }

    fun deleteIds(
        dslContext: DSLContext,
        projectId: String,
        tgitIds: Set<Long>
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(TGIT_ID.`in`(tgitIds)).execute()
        }
    }

    fun fetch(
        dslContext: DSLContext,
        projectId: String,
        tgitIds: Set<Long>?
    ): List<TProjectTgitIdLinkRecord> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            val dsl = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
            if (tgitIds != null) {
                dsl.and(TGIT_ID.`in`(tgitIds))
            }
            return dsl.fetch()
        }
    }

    fun fetchAllProject(
        dslContext: DSLContext
    ): Set<String> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            return dslContext.selectDistinct(PROJECT_ID).from(this).skipCheck().fetch().map { it[PROJECT_ID] }.toSet()
        }
    }

    fun fetchAll(
        dslContext: DSLContext,
        sqlLimit: SQLLimit?
    ): List<TProjectTgitIdLinkRecord> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            val dsl = dslContext.selectFrom(this)
            if (sqlLimit != null) {
                dsl.offset(sqlLimit.offset).limit(sqlLimit.limit)
            }
            return dsl.skipCheck().fetch()
        }
    }

    fun updateUrl(
        dslContext: DSLContext,
        projectId: String,
        tgitId: Long,
        url: String
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.update(this).set(URL, url).where(PROJECT_ID.eq(projectId)).and(TGIT_ID.eq(tgitId)).execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        tgitId: Long,
        status: TGitRepoStatus
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(TGIT_ID.eq(tgitId))
                .execute()
        }
    }

    fun batchUpdateStatus(
        dslContext: DSLContext,
        projectId: String,
        tgitIds: Set<Long>,
        status: TGitRepoStatus
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.batch(
                tgitIds.map {
                    dslContext.update(this)
                        .set(STATUS, status.name)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .where(PROJECT_ID.eq(projectId))
                        .and(TGIT_ID.eq(it))
                }
            ).execute()
        }
    }

    fun fetchByTGitId(
        dslContext: DSLContext,
        tgitId: Long,
        notProjectId: String?
    ): List<TProjectTgitIdLinkRecord> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            val dsl = dslContext.selectFrom(this).where(TGIT_ID.eq(tgitId))
            if (!notProjectId.isNullOrBlank()) {
                dsl.and(PROJECT_ID.ne(notProjectId))
            }
            return dsl.fetch()
        }
    }

    fun batchUpdateCred(
        dslContext: DSLContext,
        projectId: String,
        tgitIds: Set<Long>,
        status: TGitRepoStatus,
        oauthUser: String,
        cred: String,
        credType: TGitCredType
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.batch(
                tgitIds.map {
                    dslContext.update(this)
                        .set(STATUS, status.name)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .set(OAUTH_USER, oauthUser)
                        .set(CRED, cred)
                        .set(CRED_TYPE, credType.name)
                        .where(PROJECT_ID.eq(projectId))
                        .and(TGIT_ID.eq(it))
                }
            ).execute()
        }
    }
}
