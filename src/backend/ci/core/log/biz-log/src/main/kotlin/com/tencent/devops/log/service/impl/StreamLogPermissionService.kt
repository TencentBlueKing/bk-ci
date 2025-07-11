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

package com.tencent.devops.log.service.impl

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.log.service.LogPermissionService
import org.slf4j.LoggerFactory

class StreamLogPermissionService(
    val client: Client,
    private val tokenCheckService: ClientTokenService
) : LogPermissionService {

    override fun verifyUserLogPermission(
        projectCode: String,
        userId: String,
        permission: AuthPermission?,
        authResourceType: AuthResourceType?
    ): Boolean {
        logger.info("StreamLogPermissionService user:$userId projectId: $projectCode ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(),
            action = permission?.value ?: AuthPermission.VIEW.value,
            projectCode = projectCode,
            resourceCode = authResourceType?.value ?: AuthResourceType.PIPELINE_DEFAULT.value
        ).data ?: false
    }

    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?,
        authResourceType: AuthResourceType?
    ): Boolean {
        val action = permission?.value ?: AuthPermission.VIEW.value
        logger.info("StreamLogPermissionService user:$userId projectId: $projectCode ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(),
            action = action,
            projectCode = projectCode,
            resourceCode = authResourceType?.value ?: AuthResourceType.PIPELINE_DEFAULT.value
        ).data ?: false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamLogPermissionService::class.java)
    }
}
