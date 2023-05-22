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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.service.CertPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamCertPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val certDao: CertDao,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService
) : CertPermissionService {
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
        if (authPermission == AuthPermission.VIEW)
            return
        val permissionCheck = validatePermission(userId, projectId, authPermission)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        logger.info("StreamCertPermissionServiceImpl user:$userId projectId: $projectId ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenService.getSystemToken(null) ?: "",
            action = AuthPermission.ENABLE.value, // 证书类所有检验都以操作类来校验。即便是view也走操作类权限校验
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

    override fun filterCert(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): List<String> {
        val checkPermission = validatePermission(userId, projectId, authPermission)
        if (!checkPermission) {
            return emptyList()
        }
        return getAllCertByProject(projectId)
    }

    override fun filterCerts(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        // 证书类所有检验都以操作类来校验。即便是view也走操作类权限校验. 若后续产品有调整,此处需要拆解为不同的permission类型
        val checkPermission = validatePermission(userId, projectId, AuthPermission.ENABLE)
        if (!checkPermission) {
            return emptyMap()
        }
        val projectAllInstances = getAllCertByProject(projectId)
        val resultMap = mutableMapOf<AuthPermission, List<String>>()
        authPermissions.forEach {
            resultMap[it] = projectAllInstances
        }
        return resultMap
    }

    override fun createResource(
        userId: String,
        projectId: String,
        certId: String
    ) {
        return
    }

    override fun deleteResource(
        projectId: String,
        certId: String
    ) {
        return
    }

    private fun getAllCertByProject(projectId: String): List<String> {
        val idList = mutableListOf<String>()
        val count = certDao.countByProject(dslContext, projectId, null)
        val records = certDao.listByProject(dslContext, projectId, 0, count.toInt())
        records.map {
            idList.add(it.certId)
        }
        return idList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamCertPermissionServiceImpl::class.java)
    }
}
