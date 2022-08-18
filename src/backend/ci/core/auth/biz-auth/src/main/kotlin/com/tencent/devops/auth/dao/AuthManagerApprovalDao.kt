package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthManagerApproval
import com.tencent.devops.model.auth.tables.records.TAuthManagerApprovalRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthManagerApprovalDao {
    fun getApprovalById(
        dslContext: DSLContext,
        approvalId: Int
    ): TAuthManagerApprovalRecord? {
        with(TAuthManagerApproval.T_AUTH_MANAGER_APPROVAL) {
            return dslContext.selectFrom(this)
                .where(ID.eq(approvalId)).orderBy(CREATE_TIME.desc()).fetchOne()
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
}
