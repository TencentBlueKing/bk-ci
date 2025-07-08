/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.bk.sdk.iam.util.AuthCacheUtil
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.auth.utils.AuthCacheKeyUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.LoggerFactory

class RbacEnvironmentPermissionService(
    private val client: Client,
    private val tokenCheckService: ClientTokenService
) : EnvironmentPermissionService {
    val envResourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value
    val nodeResourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value

    override fun listEnvByPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Set<Long> {
        return client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenCheckService.getSystemToken()!!,
            userId = userId,
            action = buildEnvAction(permission),
            projectCode = projectId,
            resourceType = envResourceType
        ).data?.map { HashUtil.decodeIdToLong(it) }?.toSet() ?: emptySet()
    }

    override fun listEnvByViewPermission(
        userId: String,
        projectId: String
    ): Set<Long> {
        return listEnvByPermission(userId, projectId, AuthPermission.VIEW)
    }

    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenCheckService.getSystemToken()!!,
            userId = userId,
            projectCode = projectId,
            resourceType = envResourceType,
            action = RbacAuthUtils.buildActionList(permissions, AuthResourceType.ENVIRONMENT_ENVIRONMENT)
        ).data ?: emptyMap()
    }

    override fun getEnvListResult(canListEnv: List<TEnvRecord>, envRecordList: List<TEnvRecord>): List<TEnvRecord> {
        return canListEnv
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        envId: Long,
        permission: AuthPermission
    ): Boolean {
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = userId,
            resourceType = envResourceType,
            action = buildEnvAction(permission),
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId)
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenCheckService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(envId), // 此处之所以要加密,为兼容企业版。已发布的企业版记录的为hashId
                resourceType = envResourceType,
                relationResourceType = null,
                action = buildEnvAction(permission)
            ).data ?: false
        }
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = userId,
            resourceType = AuthResourceType.PROJECT.value,
            action = buildEnvAction(permission),
            projectCode = projectId,
            resourceCode = projectId
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenCheckService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceCode = projectId,
                resourceType = AuthResourceType.PROJECT.value,
                relationResourceType = null,
                action = buildEnvAction(permission)
            ).data ?: false
        }
    }

    override fun createEnv(userId: String, projectId: String, envId: Long, envName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = envResourceType,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun updateEnv(userId: String, projectId: String, envId: Long, envName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = envResourceType,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun deleteEnv(projectId: String, envId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = envResourceType,
            resourceCode = HashUtil.encodeLongId(envId)
        )
    }

    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        return client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenCheckService.getSystemToken()!!,
            userId = userId,
            action = buildNodeAction(permission),
            projectCode = projectId,
            resourceType = nodeResourceType
        ).data?.map { HashUtil.decodeIdToLong(it) }?.toSet() ?: emptySet()
    }

    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenCheckService.getSystemToken()!!,
            userId = userId,
            projectCode = projectId,
            resourceType = nodeResourceType,
            action = RbacAuthUtils.buildActionList(permissions, AuthResourceType.ENVIRONMENT_ENV_NODE)
        ).data ?: emptyMap()
    }

    override fun listNodeByRbacPermission(
        userId: String,
        projectId: String,
        nodeRecordList: List<TNodeRecord>,
        authPermission: AuthPermission
    ): List<TNodeRecord> {
        val hasRbacPermissionNodeIds = listNodeByPermission(userId, projectId, authPermission)
        val hasRbacPermissionNode = nodeRecordList.filter { hasRbacPermissionNodeIds.contains(it.nodeId) }
        return hasRbacPermissionNode.ifEmpty { emptyList() }
    }

    override fun checkNodePermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = userId,
            resourceType = nodeResourceType,
            action = buildNodeAction(permission),
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId)
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenCheckService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(nodeId),
                resourceType = nodeResourceType,
                relationResourceType = null,
                action = buildNodeAction(permission)
            ).data ?: false
        }
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = userId,
            resourceType = AuthResourceType.PROJECT.value,
            action = buildNodeAction(permission),
            projectCode = projectId,
            resourceCode = projectId
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenCheckService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceCode = projectId,
                resourceType = AuthResourceType.PROJECT.value,
                relationResourceType = null,
                action = buildNodeAction(permission)
            ).data ?: false
        }
    }

    override fun createNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = nodeResourceType,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun updateNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = nodeResourceType,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun deleteNode(projectId: String, nodeId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenCheckService.getSystemToken()!!,
            projectCode = projectId,
            resourceType = nodeResourceType,
            resourceCode = HashUtil.encodeLongId(nodeId)
        )
    }

    private fun buildNodeAction(authPermission: AuthPermission): String {
        return RbacAuthUtils.buildAction(authPermission, AuthResourceType.ENVIRONMENT_ENV_NODE)
    }

    private fun buildEnvAction(authPermission: AuthPermission): String {
        return RbacAuthUtils.buildAction(authPermission, AuthResourceType.ENVIRONMENT_ENVIRONMENT)
    }

    companion object {
        val logger = LoggerFactory.getLogger(RbacEnvironmentPermissionService::class.java)
    }
}
