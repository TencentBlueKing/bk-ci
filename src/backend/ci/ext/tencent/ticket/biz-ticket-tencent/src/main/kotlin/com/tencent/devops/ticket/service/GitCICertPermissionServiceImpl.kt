package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCICertPermissionServiceImpl @Autowired constructor(
    val client: Client
): CertPermissionService {
    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = validatePermission(userId, projectId, authPermission)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = validatePermission(userId, projectId, authPermission)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectId)
        logger.info("GitCICredentialPermission user:$userId projectId: $projectId gitProject: $gitProjectId")
        return client.get(ServiceGitCiResource::class).checkUserGitAuth(userId, gitProjectId).data ?: false
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        return validatePermission(userId, projectId, authPermission)
    }

    override fun filterCert(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): List<String> {
        return emptyList()
    }

    override fun filterCerts(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    override fun createResource(userId: String, projectId: String, certId: String) {
        return
    }

    override fun deleteResource(projectId: String, certId: String) {
        return
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCICredentialPermissionServiceImpl::class.java)
    }
}
