package com.tencent.devops.log.dao.v2

import com.tencent.devops.model.log.tables.TLogStatusV2
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class LogStatusDaoV2 {

    fun finish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        executeCount: Int?,
        finish: Boolean
    ) {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            dslContext.insertInto(this,
                BUILD_ID, TAG, EXECUTE_COUNT, FINISHED)
                .values(buildId, tag ?: "", executeCount ?: 1, finish)
                .onDuplicateKeyUpdate()
                .set(FINISHED, finish)
                .execute()
        }
    }

    fun isFinish(
        dslContext: DSLContext,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): Boolean {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(TAG.eq(tag ?: ""))
                .and(EXECUTE_COUNT.eq(executeCount ?: 1))
                .fetchOne()?.finished ?: false
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildIds: Set<String>
    ): Int {
        with(TLogStatusV2.T_LOG_STATUS_V2) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }
}