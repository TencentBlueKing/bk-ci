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

package com.tencent.devops.repository.service.permission

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.dao.RepositoryDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("ALL")
class V3RepositoryPermissionService constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val repositoryDao: RepositoryDao,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val codeAuthServiceCode: CodeAuthServiceCode
) : AbstractRepositoryPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    codeAuthServiceCode = codeAuthServiceCode
) {

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        repositoryId: Long?,
        message: String
    ) {
        if (isProjectOwner(projectId, userId)) {
            return
        }
        super.validatePermission(userId, projectId, authPermission, repositoryId, message)
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        repositoryId: Long?
    ): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.hasPermission(userId, projectId, authPermission, repositoryId)
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        val resourceCodeList = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            authPermissionApi.getUserResourceByPermission(
                user = userId,
                serviceCode = codeAuthServiceCode,
                resourceType = AuthResourceType.CODE_REPERTORY,
                projectCode = projectId,
                permission = authPermission,
                supplier = supplierForFakePermission(projectId)
            )
        }

        if (resourceCodeList.contains("*")) {
            return getAllInstance(resourceCodeList, projectId, userId)
        }
        return resourceCodeList.map { HashUtil.decodeOtherIdToLong(it) }
    }

    override fun filterRepositories(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = supplierForFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<Long>>()

        permissionResourcesMap.forEach { (key, value) ->
            instanceMap[key] = if (isProjectOwner(projectId, userId)) {
                getAllInstance(arrayListOf("*"), projectId, userId)
            } else {
                getAllInstance(value, projectId, userId)
            }
        }
        return instanceMap
    }

    private fun getAllInstance(resourceCodeList: List<String>, projectId: String, userId: String): List<Long> {
        if (resourceCodeList.contains("*")) {
            val instanceIds = mutableListOf<Long>()
            val repositoryInfos = repositoryDao.listByProject(dslContext, projectId, null)
            repositoryInfos.map {
                instanceIds.add(it.repositoryId)
            }
            return instanceIds
        }
        return resourceCodeList.map { HashUtil.decodeOtherIdToLong(it) }
    }

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("repository permission get ProjectOwner $projectId | $projectCreator| $userId")
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
        val logger = LoggerFactory.getLogger(V3RepositoryPermissionService::class.java)
    }
}
