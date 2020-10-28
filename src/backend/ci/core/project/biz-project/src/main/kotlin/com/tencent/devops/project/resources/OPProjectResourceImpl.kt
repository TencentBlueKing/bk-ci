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
import com.tencent.devops.project.api.op.OPProjectResource
import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectGraySetRequest
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.OpProjectService
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class OPProjectResourceImpl @Autowired constructor(
    private val opProjectService: OpProjectService,
    private val projectService: ProjectService
) : OPProjectResource {

    override fun list(userId: String): Result<List<ProjectVO>> {
        return Result(projectService.list(userId))
    }

    override fun listGrayProject(): Result<OpGrayProject> {
        return opProjectService.listGrayProject()
    }

    override fun setGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(
            data = opProjectService.setGrayProject(
                projectGraySetRequest.projectCodeList,
                projectGraySetRequest.operateFlag
            )
        )
    }

    override fun setCodeCCGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(
            data = opProjectService.setCodeCCGrayProject(
                projectGraySetRequest.projectCodeList,
                projectGraySetRequest.operateFlag
            )
        )
    }

    override fun updateProject(
        userId: String,
        accessToken: String,
        projectInfoRequest: OpProjectUpdateInfoRequest
    ): Result<Int> {
        return Result(data = opProjectService.updateProjectFromOp(userId, accessToken, projectInfoRequest))
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        request: HttpServletRequest
    ): Result<Map<String, Any?>?> {
        return opProjectService.getProjectList(
            projectName,
            englishName,
            projectType,
            isSecrecy,
            creator,
            approver,
            approvalStatus,
            offset,
            limit,
            grayFlag
        )
    }

    override fun getProjectCount(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean
    ): Result<Int> {
        return opProjectService.getProjectCount(
            projectName,
            englishName,
            projectType,
            isSecrecy,
            creator,
            approver,
            approvalStatus,
            grayFlag
        )
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        repoGrayFlag: Boolean,
        request: HttpServletRequest
    ): Result<Map<String, Any?>?> {
        return opProjectService.getProjectList(projectName = projectName, englishName = englishName, projectType = projectType, isSecrecy = isSecrecy, creator = creator, approver = approver, approvalStatus = approvalStatus, offset = offset, limit = limit, grayFlag = grayFlag, repoGrayFlag = repoGrayFlag)
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean,
        request: HttpServletRequest
    ): Result<Map<String, Any?>?> {
        return opProjectService.getProjectList(projectName = projectName, englishName = englishName, projectType = projectType, isSecrecy = isSecrecy, creator = creator, approver = approver, approvalStatus = approvalStatus, offset = offset, limit = limit, grayFlag = grayFlag, repoGrayFlag = repoGrayFlag, macosGrayFlag = macosGrayFlag)
    }

    override fun setRepoGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(data = opProjectService.setRepoGrayProject(projectGraySetRequest.projectCodeList, projectGraySetRequest.operateFlag))
    }

    override fun setRepoNotGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(data = opProjectService.setRepoNotGrayProject(projectGraySetRequest.projectCodeList, projectGraySetRequest.operateFlag))
    }

    override fun setMacOSGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(data = opProjectService.setMacOSGrayProject(projectGraySetRequest.projectCodeList, projectGraySetRequest.operateFlag))
    }

    override fun synProject(projectCode: String, isRefresh: Boolean): Result<Boolean> {
        return opProjectService.synProject(projectCode, isRefresh)
    }

    override fun synProjectInit(isRefresh: Boolean): Result<List<String>> {
        return opProjectService.synProjectInit(isRefresh)
    }
}
