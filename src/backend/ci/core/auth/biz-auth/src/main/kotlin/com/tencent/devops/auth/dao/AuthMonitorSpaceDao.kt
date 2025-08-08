package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthMonitorSpace
import com.tencent.devops.model.auth.tables.records.TAuthMonitorSpaceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

    fun list(
        dslContext: DSLContext,
        projectCodes: List<String>
    ): Map<String, Long> {
        return with(TAuthMonitorSpace.T_AUTH_MONITOR_SPACE) {
            dslContext.select(PROJECT_CODE, SPACE_BIZ_ID)
                .from(this)
                .where(PROJECT_CODE.`in`(projectCodes))
                .fetch()
                .map { Pair(it.value1(), it.value2()) }.toMap()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectCode: String,
        spaceUid: String,
        updateUser: String
    ) {
        val now = LocalDateTime.now()
        with(TAuthMonitorSpace.T_AUTH_MONITOR_SPACE) {
            dslContext.update(this)
                .set(SPACE_UID, spaceUid)
                .set(UPDATE_USER, updateUser)
                .set(UPDATE_TIME, now)
                .where(PROJECT_CODE.eq(projectCode))
                .execute()
        }
    }
}
