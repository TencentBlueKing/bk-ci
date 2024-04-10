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

package com.tencent.devops.stream.v1.resources

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.stream.api.service.v1.GitCIDetailResource
import com.tencent.devops.stream.v1.pojo.V1GitCIModelDetail
import com.tencent.devops.stream.v1.service.V1GitCIDetailService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GitCIDetailResourceImpl @Autowired constructor(
    private val gitCIDetailService: V1GitCIDetailService
) : GitCIDetailResource {

    override fun getLatestBuildDetail(
        userId: String,
        gitProjectId: Long,
        pipelineId: String?,
        buildId: String?
    ): Result<V1GitCIModelDetail?> {
        checkParam(userId)
        return if (!buildId.isNullOrBlank()) {
            Result(gitCIDetailService.getBuildDetail(userId, gitProjectId, buildId))
        } else {
            Result(gitCIDetailService.getProjectLatestBuildDetail(userId, gitProjectId, pipelineId))
        }
    }

    override fun search(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(userId)
        return Result(
            gitCIDetailService.search(
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
        gitProjectId: Long,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParam(userId)
        return Result(
            gitCIDetailService.downloadUrl(
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
        gitProjectId: Long,
        pipelineId: String,
        buildId: String
    ): Result<List<Report>> {
        checkParam(userId)

        return Result(gitCIDetailService.getReports(userId, gitProjectId, pipelineId, buildId))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
