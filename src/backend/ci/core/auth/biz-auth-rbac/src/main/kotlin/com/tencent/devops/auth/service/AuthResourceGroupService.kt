package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList")
class AuthResourceGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val authResourceDao: AuthResourceDao,
    private val authResourceGroupDao: AuthResourceGroupDao
) {
    fun create(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        groupCode: String,
        groupName: String,
        defaultGroup: Boolean,
        relationId: String
    ) {
        val iamResourceCode = authResourceDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )!!.iamResourceCode

        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            iamResourceCode = iamResourceCode,
            groupCode = groupCode,
            groupName = groupName,
            defaultGroup = defaultGroup,
            relationId = relationId
        )
    }
}
