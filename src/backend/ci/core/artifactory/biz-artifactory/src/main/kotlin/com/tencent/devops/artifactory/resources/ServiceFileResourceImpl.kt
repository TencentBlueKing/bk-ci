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

import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream
import javax.servlet.http.HttpServletResponse

@RestResource
class ServiceFileResourceImpl @Autowired constructor(private val archiveFileService: ArchiveFileService) :
    ServiceFileResource {

    override fun uploadFile(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        projectCode: String?,
        fileChannelType: FileChannelTypeEnum,
        logo: Boolean?
    ): Result<String?> {
        val url = archiveFileService.uploadFile(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            projectId = projectCode,
            fileChannelType = fileChannelType,
            logo = logo
        )
        return Result(url)
    }

    override fun downloadFile(userId: String, filePath: String, response: HttpServletResponse) {
        archiveFileService.downloadFileToLocal(userId, filePath, response)
    }

    override fun getFileContent(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): Result<String> {
        val content = archiveFileService.getFileContent(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            filePath = filePath
        )
        return Result(content)
    }

    override fun listFileNamesByPath(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): Result<List<String>> {
        val fileNames = archiveFileService.listFileNamesByPath(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            filePath = filePath
        )
        return Result(fileNames)
    }
}
