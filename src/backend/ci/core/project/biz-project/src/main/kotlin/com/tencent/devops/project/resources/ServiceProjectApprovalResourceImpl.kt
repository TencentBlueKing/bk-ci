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
 *
 */

package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectApprovalService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectApprovalResourceImpl @Autowired constructor(
    private val projectApprovalService: ProjectApprovalService
) : ServiceProjectApprovalResource {

    override fun get(projectId: String): Result<ProjectApprovalInfo?> {
        return Result(projectApprovalService.get(projectId = projectId))
    }

    override fun createApproved(projectId: String, applicant: String, approver: String): Result<Boolean> {
        projectApprovalService.createApproved(
            projectId = projectId,
            applicant = applicant,
            approver = approver
        )
        return Result(true)
    }

    override fun createReject(projectId: String, applicant: String, approver: String): Result<Boolean> {
        projectApprovalService.createReject(
            projectId = projectId,
            applicant = applicant,
            approver = approver
        )
        return Result(true)
    }

    override fun updateApproved(projectId: String, applicant: String, approver: String): Result<Boolean> {
        projectApprovalService.updateApproved(
            projectId = projectId,
            applicant = applicant,
            approver = approver
        )
        return Result(true)
    }

    override fun updateReject(projectId: String, applicant: String, approver: String): Result<Boolean> {
        projectApprovalService.updateReject(
            projectId = projectId,
            applicant = applicant,
            approver = approver
        )
        return Result(true)
    }

    override fun createMigration(projectId: String): Result<Boolean> {
        projectApprovalService.createMigration(projectId = projectId)
        return Result(true)
    }
}
