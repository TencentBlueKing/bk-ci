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

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacEnvironmentPermissionService(
    private val dslContext: DSLContext,
    private val client: Client,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val tokenCheckService: ClientTokenService
) : EnvironmentPermissionService {
    val envResourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT
    val nodeResourceType = AuthResourceType.ENVIRONMENT_ENV_NODE

    override fun listEnvByPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Set<Long> {
        val resourceInstances = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            action = buildEnvAction(permission),
            projectCode = projectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value
        ).data ?: emptyList()
        val projectAllId = mutableListOf<String>()
        envDao.list(dslContext, projectId).map {
            projectAllId.add(HashUtil.encodeLongId(it.envId))
        }
        return getAllEnvInstance(resourceInstances, projectAllId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listEnvByPermissions(userId: String, projectId: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val actions = RbacAuthUtils.buildActionList(permissions, AuthResourceType.ENVIRONMENT_ENVIRONMENT)
        val instanceResourcesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
            action = actions
        ).data ?: emptyMap()
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        val projectAllId = mutableListOf<String>()
        // iam存储的为hashId,故全量数据需要转hash
        envDao.list(dslContext, projectId).map {
            projectAllId.add(HashUtil.encodeLongId(it.envId))
        }
        instanceResourcesMap.forEach { (key, value) ->
            val envs = getAllEnvInstance(value, projectAllId).toList()

            instanceMap[key] = envs.map { it }
            // todo 再确定一下，这里要去掉，因为rbac有  list权限，会拿 list动作，去获取资源，所以不会拉到 list动作的权限
            /*if (key == AuthPermission.VIEW) {
                instanceMap[AuthPermission.LIST] = envs.map { it }
            }*/
        }
        logger.info("listEnvByPermissions Rbac [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        envId: Long,
        permission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId), // 此处之所以要加密,为兼容企业版。已发布的企业版记录的为hashId
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
            relationResourceType = null,
            action = buildEnvAction(permission)
        ).data ?: false
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            relationResourceType = null,
            action = buildEnvAction(permission)
        ).data ?: false
    }

    override fun createEnv(userId: String, projectId: String, envId: Long, envName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun updateEnv(userId: String, projectId: String, envId: Long, envName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(envId),
            resourceName = envName
        )
    }

    override fun deleteEnv(projectId: String, envId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(envId),
        )
    }

    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val resourceInstances = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            action = buildNodeAction(permission),
            projectCode = projectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value
        ).data ?: emptyList()

        val instanceIds = mutableListOf<String>()
        nodeDao.listNodes(dslContext, projectId).map {
            instanceIds.add(HashUtil.encodeLongId(it.nodeId))
        }

        return getAllNodeInstance(resourceInstances, instanceIds).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listNodeByPermissions(userId: String, projectId: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val actions = RbacAuthUtils.buildActionList(permissions, AuthResourceType.ENVIRONMENT_ENV_NODE)

        val instanceResourcesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value,
            action = actions
        ).data ?: emptyMap()

        // iam存储的为hashId,故全量数据需要转hash
        val instanceIds = mutableListOf<String>()
        nodeDao.listNodes(dslContext, projectId).map {
            instanceIds.add(HashUtil.encodeLongId(it.nodeId))
        }

        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        instanceResourcesMap.forEach { (key, value) ->
            val nodes = getAllNodeInstance(value, instanceIds).map { it }
            logger.info("listNodeByPermissions Rbac [$nodes] ")
            instanceMap[key] = nodes
        }
        logger.info("listNodeByPermissions Rbac [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    override fun checkNodePermission(userId: String, projectId: String, nodeId: Long, permission: AuthPermission): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId), // 此处之所以要加密,为兼容企业版。已发布的企业版记录的为hashId
            resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value,
            relationResourceType = null,
            action = buildNodeAction(permission)
        ).data ?: false
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            relationResourceType = null,
            action = buildNodeAction(permission)
        ).data ?: false
    }

    override fun createNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun updateNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = nodeName
        )
    }

    override fun deleteNode(projectId: String, nodeId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = envResourceType.value,
            resourceCode = HashUtil.encodeLongId(nodeId),
        )
    }

    // 拿到的数据统一为加密后的id
    private fun getAllNodeInstance(resourceCodeList: List<String>, projectAllResourceId: List<String>): List<String> {
        val instanceIds = mutableListOf<String>()
        if (resourceCodeList.contains("*")) {
            return projectAllResourceId
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
    }

    private fun getAllEnvInstance(
        resourceCodeList: List<String>,
        projectAllResourceId: List<String>
    ): Set<String> {
        val instanceIds = mutableSetOf<String>()

        if (resourceCodeList.contains("*")) {
            return projectAllResourceId.toSet()
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
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
