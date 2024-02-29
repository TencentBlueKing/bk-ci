package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TProjectTgitIdLink
import com.tencent.devops.model.remotedev.tables.TProjectTgitLink
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitIdLinkRecord
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitLinkRecord
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectTGitLinkDao {

    fun add(
        dslContext: DSLContext,
        projectId: String,
        tgitId: Long,
        status: TGitRepoStatus,
        oauthUser: String,
        gitType: String
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                TGIT_ID,
                STATUS,
                OAUTH_USER,
                GIT_TYPE
            ).values(
                projectId,
                tgitId,
                status.name,
                oauthUser,
                gitType
            ).onDuplicateKeyUpdate()
                .set(STATUS, status.name)
                .set(OAUTH_USER, oauthUser)
                .execute()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        urls: List<TGitRepoDaoData>
    ) {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            dslContext.batch(
                urls.map {
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        TGIT_ID,
                        STATUS,
                        OAUTH_USER,
                        GIT_TYPE
                    ).values(
                        projectId,
                        it.tgitId,
                        it.status.name,
                        it.oauthUser,
                        it.gitType
                    ).onDuplicateKeyUpdate()
                        .set(STATUS, it.status.name)
                        .set(OAUTH_USER, it.oauthUser)
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

    fun fetch(
        dslContext: DSLContext,
        projectId: String
    ): List<TProjectTgitIdLinkRecord> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetch()
        }
    }

    fun fetchAll(
        dslContext: DSLContext
    ): List<TProjectTgitIdLinkRecord> {
        with(TProjectTgitIdLink.T_PROJECT_TGIT_ID_LINK) {
            return dslContext.selectFrom(this).skipCheck().fetch()
        }
    }

    fun fetchOld(
        dslContext: DSLContext,
        projectId: String?
    ): List<TProjectTgitLinkRecord> {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            val dsl = dslContext.selectFrom(this)
            if (!projectId.isNullOrBlank()) {
                dsl.where(PROJECT_ID.eq(projectId))
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
}
