package com.tencent.devops.auth.service.sample

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory

class SampleAuthPermissionService : PermissionService {
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        return true
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return true
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        logger.info("getUserResourceByAction $userId $action $projectCode $resourceType")
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    companion object {
        val logger = LoggerFactory.getLogger(SampleAuthPermissionService::class.java)
    }
}
