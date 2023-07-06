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

package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPProjectResource
import com.tencent.devops.project.pojo.OpProjectGraySetRequest
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.SystemEnums
import com.tencent.devops.project.service.OpProjectService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.ProjectTagService
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@Suppress("ALL")
@RestResource
class OPProjectResourceImpl @Autowired constructor(
    private val opProjectService: OpProjectService,
    private val projectService: ProjectService,
    private val projectTagService: ProjectTagService
) : OPProjectResource {

    override fun list(userId: String): Result<List<ProjectVO>> {
        return Result(projectService.list(userId))
    }

    override fun setGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(
            data = projectTagService.setGrayExt(
                projectCodeList = projectGraySetRequest.projectCodeList,
                operateFlag = projectGraySetRequest.operateFlag,
                system = SystemEnums.CI
            )
        )
    }

    override fun setCodeCCGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(
            data = projectTagService.setGrayExt(
                projectCodeList = projectGraySetRequest.projectCodeList,
                operateFlag = projectGraySetRequest.operateFlag,
                system = SystemEnums.CODECC
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

    override fun updateProjectCreator(projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>): Result<Boolean> {
        return Result(
            projectService.updateProjectCreator(projectUpdateCreatorDtoList = projectUpdateCreatorDtoList)
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
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        request: HttpServletRequest
    ): Result<Map<String, Any?>?> {
        return projectTagService.getProjectListByFlag(
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            offset = offset,
            limit = limit,
            grayFlag = grayFlag,
            codeCCGrayFlag = codeCCGrayFlag,
            repoGrayFlag = repoGrayFlag
        )
    }

    override fun synProject(projectCode: String, isRefresh: Boolean): Result<Boolean> {
        return opProjectService.synProject(projectCode, isRefresh)
    }

    override fun synProjectInit(isRefresh: Boolean): Result<List<String>> {
        return opProjectService.synProjectInit(isRefresh)
    }

    override fun setProjectProperties(
        userId: String,
        projectCode: String,
        properties: ProjectProperties
    ): Result<Boolean> {
        return Result(opProjectService.updateProjectProperties(userId, projectCode, properties))
    }
}
