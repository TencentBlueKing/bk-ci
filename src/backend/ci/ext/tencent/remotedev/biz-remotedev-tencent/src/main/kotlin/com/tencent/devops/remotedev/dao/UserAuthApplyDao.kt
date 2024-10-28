package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TUserAuthApply
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
    ) {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            dslContext.insertInto(this, PROJECT_ID, USER_ID, STATUS, AUTH_INFO)
                .values(projectId, userId, status.value, JSON.json(JsonUtil.toJson(info, false)))
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

    fun checkRunning(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): Boolean {
        with(TUserAuthApply.T_USER_AUTH_APPLY) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId)).and(USER_ID.eq(userId))
                .and(STATUS.eq(UserAuthRecordStatus.RUNNING.value)).fetchOne(0, Long::class.java)!! > 0
        }
    }
}