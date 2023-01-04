/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ticket.service

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.service.CredentialPermissionService.Companion.CredentialResourceType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class CredentialPermissionServiceImpl @Autowired constructor(
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val ticketAuthServiceCode: TicketAuthServiceCode,
    private val managerService: ManagerService,
    private val credentialDao: CredentialDao,
    private val dslContext: DSLContext
) : CredentialPermissionService {

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validatePermission(userId, projectId, authPermission)) {
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
        if (!validatePermission(userId, projectId, resourceCode, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            permission = authPermission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            authPermission = authPermission,
            resourceType = CredentialResourceType
        )
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            resourceCode = resourceCode,
            permission = authPermission
        )

        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            authPermission = authPermission,
            resourceType = CredentialResourceType
        )
    }

    override fun filterCredential(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val idList = mutableListOf<String>()
        if (managerService.isManagerPermission(
                userId = userId,
                projectId = projectId,
                authPermission = authPermission,
                resourceType = CredentialResourceType
            )) {
            val count = credentialDao.countByProject(dslContext, projectId)
            val credentialRecords = credentialDao.listByProject(dslContext, projectId, 0, count.toInt())
            credentialRecords.map { idList.add(it.credentialId) }
            return idList
        }

        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            permission = authPermission
        ) { emptyList() }
    }

    override fun filterCredentials(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val iamPermissionIds = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            permissions = authPermissions
        ) { emptyList() }

        val managerIds = mutableListOf<String>()
        val count = credentialDao.countByProject(dslContext, projectId)
        val credentialRecords = credentialDao.listByProject(dslContext, projectId, 0, count.toInt())
        credentialRecords.map { managerIds.add(it.credentialId) }

        val managerPermissionMap = mutableMapOf<AuthPermission, List<String>>()
        var isManager = false
        iamPermissionIds.keys.forEach {
            if (managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    resourceType = CredentialResourceType,
                    authPermission = it
                )) {
                if (iamPermissionIds[it] == null) {
                    managerPermissionMap[it] = managerIds
                } else {
                    val collectionSet = mutableSetOf<String>()
                    collectionSet.addAll(managerIds.toSet())
                    collectionSet.addAll(iamPermissionIds[it]!!.toSet())
                    managerPermissionMap[it] = collectionSet.toList()
                }

                isManager = true
            } else {
                managerPermissionMap[it] = iamPermissionIds[it] ?: emptyList()
            }
        }

        if (isManager) {
            return managerPermissionMap
        }
        return iamPermissionIds
    }

    override fun createResource(
        userId: String,
        projectId: String,
        credentialId: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        authResourceApi.createGrantResource(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            resourceCode = credentialId,
            resourceName = credentialId,
            authGroupList = authGroupList
        )
    }

    override fun modifyResource(userId: String, projectId: String, credentialId: String) = Unit

    override fun deleteResource(projectId: String, credentialId: String) {
        authResourceApi.deleteResource(
            serviceCode = ticketAuthServiceCode,
            resourceType = CredentialResourceType,
            projectCode = projectId,
            resourceCode = credentialId
        )
    }
}
