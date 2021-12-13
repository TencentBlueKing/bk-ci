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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.client.bkrepo.DefaultBkRepoClient
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.util.BkRepoUtils
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_DEVOPS_PROJECT_ID
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_REPORT
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_STATIC
import com.tencent.devops.artifactory.util.BkRepoUtils.toFileDetail
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.archive.util.MimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.File
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.NotFoundException

@Service
@Suppress("TooManyFunctions", "MagicNumber")
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = "bkrepo")
class BkRepoArchiveFileServiceImpl @Autowired constructor(
    private val defaultBkRepoClient: DefaultBkRepoClient
) : ArchiveFileServiceImpl() {

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        val nodeDetail = defaultBkRepoClient.getFileDetail(userId = userId,
            projectId = projectId,
            repoName = BkRepoUtils.getRepoName(artifactoryType),
            path = path)
            ?: throw NotFoundException("file[$projectId|$artifactoryType|$path] not found")
        return nodeDetail.toFileDetail()
    }

    override fun uploadFile(
        userId: String,
        file: File,
        projectId: String?,
        filePath: String?,
        fileName: String?,
        fileType: FileTypeEnum?,
        props: Map<String, String?>?,
        fileChannelType: FileChannelTypeEnum
    ): String {
        val destPath = filePath ?: DefaultPathUtils.randomFileName()
        val metadata = mutableMapOf<String, String>()
        metadata["shaContent"] = file.inputStream().use { ShaUtils.sha1InputStream(it) }
        props?.forEach {
            metadata[it.key] = it.value!!
        }
        val repoProjectId = if (filePath == null) {
            BKREPO_DEVOPS_PROJECT_ID
        } else {
            projectId!!
        }
        val repoName = BkRepoUtils.getRepoName(fileType)
        defaultBkRepoClient.uploadLocalFile(userId, repoProjectId, repoName, destPath, file, metadata)
        return generateFileDownloadUrl(fileChannelType, "$repoProjectId/$repoName/$destPath")
    }

    override fun downloadArchiveFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String,
        response: HttpServletResponse
    ) {
        val destPath = generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        downloadFileToLocal(userId, "$projectId/${BkRepoUtils.getRepoName(fileType)}/$destPath", response)
    }

    override fun downloadFile(userId: String, filePath: String, outputStream: OutputStream) {
        logger.info("downloadFile, filePath: $filePath")
        if (filePath.contains("..")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("filePath"))
        }
        val artifactInfo = BkRepoUtils.parseArtifactoryInfo(filePath)
        with(artifactInfo) {
            defaultBkRepoClient.downloadFile(userId, projectId, repoName, artifactUri, outputStream)
        }
    }

    override fun downloadFile(
        userId: String,
        filePath: String,
        response: HttpServletResponse,
        logo: Boolean?
    ) {
        response.contentType = MimeUtil.mediaType(filePath)
        val path = if (logo == true) {
            "$BKREPO_STORE_PROJECT_ID/$REPO_NAME_STATIC/" +
                URLDecoder.decode(filePath, Charsets.UTF_8.name()).removePrefix("/")
        } else {
            filePath
        }
        downloadFile(userId, path, response.outputStream)
    }

    override fun downloadFileToLocal(userId: String, filePath: String, response: HttpServletResponse) {
        logger.info("downloadFile, filePath: $filePath")
        val artifactInfo = BkRepoUtils.parseArtifactoryInfo(filePath)
        val fileName = URLEncoder.encode(artifactInfo.fileName, "UTF-8")

        response.setHeader("Content-Type", MimeUtil.mediaType(artifactInfo.fileName))
        response.setHeader("Content-disposition", "attachment;filename=$fileName")
        response.setHeader("Cache-Control", "no-cache")
        with(artifactInfo) {
            defaultBkRepoClient.downloadFile(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                fullPath = artifactUri,
                outputStream = response.outputStream
            )
        }
    }

    override fun downloadReport(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ) {
        val filePath = "/$projectId/$REPO_NAME_REPORT/$pipelineId/$buildId/$elementId/$path"
        val response = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).response!!
        downloadFile(userId, filePath, response)
    }

    override fun searchFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Page<FileInfo> {
        val fileNameSet = mutableSetOf<String>()
        searchProps.fileNames?.forEach {
            fileNameSet.add(it)
        }

        val nodeList = defaultBkRepoClient.queryByNameAndMetadata(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(BkRepoUtils.REPO_NAME_PIPELINE, BkRepoUtils.REPO_NAME_CUSTOM),
            fileNames = listOf(),
            metadata = searchProps.props,
            page = page ?: 1,
            pageSize = pageSize ?: DEFAULT_PAGESIZE
        )
        return Page(
            count = nodeList.size.toLong(),
            page = page ?: 1,
            pageSize = pageSize ?: DEFAULT_PAGESIZE,
            totalPages = 1,
            records = nodeList.map { it.toFileInfo() }
        )
    }

    override fun generateDestPath(
        fileType: FileTypeEnum,
        projectId: String,
        customFilePath: String?,
        pipelineId: String?,
        buildId: String?
    ): String {
        val result = if (FileTypeEnum.BK_CUSTOM == fileType) {
            if (customFilePath.isNullOrBlank() || customFilePath.contains("..")) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("customFilePath"))
            }
            customFilePath.removePrefix("/")
        } else {
            if (pipelineId.isNullOrBlank() || buildId.isNullOrBlank()) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("pipelineId or buildId"))
            }
            val filePath = if (customFilePath.isNullOrBlank()) {
                ""
            } else {
                customFilePath.removePrefix("/")
            }
            "$pipelineId/$buildId/$filePath"
        }
        logger.info("generateDestPath, result: $result")
        return result
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        customFilePath: String?,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        val filePath = generateDestPath(
            fileType = artifactoryType.toFileType(),
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        return getFileDownloadUrls(
            userId = userId,
            filePath = "$projectId/${BkRepoUtils.getRepoName(artifactoryType)}/$filePath",
            artifactoryType = artifactoryType,
            fileChannelType = fileChannelType, fullUrl = fullUrl)
    }

    override fun getFileDownloadUrls(
        userId: String,
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        logger.info("getFileDownloadUrls, userId: $userId, filePath: $filePath, artifactoryType, $artifactoryType, " +
            "fileChannelType, $fileChannelType")
        if (filePath.contains("..")) {
            logger.warn("getFileDownloadUrls, path contains '..'")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val artifactoryInfo = BkRepoUtils.parseArtifactoryInfo(filePath)
        logger.debug("getFileDownloadUrls, artifactoryInfo, $artifactoryInfo")
        val repoName = BkRepoUtils.getRepoName(artifactoryType)
        val fileUrls = defaultBkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = userId,
            projectId = artifactoryInfo.projectId,
            repoNames = listOf(repoName),
            pathNamePairs = listOf(BkRepoUtils.parsePathNamePair(artifactoryInfo.artifactUri)),
            metadata = mapOf(),
            page = 1,
            pageSize = DOWNLOAD_FILE_URL_LIMIT
        ).map {
            generateFileDownloadUrl(fileChannelType, "${artifactoryInfo.projectId}/$repoName/${it.fullPath}", fullUrl)
        }
        if (fileUrls.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        return GetFileDownloadUrlsResponse(fileUrls)
    }

    override fun acrossProjectCopy(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count {
        val repoName = BkRepoUtils.getRepoName(artifactoryType)
        val fileNodes = defaultBkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            pathNamePairs = listOf(BkRepoUtils.parsePathNamePair(path)),
            page = 1,
            pageSize = ACROSS_PROJECT_COPY_LIMIT
        )

        fileNodes.forEach {
            defaultBkRepoClient.copy(
                userId = userId,
                fromProject = projectId,
                fromRepo = repoName,
                fromPath = it.fullPath,
                toProject = targetProjectId,
                toRepo = repoName,
                toPath = targetPath
            )
        }
        return Count(fileNodes.size)
    }

    override fun getReportRootUrl(projectId: String, pipelineId: String, buildId: String, taskId: String): String {
        val filePath = generateDestPath(
            fileType = FileTypeEnum.BK_REPORT,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            customFilePath = taskId
        )
        return generateFileDownloadUrl(FileChannelTypeEnum.WEB_SHOW, "$projectId/$REPO_NAME_REPORT/$filePath")
    }

    override fun validateUserDownloadFilePermission(userId: String, filePath: String): Boolean {
        logger.info("validateUserDownloadFilePermission, userId: $userId, filePath: $filePath")
        val realFilePath = URLDecoder.decode(filePath, "UTF-8")
        val realFilePathParts = realFilePath.split(fileSeparator)
        // 兼容用户路径里面带多个/的情况，先把路径里的文件类型、项目代码和流水线ID放到集合里
        var num = 0
        val dataList = mutableListOf<String>()
        realFilePathParts.forEach {
            if (num == 3) {
                return@forEach
            }
            if (it.isNotBlank()) {
                dataList.add(it)
                num++
            }
        }
        val fileType = dataList[0]
        var flag = true
        val validateFileTypeList = listOf(
            FileTypeEnum.BK_CUSTOM.fileType,
            FileTypeEnum.BK_ARCHIVE.fileType,
            FileTypeEnum.BK_REPORT.fileType
        )
        // 校验用户是否有下载流水线文件的权限
        if (validateFileTypeList.contains(fileType)) {
            flag = authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectCode = dataList[1],
                resourceCode = dataList[2],
                permission = AuthPermission.DOWNLOAD
            )
        }
        logger.info("validateUserDownloadFilePermission, result: $flag")
        return flag
    }

    override fun deleteFile(userId: String, filePath: String) {
        logger.info("deleteFile, filePath: $filePath")
        val artifactInfo = BkRepoUtils.parseArtifactoryInfo(filePath)
        with(artifactInfo) {
            defaultBkRepoClient.delete(BKREPO_DEFAULT_USER, projectId, repoName, artifactUri)
        }
    }

    companion object {
        private const val ACROSS_PROJECT_COPY_LIMIT = 1000
        private const val DOWNLOAD_FILE_URL_LIMIT = 1000
        private const val DEFAULT_PAGESIZE = 1000
        private val logger = LoggerFactory.getLogger(BkRepoArchiveFileServiceImpl::class.java)
    }
}
