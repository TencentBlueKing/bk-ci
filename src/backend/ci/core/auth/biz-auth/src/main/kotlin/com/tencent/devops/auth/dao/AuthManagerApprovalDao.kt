package com.tencent.devops.auth.dao

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.auth.tables.TAuthManagerApproval
import com.tencent.devops.model.auth.tables.records.TAuthManagerApprovalRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
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
        var id = 0
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                id = transactionContext.insertInto(
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
                    now.plusDays(2),
                    status,
                    now,
                    now
                ).returning(ID)
                    .fetchOne()!!.id
                val encodeIntId = HashUtil.encodeIntId(id)
                transactionContext.update(this)
                    .set(AUTH_HASH_ID, encodeIntId)
                    .where(ID.eq(id))
                    .execute()
            }
        }
        return id
    }

    fun deleteByapprovalId(
        dslContext: DSLContext,
        approvalId: Int
    ): Int {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.deleteFrom(this).where(ID.eq(approvalId)).execute()
        }
    }

    fun getAll(dslContext: DSLContext): Result<Record1<Int>> {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.select(ID).from(this).fetch()
        }
    }

    fun updateTest(dslContext: DSLContext, id: Int, hashId: String) {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            dslContext.update(this).set(AUTH_HASH_ID, hashId).where(ID.eq(id))
                .execute()
        }
    }
}
