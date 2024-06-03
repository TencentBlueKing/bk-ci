package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthManagerApproval
import com.tencent.devops.model.auth.tables.records.TAuthManagerApprovalRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthManagerApprovalDao {
    fun getApprovalById(
        dslContext: DSLContext,
        approvalId: Int
    ): TAuthManagerApprovalRecord? {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.selectFrom(this)
                .where(ID.eq(approvalId)).fetchOne()
        }
    }

    fun get(
        dslContext: DSLContext,
        managerId: Int,
        userId: String
    ): TAuthManagerApprovalRecord? {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.selectFrom(this)
                .where(MANAGER_ID.eq(managerId).and(USER_ID.eq(userId))).orderBy(CREATE_TIME.desc())
                .limit(1).fetchOne()
        }
    }

    fun updateApprovalStatus(
        dslContext: DSLContext,
        approvalId: Int,
        status: Int
    ): Int {
        return with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            dslContext.update(this)
                .set(STATUS, status)
                .where(ID.eq(approvalId))
                .execute()
        }
    }

    fun createApproval(
        dslContext: DSLContext,
        userId: String,
        managerId: Int,
        expireTime: LocalDateTime,
        status: Int
    ): Int {
        val now = LocalDateTime.now()
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.insertInto(
                this,
                USER_ID,
                MANAGER_ID,
                EXPIRED_TIME,
                START_TIME,
                END_TIME,
                STATUS,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                userId,
                managerId,
                expireTime,
                now,
                now.plusDays(EXPIRATION_TIME_OF_APPROVAL),
                status,
                now,
                now
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun deleteByapprovalId(
        dslContext: DSLContext,
        approvalId: Int
    ): Int {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.deleteFrom(this).where(ID.eq(approvalId)).execute()
        }
    }

    companion object {
        const val EXPIRATION_TIME_OF_APPROVAL = 3L
    }
}
