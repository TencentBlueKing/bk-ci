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

package com.tencent.devops.environment.permission

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord

/**
 * 权限校验接口
 */
abstract class AbstractEnvironmentPermissionService constructor(
    private val authResourceApi: AuthResourceApi,
    val authPermissionApi: AuthPermissionApi,
    val environmentAuthServiceCode: EnvironmentAuthServiceCode
) : EnvironmentPermissionService {

    val envResourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT
    val nodeResourceType = AuthResourceType.ENVIRONMENT_ENV_NODE

    abstract fun supplierForEnvFakePermission(projectId: String): () -> MutableList<String>

    abstract fun supplierForNodeFakePermission(projectId: String): () -> MutableList<String>

    override fun listEnvByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = supplierForEnvFakePermission(projectId)
        ).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listEnvByViewPermission(
        userId: String,
        projectId: String
    ): Set<Long> {
        return listEnvByPermission(userId, projectId, AuthPermission.USE)
    }

    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForEnvFakePermission(projectId)
        )
    }

    override fun getEnvListResult(canListEnv: List<TEnvRecord>, envRecordList: List<TEnvRecord>): List<TEnvRecord> {
        return envRecordList
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        envId: Long,
        permission: AuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId),
            permission = permission
        )
    }

    override fun checkEnvPermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
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

    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = supplierForNodeFakePermission(projectId)
        ).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForNodeFakePermission(projectId)
        )
    }

    override fun listNodeByRbacPermission(
        userId: String,
        projectId: String,
        nodeRecordList: List<TNodeRecord>,
        authPermission: AuthPermission
    ): List<TNodeRecord> {
        return nodeRecordList
    }

    override fun checkNodePermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        if (permission == AuthPermission.VIEW)
            return true
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            permission = permission
        )
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = "*",
            permission = permission
        )
    }

    override fun createNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun updateNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
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
