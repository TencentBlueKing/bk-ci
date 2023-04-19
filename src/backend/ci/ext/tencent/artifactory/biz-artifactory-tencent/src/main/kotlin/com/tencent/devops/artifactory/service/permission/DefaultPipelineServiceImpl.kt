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

package com.tencent.devops.artifactory.service.permission

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class DefaultPipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val pipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val bkAuthPermissionApi: BSAuthPermissionApi,
    private val authProjectApi: BSAuthProjectApi,
    private val artifactoryAuthServiceCode: BSRepoAuthServiceCode
) : PipelineService(client) {

    private val resourceType = AuthResourceType.PIPELINE_DEFAULT

    override fun validatePermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?,
        message: String?
    ) {
        if (!hasPermission(userId, projectId, pipelineId, permission)) {
            throw PermissionForbiddenException(message ?: "permission denied")
        }
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?
    ): Boolean {
        return if (pipelineId == null) {
            authProjectApi.checkProjectUser(userId, artifactoryAuthServiceCode, projectId)
        } else {
            bkAuthPermissionApi.validateUserResourcePermission(
                userId,
                pipelineAuthServiceCode,
                AuthResourceType.PIPELINE_DEFAULT,
                projectId,
                pipelineId,
                permission ?: AuthPermission.DOWNLOAD
            )
        }
    }

    override fun filterPipeline(user: String, projectId: String): List<String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            return bkAuthPermissionApi.getUserResourceByPermission(
                user,
                pipelineAuthServiceCode,
                resourceType,
                projectId,
                AuthPermission.LIST,
                null
            )
        } finally {
            logger.info("filterPipeline cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(DefaultPipelineServiceImpl::class.java)
    }
}
