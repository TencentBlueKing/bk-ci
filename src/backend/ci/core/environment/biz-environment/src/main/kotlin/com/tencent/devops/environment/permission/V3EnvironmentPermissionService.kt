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
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("ALL")
class V3EnvironmentPermissionService constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    environmentAuthServiceCode: EnvironmentAuthServiceCode
) : AbstractEnvironmentPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    environmentAuthServiceCode = environmentAuthServiceCode
) {
    override fun supplierForEnvFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun supplierForNodeFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        envId: Long,
        permission: AuthPermission
    ): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkEnvPermission(userId, projectId, envId, permission)
    }

    override fun checkEnvPermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkEnvPermission(userId, projectId, permission)
    }

    override fun checkNodePermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkNodePermission(userId, projectId, nodeId, permission)
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkNodePermission(userId, projectId, permission)
    }

    override fun listEnvByViewPermission(userId: String, projectId: String): Set<Long> {
        return listEnvByPermission(userId, projectId, AuthPermission.USE)
    }

    // 解密后
    override fun listEnvByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val resourceInstances = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            authPermissionApi.getUserResourceByPermission(
                user = userId,
                serviceCode = environmentAuthServiceCode,
                resourceType = envResourceType,
                projectCode = projectId,
                permission = permission,
                supplier = supplierForEnvFakePermission(projectId)
            )
        }

        return getAllEnvInstance(resourceInstances, projectId, userId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    // 加密
    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val instanceResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForEnvFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        instanceResourcesMap.forEach { (key, value) ->
            val envs = if (isProjectOwner(projectId, userId)) {
                getAllEnvInstance(arrayListOf("*"), projectId, userId).toList()
            } else {
                getAllEnvInstance(value, projectId, userId).toList()
            }
            instanceMap[key] = envs.map { it }
        }
        logger.info("listEnvByPermissions v3Impl [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    // 解密后
    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val resourceInstances = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            authPermissionApi.getUserResourceByPermission(
                user = userId,
                serviceCode = environmentAuthServiceCode,
                resourceType = nodeResourceType,
                projectCode = projectId,
                permission = permission,
                supplier = supplierForEnvFakePermission(projectId)
            )
        }

        return getAllNodeInstance(resourceInstances, projectId, userId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    // 加密
    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val instanceResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForEnvFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        instanceResourcesMap.forEach { (key, value) ->
            val nodes = if (isProjectOwner(projectId, userId)) {
                getAllNodeInstance(arrayListOf("*"), projectId, userId).toList().map { it }
            } else {
                getAllNodeInstance(value, projectId, userId).toList().map { it }
            }
            logger.info("listNodeByPermissions v3Impl [$nodes] ")
            instanceMap[key] = nodes
        }
        logger.info("listNodeByPermissions v3Impl [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    // 拿到的数据统一为加密后的id
    private fun getAllNodeInstance(resourceCodeList: List<String>, projectId: String, userId: String): Set<String> {
        val instanceIds = mutableSetOf<String>()
        if (resourceCodeList.contains("*")) {
            val repositoryInfos = nodeDao.listNodes(dslContext, projectId)
            repositoryInfos.map {
                instanceIds.add(HashUtil.encodeLongId(it.nodeId))
            }
            return instanceIds
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
    }

    // 拿到的数据统一为加密后的id
    private fun getAllEnvInstance(resourceCodeList: List<String>, projectId: String, userId: String): Set<String> {
        val instanceIds = mutableSetOf<String>()

        if (resourceCodeList.contains("*")) {
            val repositoryInfos = envDao.list(dslContext, projectId)
            repositoryInfos.map {
                instanceIds.add(HashUtil.encodeLongId(it.envId))
            }
            return instanceIds
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
    }

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("env permission get ProjectOwner $projectId | $projectCreator| $userId")
            return if (!projectCreator.isNullOrEmpty()) {
                redisOperation.set(OwnerUtils.getOwnerRedisKey(projectId), projectCreator!!)
                userId == projectCreator
            } else {
                false
            }
        } else {
            return userId == cacheOwner
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(V3EnvironmentPermissionService::class.java)
    }
}
