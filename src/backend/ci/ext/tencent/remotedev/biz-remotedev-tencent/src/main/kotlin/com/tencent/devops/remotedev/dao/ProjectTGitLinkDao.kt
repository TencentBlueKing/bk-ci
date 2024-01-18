package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectTgitLink
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectTGitLinkDao {

    fun add(
        dslContext: DSLContext,
        projectId: String,
        url: String
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                URL
            ).values(
                projectId,
                url
            ).onDuplicateKeyIgnore()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        urls: Set<String>
    ) {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            dslContext.batch(
                urls.map { url ->
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        URL
                    ).values(
                        projectId,
                        url
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

    fun fetchUrl(
        dslContext: DSLContext,
        projectId: String
    ): Set<String> {
        with(TProjectTgitLink.T_PROJECT_TGIT_LINK) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetch().map { it.url }.toSet()
        }
    }
}
