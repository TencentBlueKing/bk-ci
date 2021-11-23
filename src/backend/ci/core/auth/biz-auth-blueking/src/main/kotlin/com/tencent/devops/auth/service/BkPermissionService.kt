package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionService
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BkPermissionService @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration,
    private val iamCacheService: IamCacheService
) : AbsPermissionService(authHelper, policyService, iamConfiguration, iamCacheService) {
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        if (isAdmin(userId)) return true
        return super.validateUserActionPermission(userId, action)
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        if (isAdmin(userId)) return true
        return super.validateUserResourcePermission(userId, action, projectCode, resourceType)
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        if (isAdmin(userId)) return true
        return super.validateUserResourcePermissionByRelation(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType,
            relationResourceType = relationResourceType
        )
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        logger.info("getUserResourceByAction $userId $action $projectCode")
        if (isAdmin(userId)) {
            logger.info("getUserResourceByAction $userId is admin")
            return arrayListOf("*")
        }

        return super.getUserResourceByAction(userId, action, projectCode, resourceType)
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        if (isAdmin(userId)) {
            val permissionMap = mutableMapOf<AuthPermission, List<String>>()
            actions.forEach {
                permissionMap[AuthPermission.get(it)] = arrayListOf("*")
                return permissionMap
            }
        }
        return super.getUserResourcesByActions(userId, actions, projectCode, resourceType)
    }

    private fun isAdmin(userId: String): Boolean {
        if (userId == "admin") {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(BkPermissionService::class.java)
    }
}
