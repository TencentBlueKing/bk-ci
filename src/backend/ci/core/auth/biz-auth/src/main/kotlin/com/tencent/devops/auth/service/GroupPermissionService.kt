package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthGroupPermissionDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GroupPermissionService @Autowired constructor(
    val dslContext: DSLContext,
    val groupPermissionDao: AuthGroupPermissionDao
) {

    fun getPermissionByGroupCode(groupCode: String): List<String>? {
        val permissionRecord = groupPermissionDao.getByGroupCode(dslContext, groupCode)
        var permissionList = mutableListOf<String>()
        if(permissionRecord != null) {
            permissionList = permissionRecord.map { it.authAction }
        }
        return permissionList
    }
}