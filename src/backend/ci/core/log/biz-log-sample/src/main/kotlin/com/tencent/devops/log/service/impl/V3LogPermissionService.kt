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

package com.tencent.devops.log.service.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.service.LogPermissionService
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class V3LogPermissionService @Autowired constructor(
    val authPermissionApi: AuthPermissionApi,
    val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val client: Client,
    private val redisOperation: RedisOperation
) : LogPermissionService {
    private val projectOwnerCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String/*projectId*/, String/*userId*/>()

    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        logger.info("checkPipelinePermission only check action project[$projectCode]")
        if (isProjectOwner(projectCode, userId)) {
            logger.info("project owner checkPipelinePermission success |$projectCode|$userId")
            return true
        }
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectCode,
            resourceCode = pipelineId,
            permission = permission ?: AuthPermission.VIEW,
            relationResourceType = null
        )
    }

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val projectCreator = projectOwnerCache.get(projectId) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data
            projectVo?.creator ?: ""
        }
        return userId == projectCreator
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V3LogPermissionService::class.java)
    }
}
