package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectTgitLink
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitLinkRecord
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectTGitLinkDao {

    fun add(
        dslContext: DSLContext,
        projectId: String,
        url: String,
        status: TGitRepoStatus
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                URL,
                STATUS
            ).values(
                projectId,
                url,
                status.name
            ).onDuplicateKeyUpdate()
                .set(STATUS, status.name)
                .execute()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        urls: List<TGitRepoData>
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.batch(
                urls.map { url ->
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        URL,
                        STATUS
                    ).values(
                        projectId,
                        url.url,
                        url.status.name
                    ).onDuplicateKeyIgnore()
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
