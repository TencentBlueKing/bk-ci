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

package com.tencent.devops.environment.permission.impl

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * Pipeline专用权限校验接口
 */
class EnvironmentPermissionServiceImpl @Autowired constructor(
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val environmentAuthServiceCode: EnvironmentAuthServiceCode,
    private val managerService: ManagerService,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val dslContext: DSLContext
) : EnvironmentPermissionService {

    private val envResourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT
    private val nodeResourceType = AuthResourceType.ENVIRONMENT_ENV_NODE

    override fun listEnvByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val envHashIdSet = mutableSetOf<Long>()
        if (managerService.isManagerPermission(
                userId = userId,
                projectId = projectId,
                resourceType = envResourceType,
                authPermission = permission
            )) {
            val envRecords = envDao.list(dslContext, projectId)
            envRecords.map { envHashIdSet.add(it.envId) }
            return envHashIdSet
        }

        val iamInstance = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        ).map { HashUtil.decodeIdToLong(it) }.toSet()

        envHashIdSet.addAll(iamInstance)
        return envHashIdSet
    }

    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {

        val iamInstancesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = null
        )

        val managerPermissionMap = mutableMapOf<AuthPermission, List<String>>()
        val envIdList = envDao.list(dslContext, projectId).map { HashUtil.encodeLongId(it.envId) }
        var isChange = false
        iamInstancesMap.keys.forEach {
            if (managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    resourceType = envResourceType,
                    authPermission = it
                )) {
                if (iamInstancesMap[it] == null) {
                    managerPermissionMap[it] = envIdList
                } else {
                    val collectionSet = mutableSetOf<String>()
                    collectionSet.addAll(envIdList.toSet())
                    collectionSet.addAll(iamInstancesMap[it]!!.toSet())
                    managerPermissionMap[it] = collectionSet.toList()
                }

                isChange = true
            } else {
                managerPermissionMap[it] = iamInstancesMap[it] ?: emptyList()
            }
        }

        if (isChange) {
            return managerPermissionMap
        }
        return iamInstancesMap
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
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(envId),
            permission = permission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = envResourceType,
            authPermission = permission
        )
    }

    override fun checkEnvPermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            resourceCode = "*",
            permission = permission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = envResourceType,
            authPermission = permission
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
        val nodeHashIds = mutableSetOf<Long>()
        if (managerService.isManagerPermission(
                userId = userId,
                projectId = projectId,
                resourceType = nodeResourceType,
                authPermission = permission
            )) {
            val nodeRecords = nodeDao.listNodes(dslContext, projectId)
            nodeRecords.map { nodeHashIds.add(it.nodeId) }
            return nodeHashIds
        }
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        ).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val iamPermissionMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = null
        )

        val managerPermissionMap = mutableMapOf<AuthPermission, List<String>>()
        val envIdList = nodeDao.listNodes(dslContext, projectId).map { HashUtil.encodeLongId(it.nodeId) }
        var isChange = false
        iamPermissionMap.keys.forEach {
            if (managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    resourceType = nodeResourceType,
                    authPermission = it
                )) {
                if (iamPermissionMap[it] == null) {
                    managerPermissionMap[it] = envIdList
                } else {
                    val collectionSet = mutableSetOf<String>()
                    collectionSet.addAll(envIdList.toSet())
                    collectionSet.addAll(iamPermissionMap[it]!!.toSet())
                    managerPermissionMap[it] = collectionSet.toList()
                }

                isChange = true
            } else {
                managerPermissionMap[it] = iamPermissionMap[it] ?: emptyList()
            }
        }

        if (isChange) {
            logger.info("listNodeByPermissions $userId $projectId is manager, map: $managerPermissionMap")
            return managerPermissionMap
        }
        logger.info("listNodeByPermissions $userId $projectId not manager, map: $iamPermissionMap")
        return iamPermissionMap
    }

    override fun listNodeByListPermission(userId: String, projectId: String, nodeRecordList: List<TNodeRecord>): List<TNodeRecord> {
        return nodeRecordList
    }

    override fun checkNodePermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            permission = permission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = nodeResourceType,
            authPermission = permission
        )
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        val iamPermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            resourceCode = "*",
            permission = permission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = nodeResourceType,
            authPermission = permission
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

    override fun isRbac(): Boolean = false

    companion object {
        private val logger = LoggerFactory.getLogger(EnvironmentPermissionServiceImpl::class.java)
    }
}
