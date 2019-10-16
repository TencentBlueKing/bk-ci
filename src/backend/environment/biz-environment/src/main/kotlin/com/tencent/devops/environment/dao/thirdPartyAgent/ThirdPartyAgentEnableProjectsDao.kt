package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyEnableProjects
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyEnableProjectsRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentEnableProjectsDao {

    fun enable(
        dslContext: DSLContext,
        projectId: String,
        enable: Boolean
    ) {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .fetchOne()
                val now = LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this,
                            PROJECT_ID,
                            ENALBE,
                            CREATED_TIME,
                            UPDATED_TIME)
                            .values(projectId,
                                    ByteUtils.bool2Byte(enable),
                                    now,
                                    now)
                            .execute()
                } else {
                    context.update(this)
                            .set(ENALBE, ByteUtils.bool2Byte(enable))
                            .set(UPDATED_TIME, now)
                            .where(PROJECT_ID.eq(projectId))
                            .execute()
                }
            }
        }
    }

    fun isEnable(dslContext: DSLContext, projectId: String): Boolean {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            val record = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne() ?: return false
            return ByteUtils.byte2Bool(record.enalbe)
        }
    }

    fun list(dslContext: DSLContext): Result<TEnvironmentThirdpartyEnableProjectsRecord> {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }
}