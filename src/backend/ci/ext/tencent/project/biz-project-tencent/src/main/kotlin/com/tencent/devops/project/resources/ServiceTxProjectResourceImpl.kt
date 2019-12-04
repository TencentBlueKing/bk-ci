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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.TxProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTxProjectResourceImpl @Autowired constructor(
    private val projectPermissionService: TxProjectPermissionService,
    private val projectLocalService: ProjectLocalService
) : ServiceTxProjectResource {
    override fun getProjectEnNamesByCenterId(
        userId: String,
        centerId: Long?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByCenterId(
                userId = userId,
                centerId = centerId,
                interfaceName = "/service/projects/enNames/center"
            )
        )
    }

    override fun getProjectEnNamesByDeptIdAndCenterName(
        userId: String,
        deptId: Long?,
        centerName: String?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByOrganization(
                userId = userId,
                deptId = deptId,
                centerName = centerName,
                interfaceName = "/service/projects/enNames/dept"
            )
        )
    }

    override fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByOrganization(
                userId = userId,
                bgId = bgId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/service/projects/enNames/organization"
            )
        )
    }

    override fun getProjectByGroup(
        userId: String,
        bgName: String?,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>> {
        return Result(projectLocalService.getProjectByGroup(userId, bgName, deptName, centerName))
    }

    override fun getProjectByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>> {
        return Result(projectLocalService.getProjectByOrganizationId(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName,
            interfaceName = "/service/project/tx/getProjectByOrganizationId"
        ))
    }

    override fun getProjectByGroupId(
        userId: String,
        bgId: Long?,
        deptId: Long?,
        centerId: Long?
    ): Result<List<ProjectVO>> {
        return Result(projectLocalService.getProjectByGroupId(userId, bgId, deptId, centerId))
    }

    override fun list(accessToken: String): Result<List<ProjectVO>> {
        return Result(projectLocalService.list(accessToken, true))
    }

    override fun getPreUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreatePreProject(userId, accessToken))
    }

    override fun getPreUserProjectV2(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreatePreProject(userId, accessToken))
    }

    // TODO
    override fun create(userId: String, accessToken: String, projectCreateInfo: ProjectCreateInfo): Result<String> {
        return Result(projectLocalService.create(userId, accessToken, projectCreateInfo))
    }

    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ): Result<Boolean> {
        return Result(projectPermissionService.verifyUserProjectPermission(accessToken, projectCode, userId))
    }

    override fun verifyProjectByOrganization(
        projectCode: String,
        organizationType: String,
        organizationId: Int
    ): Result<Boolean> {
        val projectInfo = projectLocalService.getByEnglishName(projectCode)
        val organizationIdString = organizationId.toString()
        return if (projectInfo != null) {
            val result = when (organizationType) {
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> projectInfo.bgId == organizationIdString
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> projectInfo.deptId == organizationIdString
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> projectInfo.centerId == organizationIdString
                else -> projectInfo.bgId == organizationIdString
            }
            Result(result)
        } else {
            Result(false)
        }
    }

    override fun createGitCIProject(gitProjectId: Long, userId: String): Result<ProjectVO> {
        return Result(projectLocalService.createGitCIProject(userId, gitProjectId))
    }
}
