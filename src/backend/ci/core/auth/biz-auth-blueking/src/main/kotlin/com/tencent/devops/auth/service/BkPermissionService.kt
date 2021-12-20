package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.ActionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

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
        val useAction = buildAction(action, resourceType ?: AuthResourceType.PIPELINE_DEFAULT.value)
        return super.validateUserResourcePermission(userId, useAction, projectCode, resourceType)
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
        val useAction = buildAction(action, resourceType)

        return super.validateUserResourcePermissionByRelation(
            userId = userId,
            action = useAction,
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
        val useAction = buildAction(action, resourceType)
        return super.getUserResourceByAction(userId, useAction, projectCode, resourceType)
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

        val userActions = mutableListOf<String>()
        actions.forEach {
            userActions.add(buildAction(it, resourceType))
        }

        return super.getUserResourcesByActions(userId, userActions, projectCode, resourceType)
    }

    private fun isAdmin(userId: String): Boolean {
        if (userId == "admin") {
            return true
        }
        return false
    }

    private fun buildAction(action: String, resourceType: String): String {
        // action需要兼容repo只传AuthPermission的情况,需要组装为V3的action
        return if (!action.contains("_")) {
            ActionUtils.buildAction(
                authResourceType = AuthResourceType.get(resourceType),
                permission = AuthPermission.get(action)
            )
        } else {
            action
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(BkPermissionService::class.java)
    }
}
