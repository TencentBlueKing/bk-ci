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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.UserFileResource
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.RestResource
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response

@RestResource
class UserFileResourceImpl @Autowired constructor(private val archiveFileService: ArchiveFileService) :
    UserFileResource {

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        val fileType = when (artifactoryType) {
            ArtifactoryType.PIPELINE -> FileTypeEnum.BK_ARCHIVE
            ArtifactoryType.CUSTOM_DIR -> FileTypeEnum.BK_CUSTOM
        }
        val result = archiveFileService.getFileDownloadUrls(
            fileChannelType = FileChannelTypeEnum.WEB_DOWNLOAD, filePath = path, fileType = fileType
        )
        return if (result.isNotOk() || result.data == null || result.data!!.fileUrlList!!.isEmpty()) {
            Result(result.status, result.message ?: "")
        } else {
            Result(Url(result.data!!.fileUrlList!![0]))
        }
    }

    override fun show(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        checkParameters(userId, projectId, path)
        return Result(archiveFileService.show(userId, projectId, artifactoryType, path))
    }

    override fun uploadFile(
        userId: String,
        projectCode: String?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        return archiveFileService.uploadFile(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            projectCode = projectCode,
            fileChannelType = FileChannelTypeEnum.WEB_SHOW
        )
    }

    override fun downloadFileToLocal(userId: String, filePath: String): Response {
        val validateResult = archiveFileService.validateUserDownloadFilePermission(userId, filePath)
        if (validateResult.isNotOk()) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        return archiveFileService.downloadFile(filePath)
    }

    override fun downloadFile(userId: String, filePath: String, response: HttpServletResponse) {
        val validateResult = archiveFileService.validateUserDownloadFilePermission(userId, filePath)
        if (validateResult.isNotOk()) {
            response.writer.println(JsonUtil.toJson(validateResult))
            return
        }
        archiveFileService.downloadFile(filePath, response)
    }

    override fun downloadFileExt(userId: String, filePath: String, response: HttpServletResponse) {
        downloadFile(userId, filePath, response)
    }

    override fun searchFile(
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<Page<FileInfo>> {
        return archiveFileService.searchFileList(userId, projectCode, page, pageSize, searchProps)
    }

    private fun checkParameters(userId: String, projectId: String, path: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }
}