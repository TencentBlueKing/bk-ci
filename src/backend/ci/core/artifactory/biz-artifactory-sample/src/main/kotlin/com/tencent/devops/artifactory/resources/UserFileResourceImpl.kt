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

package com.tencent.devops.artifactory.resources

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.devops.artifactory.api.user.UserFileResource
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode
import com.tencent.devops.artifactory.pojo.CopyFileRequest
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream
import javax.servlet.http.HttpServletResponse

@Suppress("ThrowsCount")
@RestResource
class UserFileResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService
) : UserFileResource {

    private fun checkParam(userId: String, projectId: String, path: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ErrorCodeException(errorCode = ArtifactoryMessageCode.INVALID_CUSTOM_ARTIFACTORY_PATH)
        }
    }

    override fun uploadToPath(
        userId: String,
        projectId: String,
        path: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        checkParam(userId, projectId, path)
        val url = archiveFileService.uploadFile(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            projectId = projectId,
            filePath = path,
            fileType = FileTypeEnum.BK_CUSTOM,
            fileChannelType = FileChannelTypeEnum.WEB_SHOW
        )
        return Result(url)
    }

    override fun downloadFileToLocal(userId: String, filePath: String, response: HttpServletResponse) {
        val validateResult = archiveFileService.validateUserDownloadFilePermission(userId, filePath)
        if (!validateResult) {
            throw PermissionForbiddenException("no permission")
        }
        return archiveFileService.downloadFileToLocal(userId, filePath, response)
    }

    override fun downloadFile(userId: String, filePath: String, logo: Boolean?, response: HttpServletResponse) {
        val validateResult = archiveFileService.validateUserDownloadFilePermission(userId, filePath)
        if (!validateResult) {
            throw PermissionForbiddenException("no permission")
        }
        archiveFileService.downloadFile(userId, filePath, response, logo)
    }

    override fun downloadFileExt(userId: String, filePath: String, logo: Boolean?, response: HttpServletResponse) {
        downloadFile(userId, filePath, logo, response)
    }

    override fun copy(userId: String, copyFileRequest: CopyFileRequest): Result<Boolean> {
        with(copyFileRequest) {
            if (dstArtifactoryType != ArtifactoryType.CUSTOM_DIR) {
                throw IllegalArgumentException("invalid dstArtifactoryType")
            }
            srcFileFullPaths.forEach {
                val filename = PathUtils.resolveName(it)
                val dstFullPath = PathUtils.combineFullPath(dstDirFullPath, filename)
                archiveFileService.copyFile(
                    userId = userId,
                    srcProjectId = projectId,
                    srcArtifactoryType = srcArtifactoryType,
                    srcFullPath = it,
                    dstProjectId = projectId,
                    dstArtifactoryType = dstArtifactoryType,
                    dstFullPath = dstFullPath
                )
            }
        }
        return Result(true)
    }
}
