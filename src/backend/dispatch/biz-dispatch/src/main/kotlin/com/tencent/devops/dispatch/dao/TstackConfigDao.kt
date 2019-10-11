package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchTstackConfig
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackConfigRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TstackConfigDao {
    fun getConfig(dslContext: DSLContext, projectId: String): TDispatchTstackConfigRecord? {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun getGreyWebConsoleProjects(dslContext: DSLContext): List<TDispatchTstackConfigRecord> {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            return dslContext.selectFrom(this)
                    .where(TSTACK_ENABLE.eq(true))
                    .fetch()
        }
    }

    fun saveConfig(
        dslContext: DSLContext,
        projectId: String,
        tstackEnabled: Boolean
    ) {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            dslContext.transaction { configuration ->
                val context = org.jooq.impl.DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .fetchOne()
                val now = java.time.LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this, PROJECT_ID, TSTACK_ENABLE)
                            .values(projectId, tstackEnabled)
                            .execute()
                } else {
                    context.update(this)
                            .set(TSTACK_ENABLE, tstackEnabled)
                            .where(PROJECT_ID.eq(projectId))
                            .execute()
                }
            }
        }
    }
}