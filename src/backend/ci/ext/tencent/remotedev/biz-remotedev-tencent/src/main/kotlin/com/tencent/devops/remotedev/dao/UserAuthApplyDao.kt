package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TUserAuthApply
import com.tencent.devops.model.remotedev.tables.records.TUserAuthApplyRecord
import com.tencent.devops.remotedev.pojo.UserAuthInfo
import com.tencent.devops.remotedev.pojo.UserAuthRecordStatus
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserAuthApplyDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        status: UserAuthRecordStatus,
        info: UserAuthInfo
    ): Long {
        return with(TUserAuthApply.T_USER_AUTH_APPLY) {
            dslContext.insertInto(this, PROJECT_ID, USER_ID, STATUS, AUTH_INFO)
                .values(projectId, userId, status.value, JSON.json(JsonUtil.toJson(info, false)))
                .returning(ID).fetchOne()!!.id
        }
    }

    fun updateTicketId(
        dslContext: DSLContext,
        id: Long,
        ticketId: String
    ) {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            dslContext.update(this).set(TICKET_ID, ticketId).set(UPDATE_TIME, LocalDateTime.now()).where(ID.eq(id))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: UserAuthRecordStatus
    ) {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            dslContext.update(this).set(STATUS, status.value).set(UPDATE_TIME, LocalDateTime.now()).where(ID.eq(id))
                .execute()
        }
    }

    fun fetchRunning(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): TUserAuthApplyRecord? {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(USER_ID.eq(userId))
                .and(STATUS.eq(UserAuthRecordStatus.RUNNING.value)).fetchAny()
        }
    }

    fun fetchById(
        dslContext: DSLContext,
        id: Long
    ): TUserAuthApplyRecord? {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchAny()
        }
    }
}