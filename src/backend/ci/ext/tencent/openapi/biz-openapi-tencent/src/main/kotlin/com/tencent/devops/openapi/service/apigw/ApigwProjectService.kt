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

import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwProjectService(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
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
        logger.info(
            "$interfaceName:getListByOrganizationId:Input(" +
                    "$userId,$organizationType,$organizationId,$deptName,$centerName)"
        )
        return client.get(ServiceTxProjectResource::class).getProjectByName(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName?.trim(),
            centerName = centerName?.trim()
        ).data
    }

    fun getProjectByName(
        userId: String,
        organizationType: String,
        organizationId: Long,
        name: String,
        nameType: ProjectValidateType
    ): ProjectVO? {
        return client.get(ServiceTxProjectResource::class).getProjectByName(
            userId = userId,
            name = name,
            organizationId = organizationId,
            organizationType = organizationType,
            showSecrecy = false,
            nameType = nameType
        ).data
    }

    fun createProjectUserByUser(
        createUserId: String,
        createInfo: ProjectCreateUserDTO
    ): Boolean? {
        logger.info("createProjectUserByUser:createUserId[$createUserId],createInfo[$createInfo]")
        val info = ProjectCreateUserDTO(
            userId = createInfo.userId,
            roleId = createInfo.roleId,
            roleName = createInfo.roleName,
            projectId = createInfo.projectId,
            userIds = arrayListOf(createInfo.userId!!)
        )
        return client.get(ServiceTxProjectResource::class).createProjectUser(createUserId, info).data
    }

    fun createProjectUser(
        createUserId: String,
        createInfo: ProjectCreateUserDTO
    ): Boolean? {
        logger.info("createProjectUser:createUserId[$createUserId],createInfo[$createInfo]")
        return client.get(ServiceTxProjectResource::class).createProjectUser(createUserId, createInfo).data
    }

    fun createProjectUserByApp(
        organizationType: String,
        organizationId: Long,
        createInfo: ProjectCreateUserDTO
    ): Boolean? {
        logger.info(
            "createProjectUserByApp:organizationType[" +
                    "$organizationType],organizationId[$organizationId],createInfo[$createInfo]"
        )
        return client.get(ServiceTxProjectResource::class).createProjectUserByApp(
            organizationType = organizationType,
            organizationId = organizationId,
            createInfo = createInfo
        ).data
    }

    fun createPipelinePermissionByUser(
        createUserId: String,
        accessToken: String,
        createInfo: PipelinePermissionInfo
    ): Boolean? {
        logger.info(
            "createPipelinePermission:createUserId[" +
                    "$createUserId],accessToken[$accessToken],createInfo[$createInfo]"
        )
        return client.get(ServiceTxProjectResource::class).createUserPipelinePermissionByUser(
            accessToken = createUserId,
            createUser = accessToken,
            createInfo = createInfo
        ).data
    }

    fun createPipelinePermissionByApp(
        organizationType: String,
        organizationId: Long,
        createInfo: PipelinePermissionInfo
    ): Boolean? {
        logger.info(
            "createPipelinePermission:organizationType[" +
                    "$organizationType],organizationId[$organizationId],createInfo[$createInfo]"
        )
        return client.get(ServiceTxProjectResource::class).createUserPipelinePermissionByApp(
            organizationType = organizationType,
            organizationId = organizationId,
            createInfo = createInfo
        ).data
    }

    fun getProjectRoles(
        organizationType: String,
        organizationId: Long,
        projectCode: String
    ): List<BKAuthProjectRolesResources>? {
        logger.info(
            "createPipelinePermission:organizationType[" +
                    "$organizationType],organizationId[$organizationId],projectCode[$projectCode]"
        )
        return client.get(ServiceTxProjectResource::class).getProjectRoles(
            projectCode = projectCode,
            organizationType = organizationType,
            organizationId = organizationId
        ).data
    }

    fun setProjectRouteType(
        projectCode: String
    ) {
        val projectRouteTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectCode)
        if (!projectRouteTag.isNullOrBlank()) {
            bkTag.setGatewayTag(projectRouteTag)
        }
    }
}
