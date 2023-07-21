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

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.pojo.Page
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse

@Suppress("ALL")
interface ArchiveFileService {
    /**
     * 获取报告根路径
     */
    fun getReportRootUrl(projectId: String, pipelineId: String, buildId: String, taskId: String): String

    /**
     * 上传文件
     */
    fun uploadFile(
        userId: String,
        file: File,
        projectId: String? = null,
        filePath: String? = null,
        fileName: String? = null,
        fileType: FileTypeEnum? = null,
        props: Map<String, String?>? = null,
        fileChannelType: FileChannelTypeEnum,
        logo: Boolean? = false
    ): String

    /**
     * 上传文件
     */
    fun uploadFile(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        projectId: String? = null,
        filePath: String? = null,
        fileType: FileTypeEnum? = null,
        props: Map<String, String?>? = null,
        fileChannelType: FileChannelTypeEnum,
        logo: Boolean? = false
    ): String

    /**
     * 归档文件
     */
    fun archiveFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        fileChannelType: FileChannelTypeEnum
    ): String

    /**
     * 下载文件至输出流
     */
    fun downloadFile(userId: String, filePath: String, outputStream: OutputStream)

    /**
     * 下载文件
     */
    fun downloadFile(userId: String, filePath: String, response: HttpServletResponse, logo: Boolean? = false)

    /**
     * 下载文件到本地
     */
    fun downloadFileToLocal(userId: String, filePath: String, response: HttpServletResponse)

    /**
     * 下载报告文件
     */
    fun downloadReport(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    )

    /**
     * 下载归档文件
     */
    fun downloadArchiveFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String,
        response: HttpServletResponse
    )

    /**
     * 获取仓库指定路径下的文件下载路径列表
     */
    fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        customFilePath: String?,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean = true
    ): GetFileDownloadUrlsResponse

    /**
     * 获取仓库指定路径下的文件下载路径列表
     * [fullUrl]表示是否返回包含域名的全url地址
     */
    fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean = true
    ): GetFileDownloadUrlsResponse

    /**
     * 根据文件元数据查找文件列表
     */
    fun searchFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Page<FileInfo>

    /**
     * 展示文件详情
     * @param userId userId
     * @param projectId projectId
     * @param artifactoryType artifactoryType
     * @param path path
     * @return FileDetail
     */
    fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail

    /**
     * // TODO remove this method
     * 根据用户自定义路径与 fileType 来生成真正存储文件的路径
     * @param fileType 文件存储的类型
     * @param projectId 项目id 英文名称
     * @param customFilePath 自定义路径
     * @param pipelineId 流水线id
     * @param buildId 构建id
     */
    fun generateDestPath(
        fileType: FileTypeEnum,
        projectId: String,
        customFilePath: String?,
        pipelineId: String?,
        buildId: String?
    ): String

    /**
     * 校验用户是否有下载文件的权限
     * @param userId 用户Id
     * @param filePath 下载路径
     * @return Result<Boolean>
     */
    fun validateUserDownloadFilePermission(userId: String, filePath: String): Boolean

    /**
     * 跨项目拷贝文件
     */
    fun acrossProjectCopy(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count

    /**
     * 删除文件
     */
    fun deleteFile(
        userId: String,
        filePath: String
    )

    /**
     * 查询自定义仓库文件
     */
    fun listCustomFiles(
        userId: String,
        projectId: String,
        filePath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?,
        modifiedTimeDesc: Boolean?
    ): Page<FileInfo>

    /**
     * 复制文件
     */
    fun copyFile(
        userId: String,
        srcProjectId: String,
        srcArtifactoryType: ArtifactoryType,
        srcFullPath: String,
        dstProjectId: String,
        dstArtifactoryType: ArtifactoryType,
        dstFullPath: String
    )

    /**
     * 根据文件路径获取相关文件内容
     */
    fun getFileContent(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): String

    /**
     * 获取路径下的文件名称列表
     */
    fun listFileNamesByPath(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): List<String>
}
