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

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService,
    private val projectPermissionService: ProjectPermissionService
) : ServiceProjectResource {

    override fun getProjectByUser(userName: String): Result<List<ProjectVO>> {
        return Result(projectService.getProjectByUser(userName))
    }

    // TODO: 内部版、企业版path一致，入参不一致，是否需要把该接口拆到子模块内区
    override fun verifyUserProjectPermission(projectCode: String, userId: String): Result<Boolean> {
        return Result(projectPermissionService.verifyUserProjectPermission(projectCode, userId))
    }

    override fun list(userId: String): Result<List<ProjectVO>> {
        return Result(projectService.list(userId))
    }

    override fun listByProjectCode(projectCodes: Set<String>): Result<List<ProjectVO>> {
        return Result(projectService.list(projectCodes))
    }

    override fun getNameByCode(projectCodes: String): Result<HashMap<String, String>> {
        return Result(projectService.getNameByCode(projectCodes))
    }

    override fun get(englishName: String): Result<ProjectVO?> {
        return Result(projectService.getByEnglishName(englishName))
    }

    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo): Result<String> {
        return Result(projectService.create(userId, projectCreateInfo))
    }

    override fun verifyUserProjectPermissionV2(projectCode: String, userId: String): Result<Boolean> {
        return Result(projectPermissionService.verifyUserProjectPermission(projectCode, userId))
    }

    override fun getV2(englishName: String): Result<ProjectVO?> {
        return Result(projectService.getByEnglishName(englishName))
    }

    override fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String): Result<List<ProjectVO>> {
        return Result(projectService.getProjectByGroup(userId, bgName, deptName, centerName))
    }

    override fun getPreUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectService.getOrCreatePreProject(userId, accessToken))
    }

    override fun getPreUserProjectV2(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectService.getOrCreatePreProject(userId, accessToken))
    }

    override fun getProjectEnNamesByOrganization(userId: String, bgId: Long, deptName: String?, centerName: String?): Result<List<String>> {
        return Result(
                projectService.getProjectEnNamesByOrganization(
                        userId = userId,
                        bgId = bgId,
                        deptName = deptName,
                        centerName = centerName,
                        interfaceName = "/service/projects/enNames/organization"
                )
        )    }
}
