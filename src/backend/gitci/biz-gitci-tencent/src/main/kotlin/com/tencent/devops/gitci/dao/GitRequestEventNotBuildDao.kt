package com.tencent.devops.gitci.dao

import com.tencent.devops.model.gitci.tables.TGitRequestEventNotBuild
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitRequestEventNotBuildDao {

    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String?,
        normalizedYaml: String?,
        reason: String?,
        gitprojectId: Long
    ): Long {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val record = dslContext.insertInto(this,
                    EVENT_ID,
                    ORIGIN_YAML,
                    NORMALIZED_YAML,
                    REASON,
                    GIT_PROJECT_ID,
                    CREATE_TIME
                ).values(
                    eventId,
                    originYaml,
                    normalizedYaml,
                    reason,
                    gitprojectId,
                    LocalDateTime.now()
            ).returning(ID)
            .fetchOne()
            return record.id
        }
    }
}
