/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkiam

import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.ResourceBaseRequest
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.SystemCode
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * 对接蓝鲸权限中心3.0
 */
class BkiamPermissionServiceImpl constructor(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    private val bkiamService: BkiamService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("checkPermission, request: $request")
        if (request.resourceType != ResourceType.SYSTEM && checkBkiamPermission(request)) {
            logger.debug("checkBkiamPermission passed")
            return true
        }
        return super.checkPermission(request)
    }

    override fun registerResource(request: RegisterResourceRequest) {
        logger.info("registerResource, request: $request")
        val resourceId = getResourceId(request)
        bkiamService.createResource(
            userId = request.uid,
            systemCode = SystemCode.BK_REPO,
            projectId = request.projectId!!,
            resourceType = request.resourceType,
            resourceId = resourceId,
            resourceName = resourceId
        )
    }

    private fun checkBkiamPermission(request: CheckPermissionRequest): Boolean {
        return bkiamService.validateResourcePermission(
            userId = request.uid,
            systemCode = SystemCode.BK_REPO,
            projectId = request.projectId!!,
            resourceType = request.resourceType,
            action = request.action,
            resourceId = getResourceId(request)
        )
    }

    private fun getResourceId(request: ResourceBaseRequest): String {
        return when (request.resourceType) {
            ResourceType.SYSTEM -> StringPool.EMPTY
            ResourceType.PROJECT -> request.projectId!!
            ResourceType.REPO -> request.repoName!!
            ResourceType.NODE -> throw IllegalArgumentException("invalid resource type")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkiamPermissionServiceImpl::class.java)
    }
}
