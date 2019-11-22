package com.tencent.devops.gitci.dao

import com.tencent.devops.model.gitci.tables.TGitCiServicesConf
import com.tencent.devops.model.gitci.tables.records.TGitCiServicesConfRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCIServicesConfDao {

    fun create(
        dslContext: DSLContext,
        imageName: String,
        imageTag: String,
        repoUrl: String,
        repoUsername: String?,
        repoPwd: String?,
        enable: Boolean,
        env: String?,
        createUser: String?,
        updateUser: String?
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            dslContext.insertInto(this,
                IMAGE_NAME,
                IMAGE_TAG,
                REPO_URL,
                REPO_USERNAME,
                REPO_PWD,
                ENABLE,
                ENV,
                CREATE_USER,
                UPDATE_USER,
                GMT_MODIFIED,
                GMT_CREATE
            ).values(
                imageName,
                imageTag,
                repoUrl,
                repoUsername,
                repoPwd,
                enable,
                env,
                createUser,
                updateUser,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        user: String,
        enable: Boolean?
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {

            val steps = dslContext.update(this)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .set(UPDATE_USER, user)

            if (enable != null) {
                steps.set(ENABLE, enable)
            }
            steps.where(ID.eq(id)).execute()
        }
    }

    fun get(dslContext: DSLContext, imageName: String, imageTag: String): TGitCiServicesConfRecord? {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            return dslContext.selectFrom(this)
                .where(IMAGE_NAME.eq(imageName))
                .and(IMAGE_TAG.eq(imageTag))
                .fetchOne()
        }
    }

    fun list(dslContext: DSLContext): Result<TGitCiServicesConfRecord> {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}