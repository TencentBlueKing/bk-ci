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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.permission.impl

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Pipeline专用权限校验接口
 */
@Service
class EnvironmentPermissionServiceImpl @Autowired constructor(
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val environmentAuthServiceCode: EnvironmentAuthServiceCode
) : EnvironmentPermissionService {

    private val envResourceType = BkAuthResourceType.ENVIRONMENT_ENVIRONMENT
    private val nodeResourceType = BkAuthResourceType.ENVIRONMENT_ENVIRONMENT


    override fun listEnvByPermission(userId: String, projectId: String, permission: BkAuthPermission): Set<Long> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        ).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listEnvByPermissions(userId: String, projectId: String, permissions: Set<BkAuthPermission>)
        : Map<BkAuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = null
        )
    }

    override fun checkEnvPermission(userId: String, projectId: String, envId: Long, permission: BkAuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId),
            permission = permission
        )
    }

    override fun checkEnvPermission(userId: String, projectId: String, permission: BkAuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = "*",
            permission = permission
        )
    }

    override fun createEnv(userId: String, projectId: String, envId: Long, envName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun updateEnv(userId: String, projectId: String, envId: Long, envName: String) {
        authResourceApi.modifyResource(
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun deleteEnv(projectId: String, envId: Long) {
        authResourceApi.deleteResource(
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId)
        )
    }

    override fun listNodeByPermission(userId: String, projectId: String, permission: BkAuthPermission): Set<Long> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        ).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listNodeByPermissions(userId: String, projectId: String, permissions: Set<BkAuthPermission>): Map<BkAuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = null
        )
    }

    override fun checkNodePermission(userId: String, projectId: String, nodeId: Long, permission: BkAuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            permission = permission
        )
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: BkAuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = "*",
            permission = permission
        )
    }

    override fun createNode(user: String, projectId: String, nodeId: Long, nodeName: String) {
        authResourceApi.createResource(
            user = user,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun updateNode(user: String, projectId: String, nodeId: Long, nodeName: String) {
        authResourceApi.modifyResource(
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun deleteNode(projectId: String, nodeId: Long) {
        authResourceApi.deleteResource(
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId)
        )
    }
}