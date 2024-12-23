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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.stream.api.user.UserStreamDetailResource
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamModelDetail
import com.tencent.devops.stream.service.StreamDetailService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamDetailResourceImpl @Autowired constructor(
    private val streamDetailService: StreamDetailService,
    private val permissionService: StreamPermissionService
) : UserStreamDetailResource {

    override fun getLatestBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String?
    ): Result<StreamModelDetail?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return if (!buildId.isNullOrBlank()) {
            Result(streamDetailService.getBuildDetail(userId, gitProjectId, buildId))
        } else {
            Result(streamDetailService.getProjectLatestBuildDetail(userId, gitProjectId, pipelineId))
        }
    }

    override fun buildTriggerReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        approve: Boolean
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(streamDetailService.buildTriggerReview(userId, gitProjectId, buildId, approve))
    }

    override fun search(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(
            streamDetailService.search(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = buildId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun downloadUrl(
        userId: String,
        gitUserId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(
            userId = userId,
            projectId = projectId,
            gitProjectId = gitProjectId,
            permission = AuthPermission.VIEW
        )
        return Result(
            streamDetailService.downloadUrl(
                userId = userId,
                gitUserId = gitUserId,
                gitProjectId = gitProjectId,
                artifactoryType = artifactoryType,
                path = path
            )
        )
    }

    override fun getReports(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<Report>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        try {
            permissionService.checkStreamPermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.VIEW
            )
        } catch (error: CustomException) {
            return Result(ErrorCodeEnum.NO_REPORT_AUTH.errorCode, ErrorCodeEnum.NO_REPORT_AUTH.getErrorMessage())
        }
        return Result(streamDetailService.getReports(userId, gitProjectId, pipelineId, buildId))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
