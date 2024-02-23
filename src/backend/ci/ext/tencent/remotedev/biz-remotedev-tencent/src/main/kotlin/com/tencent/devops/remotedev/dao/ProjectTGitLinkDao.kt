package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectTgitLink
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
        url: String,
        status: TGitRepoStatus,
        oauthUser: String
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                URL,
                STATUS,
                OAUTH_USER
            ).values(
                projectId,
                url,
                status.name,
                oauthUser
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
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.batch(
                urls.map {
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        URL,
                        STATUS,
                        OAUTH_USER
                    ).values(
                        projectId,
                        it.url,
                        it.status.name,
                        it.oauthUser
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
        url: String
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(URL.eq(url)).execute()
        }
    }

    fun fetch(
        dslContext: DSLContext,
        projectId: String
    ): List<TProjectTgitLinkRecord> {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetch()
        }
    }
}
