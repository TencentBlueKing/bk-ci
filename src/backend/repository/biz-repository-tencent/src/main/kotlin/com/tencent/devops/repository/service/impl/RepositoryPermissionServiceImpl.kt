package com.tencent.devops.repository.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.code.BSCodeAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPermissionServiceImpl @Autowired constructor(
        private val authResourceApi: AuthResourceApi,
        private val authPermissionApi: AuthPermissionApi,
        private val codeAuthServiceCode: BSCodeAuthServiceCode,
        private val bkAuthTokenApi: BSAuthTokenApi,
        private val bkAuthProperties: BkAuthProperties,
        private val objectMapper: ObjectMapper
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserResourcesByPermissions(user: String, projectCode: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserResourceByPermission(user: String, projectCode: String, permission: AuthPermission): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, permission: AuthPermission): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, resourceCode: String, permission: AuthPermission): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun modifyResource(projectCode: String, resourceCode: String, resourceName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}