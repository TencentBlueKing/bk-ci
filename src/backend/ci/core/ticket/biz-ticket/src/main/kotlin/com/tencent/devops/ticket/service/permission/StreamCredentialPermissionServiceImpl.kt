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
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.service.CredentialPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamCredentialPermissionServiceImpl @Autowired constructor(
    private val client: Client,
    private val credentialDao: CredentialDao,
    private val dslContext: DSLContext,
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

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        logger.info("StreamCredentialPermissionServiceImpl user:$userId projectId: $projectId ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenService.getSystemToken(null) ?: "",
            action = AuthPermission.ENABLE.value, // 凭证类所有检验都以操作类来校验。即便是view也走操作类权限校验
            projectCode = projectId,
            resourceCode = null
        ).data ?: false
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        return validatePermission(userId, projectId, authPermission)
    }

    override fun filterCredential(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val checkPermission = validatePermission(userId, projectId, authPermission)
        if (!checkPermission) {
            return emptyList()
        }
        return getAllCredentialsByProject(projectId)
    }

    override fun filterCredentials(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        // 凭证类所有检验都以操作类来校验。即便是view也走操作类权限校验. 若后续产品有调整,此处需要拆解为不同的permission类型
        val checkPermission = validatePermission(userId, projectId, AuthPermission.ENABLE)
        if (!checkPermission) {
            return emptyMap()
        }
        val projectAllInstances = getAllCredentialsByProject(projectId)
        val resultMap = mutableMapOf<AuthPermission, List<String>>()
        authPermissions.forEach {
            resultMap[it] = projectAllInstances
        }
        return resultMap
    }

    override fun createResource(
        userId: String,
        projectId: String,
        credentialId: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        return
    }

    private fun getAllCredentialsByProject(projectId: String): List<String> {
        val idList = mutableListOf<String>()
        val count = credentialDao.countByProject(dslContext, projectId)
        credentialDao.listByProject(dslContext, projectId, 0, count.toInt()).filter { idList.add(it.credentialId) }
        return idList
    }

    override fun deleteResource(projectId: String, credentialId: String) {
        return
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamCredentialPermissionServiceImpl::class.java)
    }
}
