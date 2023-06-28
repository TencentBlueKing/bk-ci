package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthMonitorSpace
import com.tencent.devops.model.auth.tables.records.TAuthMonitorSpaceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthMonitorSpaceDao {
    fun create(
        dslContext: DSLContext,
        projectCode: String,
        spaceBizId: Long,
        spaceUid: String,
        creator: String
    ) {
        with(TAuthMonitorSpace.T_AUTH_MONITOR_SPACE) {
            dslContext.insertInto(
                this,
                PROJECT_CODE,
                SPACE_BIZ_ID,
                SPACE_UID,
                CREATOR
            ).values(
                projectCode,
                spaceBizId,
                spaceUid,
                creator
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String
    ): TAuthMonitorSpaceRecord? {
        return with(TAuthMonitorSpace.T_AUTH_MONITOR_SPACE) {
            dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode)).fetchAny()
        }
    }

    fun update() {

    }
}
