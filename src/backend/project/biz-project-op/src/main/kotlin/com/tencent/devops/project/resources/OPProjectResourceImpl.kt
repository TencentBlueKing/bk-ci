/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.OpProjectService
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class OPProjectResourceImpl @Autowired constructor(private val projectService: OpProjectService) : OPProjectResource {

    override fun listGrayProject(): Result<OpGrayProject> {
        return projectService.listGrayProject()
    }

    override fun setGrayProject(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
        return Result(data = projectService.setGrayProject(projectGraySetRequest.projectCodeList, projectGraySetRequest.operateFlag))
    }

    override fun updateProject(userId: String, accessToken: String, projectInfoRequest: OpProjectUpdateInfoRequest): Result<Int> {
        return Result(data = projectService.updateProjectFromOp(userId, accessToken, projectInfoRequest))
    }

    override fun getProjectList(projectName: String?, englishName: String?, projectType: Int?, isSecrecy: Boolean?, creator: String?, approver: String?, approvalStatus: Int?, offset: Int, limit: Int, grayFlag: Boolean, request: HttpServletRequest): Result<Map<String, Any?>?> {
        return projectService.getProjectList(projectName, englishName, projectType, isSecrecy, creator, approver, approvalStatus, offset, limit, grayFlag)
    }

    override fun getProjectCount(projectName: String?, englishName: String?, projectType: Int?, isSecrecy: Boolean?, creator: String?, approver: String?, approvalStatus: Int?, grayFlag: Boolean): Result<Int> {
        return projectService.getProjectCount(projectName, englishName, projectType, isSecrecy, creator, approver, approvalStatus, grayFlag)
    }

//    override fun updateProjectV2(userId: String, accessToken: String, projectInfoRequest: OpProjectUpdateInfoRequest): Result<Int> {
//        return Result(data = projectService.updateProjectFromOp(userId, accessToken, projectInfoRequest))
//    }
//
//    override fun setGrayProjectV2(projectGraySetRequest: OpProjectGraySetRequest): Result<Boolean> {
//        return Result(data = projectService.setGrayProject(projectGraySetRequest.projectCodeList, projectGraySetRequest.operateFlag))
//    }
}
