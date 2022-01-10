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
package com.tencent.devops.openapi.service.apigw

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwBuildService(
    private val client: Client,
    private val organizationProjectService: OrganizationProjectService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildService::class.java)
    }

    fun getStatusWithoutPermission(
        userId: String,
        organizationType: String,
        organizationId: Long,
        projectId: String,
        pipelineId: String,
        buildId: String,
        interfaceName: String? = "ApigwBuildService"
    ): Result<BuildHistoryWithVars> {
        logger.info("$interfaceName:getStatusWithoutPermission:Input($userId,$organizationType,$organizationId,$projectId,$pipelineId,$buildId)")
        // 根据部门进行鉴权
        val projectIds = organizationProjectService.getProjectIdsByOrganizationTypeAndId(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = null,
            centerName = null,
            interfaceName = interfaceName
        )
        logger.info("$interfaceName:getStatusWithoutPermission:Inner:projectIds=$projectIds")
        if (!projectIds.contains(projectId)) {
            val message = "($organizationType,$organizationId) can not access project($projectId)"
            logger.warn("$interfaceName:PermissionForbidden:$message")
            throw PermissionForbiddenException(
                message = message
            )
        }
        return client.get(ServiceBuildResource::class).getBuildStatusWithoutPermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }
}
