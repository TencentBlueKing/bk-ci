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

package com.tencent.devops.artifactory.resources.builds

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildArtifactoryResourceImpl @Autowired constructor(
    private val bkRepoService: BkRepoService,
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val shortUrlService: ShortUrlService,
    private val client: Client
) : BuildArtifactoryResource {

    override fun getOwnFileList(userId: String, projectId: String): Result<FileInfoPage<FileInfo>> {
        val result = bkRepoService.getOwnFileList(userId, projectId, 0, -1)
        return Result(FileInfoPage(0L, 1, -1, result.second, result.first))
    }

    override fun check(
        pipelineId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Boolean> {
        checkParam(projectId, path)
        val userId = getLastModifyUser(projectId, pipelineId)
        return Result(bkRepoService.check(userId, projectId, artifactoryType, path))
    }

    override fun setProperties(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        properties: Map<String, String>
    ): Result<Boolean> {
        checkParam(projectId, path)
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoService.setProperties(userId, projectId, artifactoryType, path, properties)
        return Result(true)
    }

    override fun getProperties(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParam(projectId, path)
        val userId = getLastModifyUser(projectId, pipelineId)
        return Result(bkRepoService.getProperties(userId, projectId, artifactoryType, path))
    }

    override fun getPropertiesByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?
    ): Result<List<FileDetail>> {
        return Result(
            bkRepoService.getPropertiesByRegex(
                projectId,
                pipelineId,
                buildId,
                artifactoryType,
                path,
                crossProjectId,
                crossPipineId,
                crossBuildNo
            )
        )
    }

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?,
        region: String?
    ): Result<List<String>> {
        checkParam(projectId, path)
        return Result(
            bkRepoDownloadService.getThirdPartyDownloadUrl(
                projectId,
                pipelineId,
                buildId,
                artifactoryType,
                path,
                ttl,
                crossProjectId,
                crossPipineId,
                crossBuildNo,
                region
            )
        )
    }

    override fun getFileDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<String>> {
        val userId = getLastModifyUser(projectId, pipelineId)
        val param = ArtifactorySearchParam(
            userId,
            projectId,
            pipelineId,
            buildId,
            path,
            artifactoryType == ArtifactoryType.CUSTOM_DIR,
            1
        )
        return Result(bkRepoService.getFileDownloadUrl(param))
    }

    private fun checkParam(projectId: String, path: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }

    override fun acrossProjectCopy(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        checkParam(projectId)
        val userId = getLastModifyUser(projectId, pipelineId)
        return Result(
            bkRepoService.acrossProjectCopy(userId, projectId, artifactoryType, path, targetProjectId, targetPath)
        )
    }

    override fun checkRepoGray(projectId: String): Result<Boolean> {
        return Result(true)
    }

    override fun checkGrayProject(projectId: String): Result<Boolean> {
        val gray = client.get(ServiceProjectResource::class).get(projectId).data?.gray ?: false
        return Result(gray)
    }

    override fun externalUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        val shortUrl = shortUrlService.createShortUrl(
            url = PathUtils.buildDetailLink(
                projectId = projectId,
                artifactoryType = artifactoryType.name,
                path = path
            ),
            ttl = 24 * 3600 * 30
        )
        return Result(Url(shortUrl))
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun getLastModifyUser(projectId: String, pipelineId: String): String {
        return client.get(ServicePipelineResource::class)
            .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
    }
}
