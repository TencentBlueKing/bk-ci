package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.service.AuthProjectUserMetricsService
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.auth.service.iam.PermissionPostProcessor
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RbacPermissionPostProcessor(
    val bkInternalPermissionService: BkInternalPermissionService,
    val authProjectUserMetricsService: AuthProjectUserMetricsService
) : PermissionPostProcessor {
    override fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        result: Boolean
    ) {
        if (result) {
            authProjectUserMetricsService.save(
                projectId = projectCode,
                userId = userId,
                operate = action
            )
        }
        val localCheckResult = bkInternalPermissionService.validateUserResourcePermission(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            action = action
        )
        if (localCheckResult != result) {
            logger.warn(
                "Verification results are inconsistent:$userId|$projectCode|$resourceType" +
                    "|$resourceCode|$action|$result|$localCheckResult"
            )
        } else {
            logger.info(
                "Verification successful！$userId|$projectCode|$resourceType" +
                    "|$resourceCode|$action|$result|$localCheckResult"
            )
        }
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>,
        result: Map<String, Boolean>
    ) {
        result.forEach { (action, verify) ->
            validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                result = verify
            )
        }
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String,
        result: List<String>
    ) {
        val localResult = bkInternalPermissionService.getUserResourceByAction(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            action = action
        )
        if (result.toSet() != localResult.toSet()) {
            logger.warn(
                "get user resource by action results are inconsistent:$userId|" +
                    "$projectCode|$resourceType|$action|$result|$localResult"
            )
        } else {
            logger.warn(
                "get user resource by action results consistency!:" +
                    "$userId|$projectCode|$resourceType|$action"
            )
        }
    }

    override fun getUserProjectsByAction(
        userId: String,
        action: String,
        result: List<String>
    ) {
        TODO("Not yet implemented")
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>,
        result: Map<AuthPermission, List<String>>
    ) {
        val localResult = bkInternalPermissionService.filterUserResourcesByActions(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCodes = resourceCodes,
            actions = actions
        )
        result.forEach { (permission, resourceCodes) ->
            if (localResult[permission]?.toSet() != resourceCodes.toSet()) {
                logger.warn(
                    "filter user resources by actions results are inconsistent:$userId|" +
                        "$projectCode|$resourceType|$actions|$resourceCodes|${localResult[permission]}"
                )
            } else {
                logger.warn(
                    "filter user resources by actions results are consistency:$userId|" +
                        "$projectCode|$resourceType|$actions|$resourceCodes|${localResult[permission]}"
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionPostProcessor::class.java)
    }
}
