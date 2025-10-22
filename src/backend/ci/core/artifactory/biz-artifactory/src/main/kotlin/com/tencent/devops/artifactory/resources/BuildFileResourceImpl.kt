/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildFileResource
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.util.PathUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream
import jakarta.servlet.http.HttpServletResponse

@RestResource@Suppress("ALL")
class BuildFileResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService,
    private val client: Client
) : BuildFileResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildFileResourceImpl::class.java)
    }

    override fun downloadFile(
        projectCode: String,
        pipelineId: String,
        filePath: String,
        response: HttpServletResponse
    ) {
        val userId = getLastModifyUser(projectCode, pipelineId)
        archiveFileService.downloadFile(userId, PathUtil.getNormalizedPath(filePath), response)
    }

    override fun archiveFile(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        val userId = getLastModifyUser(projectCode, pipelineId)
        val url = archiveFileService.archiveFile(
            userId = userId,
            projectId = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            fileType = fileType,
            customFilePath = customFilePath,
            inputStream = inputStream,
            disposition = disposition,
            fileChannelType = FileChannelTypeEnum.BUILD
        )
        return Result(url)
    }

    override fun downloadArchiveFile(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String,
        response: HttpServletResponse
    ) {
        val userId = getLastModifyUser(projectCode, pipelineId)
        return archiveFileService.downloadArchiveFile(
            userId = userId,
            projectId = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            fileType = fileType,
            customFilePath = PathUtil.getNormalizedPath(customFilePath),
            response = response
        )
    }

    override fun getFileDownloadUrls(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): Result<GetFileDownloadUrlsResponse?> {
        val userId = getLastModifyUser(projectCode, pipelineId)
        val urls = archiveFileService.getFileDownloadUrls(
            userId = userId,
            projectId = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = fileType.toArtifactoryType(),
            customFilePath = customFilePath,
            fileChannelType = FileChannelTypeEnum.BUILD
        )
        return Result(urls)
    }

    private fun getLastModifyUser(projectId: String, pipelineId: String): String {
        // pref:流水线相关的文件操作人调整为流水线的权限代持人 #11016
        return try {
            client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
                projectId = projectId,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = pipelineId
            ).data
        } catch (ignored: Exception) {
            logger.info("get pipeline oauth user fail", ignored)
            null
        }?.handoverFrom ?: client.get(ServicePipelineResource::class)
            .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
    }
}
