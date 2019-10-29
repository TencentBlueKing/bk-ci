package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.code.BSCodeAuthServiceCode
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPermissionServiceImpl @Autowired constructor(
        private val authResourceApi: AuthResourceApi,
        private val repoAuthServiceCode:BSRepoAuthServiceCode,
        private val authPermissionApi: AuthPermissionApi,
        private val codeAuthServiceCode: BSCodeAuthServiceCode
) : RepositoryPermissionService {

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?, message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filterRepositories(userId: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.createResource(
                userId,
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString(),
                repositoryName
        )
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.modifyResource(
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString(),
                repositoryName
        )
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        authResourceApi.deleteResource(
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString()
        )
    }

    override fun getUserResourcesByPermissions(user: String, projectCode: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
                user = user,
                serviceCode = codeAuthServiceCode,
                resourceType = AuthResourceType.CODE_REPERTORY,
                projectCode = projectCode,
                permissions = permissions,
                supplier = null
        )
        return permissionResourcesMap.mapValues {
            it.value.map { it.toString() }
        }
    }

    override fun getUserResourceByPermission(user: String, projectCode: String, permission: AuthPermission): List<String> {
        val resourceCodeList = authPermissionApi.getUserResourceByPermission(
                user,
                repoAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                permission,
                null
        )
        return resourceCodeList.map { it }
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
                user,
                repoAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                permission
        )
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, resourceCode: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
                user,
                repoAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                resourceCode,
                permission
        )
    }

    override fun modifyResource(projectCode: String, resourceCode: String, resourceName: String) {
        authResourceApi.modifyResource(
                repoAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                resourceCode,
                resourceName
        )
    }
}