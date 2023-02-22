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
 *
 */

package com.tencent.devops.ticket.service.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.service.CredentialPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacCredentialPermissionService constructor(
    val client: Client,
    val credentialDao: CredentialDao,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService
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

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            action = buildCredentialAction(authPermission),
            relationResourceType = null
        ).data ?: false
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = resourceCode,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
            action = buildCredentialAction(authPermission),
            relationResourceType = null
        ).data ?: false
    }

    override fun filterCredential(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): List<String> {
        return client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = buildCredentialAction(authPermission),
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        ).data ?: emptyList()
    }

    override fun filterCredentials(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = RbacAuthUtils.buildActionList(authPermissions, AuthResourceType.TICKET_CREDENTIAL),
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        ).data ?: emptyMap()
    }

    override fun createResource(
        userId: String,
        projectId: String,
        credentialId: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
            resourceCode = credentialId,
            resourceName = credentialId
        )
    }

    override fun deleteResource(
        projectId: String,
        credentialId: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
            resourceCode = credentialId
        )
    }

    private fun buildCredentialAction(permission: AuthPermission): String {
        return RbacAuthUtils.buildAction(permission, AuthResourceType.TICKET_CREDENTIAL)
    }

    companion object {
        val logger = LoggerFactory.getLogger(RbacCredentialPermissionService::class.java)
    }
}
