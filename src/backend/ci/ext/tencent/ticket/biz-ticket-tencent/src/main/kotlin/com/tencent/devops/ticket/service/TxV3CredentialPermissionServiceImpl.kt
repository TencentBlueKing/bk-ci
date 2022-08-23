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

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CredentialDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxV3CredentialPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val credentialDao: CredentialDao,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService,
    val authResourceApi: AuthResourceApiStr
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

    override fun filterCredential(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val credentialList = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = buildCredentialAction(authPermission),
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        ).data ?: emptyList()

        logger.info("filterCredential user[$userId] project[$projectId] auth[$authPermission] list[$credentialList]")
        return if (credentialList.contains("*")) {
            getAllCredentialsByProject(projectId)
        } else {
            credentialList
        }
    }

    override fun filterCredentials(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val actions = TActionUtils.buildActionList(authPermissions, AuthResourceType.TICKET_CREDENTIAL)

        val credentialAuthResult = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = actions,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        ).data ?: emptyMap()
        val credentialMap = mutableMapOf<AuthPermission, List<String>>()

        val projectAllCertIds: List<String> by lazy { getAllCredentialsByProject(projectId) }

        credentialAuthResult.forEach { (key, value) ->
            val ids =
                if (value.contains("*")) {
                    logger.info("filterCredential user[$userId] project[$projectId] auth[$key] list[$value]")
                    projectAllCertIds
                } else {
                    value
                }
            credentialMap[key] = ids
            if (key == AuthPermission.VIEW) {
                credentialMap[AuthPermission.LIST] = ids
            }
        }
        return credentialMap
    }

    override fun createResource(
        userId: String,
        projectId: String,
        credentialId: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = null,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
            projectCode = projectId,
            resourceCode = credentialId,
            resourceName = credentialId
        )
        return
    }

    override fun deleteResource(projectId: String, credentialId: String) {
        return
    }

    private fun getAllCredentialsByProject(projectId: String): List<String> {
        val idList = mutableListOf<String>()
        val count = credentialDao.countByProject(dslContext, projectId)
        credentialDao.listByProject(dslContext, projectId, 0, count.toInt())
            .filter { idList.add(it.credentialId) }
        return idList
    }

    private fun buildCredentialAction(permission: AuthPermission): String {
        return TActionUtils.buildAction(permission, AuthResourceType.TICKET_CREDENTIAL)
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxV3CredentialPermissionServiceImpl::class.java)
    }
}
