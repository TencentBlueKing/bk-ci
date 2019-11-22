package com.tencent.devops.gitci.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.gitci.pojo.GitProjectConf
import com.tencent.devops.model.gitci.tables.TGitProjectConf
import com.tencent.devops.model.gitci.tables.records.TGitProjectConfRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
class GitProjectConfDao {

    fun create(
        dslContext: DSLContext,
        gitProjectId: Long,
        name: String,
        url: String,
        enable: Boolean
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            dslContext.insertInto(this,
                ID,
                NAME,
                URL,
                ENABLE,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                    gitProjectId,
                    name,
                    url,
                    enable,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        gitProjectId: Long,
        name: String?,
        url: String?,
        enable: Boolean?
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {

            val steps = dslContext.update(this).set(UPDATE_TIME, LocalDateTime.now())
            if (!name.isNullOrBlank()) {
                steps.set(NAME, name)
            }
            if (!url.isNullOrBlank()) {
                steps.set(URL, url)
            }
            if (enable != null) {
                steps.set(ENABLE, enable)
            }
            steps.where(ID.eq(gitProjectId)).execute()
        }
    }

    fun get(dslContext: DSLContext, gitProjectId: Long): GitProjectConf? {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val record = dslContext.selectFrom(this)
                    .where(ID.eq(gitProjectId))
                    .fetchOne()
            return if (record == null) {
                null
            } else {
                GitProjectConf(
                        record.id,
                        record.name,
                        record.url,
                        record.enable,
                        record.createTime.timestampmilli(),
                        record.updateTime.timestampmilli()
                )
            }
        }
    }

    fun delete(
        dslContext: DSLContext,
        gitProjectId: Long
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(gitProjectId))
                    .execute()
        }
    }

    fun count(
        dslContext: DSLContext,
        gitProjectId: Long?,
        name: String?,
        url: String?
    ): Int {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val conditions = mutableListOf<Condition>()
            if (gitProjectId != null) {
                conditions.add(ID.eq(gitProjectId))
            }
            if (!name.isNullOrBlank()) {
                conditions.add(
                        NAME.like(
                                "%" + URLDecoder.decode(
                                        name,
                                        "UTF-8"
                                ) + "%"
                        )
                )
            }
            if (!url.isNullOrBlank()) {
                conditions.add(
                        URL.like(
                                "%" + URLDecoder.decode(
                                        url,
                                        "UTF-8"
                                ) + "%"
                        )
                )
            }

            return dslContext.selectCount()
                    .from(this)
                    .where(conditions)
                    .fetchOne(0, Int::class.java)
        }
    }

    fun getList(
        dslContext: DSLContext,
        gitProjectId: Long?,
        name: String?,
        url: String?,
        page: Int,
        pageSize: Int
    ): Result<TGitProjectConfRecord> {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val conditions = mutableListOf<Condition>()
            if (gitProjectId != null) {
                conditions.add(ID.eq(gitProjectId))
            }
            if (!name.isNullOrBlank()) {
                conditions.add(
                        NAME.like(
                                "%" + URLDecoder.decode(
                                        name,
                                        "UTF-8"
                                ) + "%"
                        )
                )
            }
            if (!url.isNullOrBlank()) {
                conditions.add(
                        URL.like(
                                "%" + URLDecoder.decode(
                                        url,
                                        "UTF-8"
                                ) + "%"
                        )
                )
            }
            return dslContext.selectFrom(this).where(conditions)
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }
}
