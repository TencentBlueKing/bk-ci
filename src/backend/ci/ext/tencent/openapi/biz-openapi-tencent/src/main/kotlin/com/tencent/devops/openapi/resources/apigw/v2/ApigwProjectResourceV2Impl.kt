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
package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwProjectResourceV2
import com.tencent.devops.openapi.service.apigw.ApigwProjectService
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ApigwProjectResourceV2Impl @Autowired constructor(
    private val client: Client,
    private val apigwProjectService: ApigwProjectService,
    private val bkTag: BkTag
) : ApigwProjectResourceV2 {

    @Value("\${project.route.tag:#{null}}")
    private val projectRouteTag: String? = ""

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        accessToken: String,
        projectCreateInfo: ProjectCreateInfo
    ): Result<String> {
        return createProjectSetRouter(
            appCode = appCode,
            apigwType = apigwType,
            userId = userId,
            accessToken = accessToken,
            routerTag = null,
            projectCreateInfo = projectCreateInfo
        )
    }

    override fun createProjectSetRouter(
        appCode: String?,
        apigwType: String?,
        userId: String,
        accessToken: String,
        routerTag: String?,
        projectCreateInfo: ProjectCreateInfo
    ): Result<String> {
        logger.info("v2/projects/newProject:create:Input($userId,$accessToken,$projectCreateInfo,$routerTag)")

        // 创建项目需要指定对接的主集群。 不同集群可能共用同一个套集群
        if (!projectRouteTag.isNullOrEmpty()) {
            bkTag.setGatewayTag(projectRouteTag)
        }
        return Result(
            client.get(ServiceTxProjectResource::class).create(
                userId = userId,
                accessToken = accessToken,
                projectCreateInfo = projectCreateInfo,
                routerTag = routerTag
            ).data!!
        )
    }

    override fun listProjectByOrganizationId(
        appCode: String?,
        apigwType: String?,
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>?> {
        return Result(
            apigwProjectService.getListByOrganizationId(
                userId = userId,
                organizationType = organizationType,
                organizationId = organizationId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/v2/projects/getProjectByOrganizationId"
            )
        )
    }

    override fun getProjectByOrganizationId(
        appCode: String?,
        apigwType: String?,
        userId: String,
        organizationType: String,
        organizationId: Long,
        name: String,
        nameType: ProjectValidateType
    ): Result<ProjectVO?> {
        return Result(
            apigwProjectService.getProjectByName(
                userId = userId,
                organizationType = organizationType,
                organizationId = organizationId,
                name = name,
                nameType = nameType
            )
        )
    }

    override fun createProjectUserByUser(
        appCode: String?,
        apigwType: String?,
        createUserId: String,
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?> {
        // 设置项目对应的consulTag
        apigwProjectService.setProjectRouteType(createInfo.projectId)
        return Result(apigwProjectService.createProjectUser(
            createUserId = createUserId,
            checkManager = true,
            createInfo = createInfo
        ))
    }

    override fun createProjectUser(
        appCode: String?,
        apigwType: String?,
        createUserId: String,
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?> {
        // 设置项目对应的consulTag
        apigwProjectService.setProjectRouteType(createInfo.projectId)
        return Result(apigwProjectService.createProjectUser(
            createUserId = createUserId,
            checkManager = true,
            createInfo = createInfo
        ))
    }

    override fun createProjectaUserByApp(
        appCode: String?,
        apigwType: String?,
        organizationType: String,
        organizationId: Long,
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?> {
        // 设置项目对应的consulTag
        apigwProjectService.setProjectRouteType(createInfo.projectId)
        return Result(
            apigwProjectService.createProjectUser(
                createUserId = "",
                checkManager = false,
                createInfo = createInfo
            )
        )
    }

    override fun createUserPipelinePermission(
        appCode: String?,
        apigwType: String?,
        createUserId: String?,
        checkManager: Boolean?,
        createInfo: PipelinePermissionInfo
    ): Result<Boolean?> {
        // 设置项目对应的consulTag
        apigwProjectService.setProjectRouteType(createInfo.projectId)
        return Result(
            apigwProjectService.createPipelinePermission(
                createUserId = createUserId,
                checkManager = checkManager ?: true,
                createInfo = createInfo
            )
        )
    }

    override fun getProjectRoles(
        appCode: String?,
        apigwType: String?,
        organizationType: String,
        organizationId: Long,
        projectId: String
    ): Result<List<BKAuthProjectRolesResources>?> {
        return Result(
            apigwProjectService.getProjectRoles(
                organizationType = organizationType,
                organizationId = organizationId,
                projectCode = projectId
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceV2Impl::class.java)
    }
}
