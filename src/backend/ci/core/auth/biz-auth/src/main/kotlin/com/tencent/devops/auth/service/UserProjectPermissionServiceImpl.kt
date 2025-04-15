package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class UserProjectPermissionServiceImpl(
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext
) : UserProjectPermissionService {
    override fun checkMember(projectCode: String, userId: String): Boolean {
        return authResourceGroupMemberDao.isProjectMember(dslContext, projectCode, userId)
    }
}