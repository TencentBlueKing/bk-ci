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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.openapi.service.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectVO
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.ws.rs.HeaderParam

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwProjectService(
    private val client: Client,
    private val organizationProjectService: OrganizationProjectService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectService::class.java)
    }

    fun getListByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "ApigwProjectService"
    ): List<ProjectVO>? {
        logger.info("$interfaceName:getListByOrganizationId:Input($userId,$organizationType,$organizationId,$deptName,$centerName)")
        return client.get(ServiceTxProjectResource::class).getProjectByOrganizationId(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName?.trim(),
            centerName = centerName?.trim()
        ).data
    }

    fun createProjectUserByUser(
        createUserId: String,
        accessToken: String,
        createInfo: ProjectCreateUserDTO
    ): Boolean? {
        logger.info("createProjectUserByUser:createUserId[$createUserId],accessToken[$accessToken],createInfo[$createInfo]")
        return client.get(ServiceTxProjectResource::class).createProjectaUserByUser(createUserId, accessToken, createInfo).data
    }

    fun createProjectUserByApp(
        organizationType: String,
        organizationId: Long,
        createInfo: ProjectCreateUserDTO
    ): Boolean? {
        logger.info("createProjectUserByApp:organizationType[$organizationType],organizationId[$organizationId],createInfo[$createInfo]")
        return client.get(ServiceTxProjectResource::class).createProjectaUserByApp(organizationType, organizationId, createInfo).data
    }

    fun createPipelinePermission(
        createUserId: String,
        accessToken: String,
        createInfo: PipelinePermissionInfo
    ): Boolean? {
        logger.info("createPipelinePermission:createUserId[$createUserId],accessToken[$accessToken],createInfo[$createInfo]")
        return client.get(ServiceTxProjectResource::class).createUserPipelinePermission(createUserId, accessToken, createInfo).data
    }
}