package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.ManagerOrganizationDao
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.pojo.enum.ApprovalType
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthManagerApprovalService @Autowired constructor(
    val dslContext: DSLContext
) {
    fun userRenewalAuth(
        approvalId: Int,
        approvalType: ApprovalType
    ): Boolean {
        return true
    }

    fun managerApproval(
        approvalId: Int,
        approvalType: ApprovalType
    ): Boolean {
        return true
    }
}
