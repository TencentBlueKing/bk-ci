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

import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BKREPO_DEVOPS_PROJECT_ID
import com.tencent.devops.artifactory.constant.BKREPO_STATIC_PROJECT_ID
import com.tencent.devops.artifactory.constant.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.constant.REPO_NAME_CUSTOM
import com.tencent.devops.artifactory.constant.REPO_NAME_IMAGE
import com.tencent.devops.artifactory.constant.REPO_NAME_PIPELINE
import com.tencent.devops.artifactory.constant.REPO_NAME_REPORT
import com.tencent.devops.artifactory.constant.REPO_NAME_STATIC
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.util.BkRepoUtils
import com.tencent.devops.artifactory.util.BkRepoUtils.parseArtifactoryType
import com.tencent.devops.artifactory.util.BkRepoUtils.toFileDetail
import com.tencent.devops.artifactory.util.BkRepoUtils.toFileInfo
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoClientConfig
import com.tencent.devops.common.archive.pojo.QueryNodeInfo
import com.tencent.devops.common.archive.util.MimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.File
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.NotFoundException

@Service
@Suppress("TooManyFunctions", "MagicNumber", "ComplexMethod")
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = "bkrepo")
class BkRepoArchiveFileServiceImpl @Autowired constructor(
    private val bkRepoClientConfig: BkRepoClientConfig,
    private val bkRepoClient: BkRepoClient
) : ArchiveFileServiceImpl() {

    @Value("\${bkrepo.dockerRegistry:#{null}}")
    private val dockerRegistry: String? = null

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        val nodeDetail = bkRepoClient.getFileDetail(userId = userId,
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
        fileChannelType: FileChannelTypeEnum,
        logo: Boolean?
    ): String {
        val pathSplit = file.name.split('.')
        val destPath = filePath ?: DefaultPathUtils.randomFileName(pathSplit[pathSplit.size - 1])
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
        return if (fileType == FileTypeEnum.BK_STATIC) {
            bkRepoClient.uploadLocalFile(
                userId = userId,
                projectId = BKREPO_STATIC_PROJECT_ID,
                repoName = REPO_NAME_STATIC,
                path = destPath,
                file = file,
                gatewayFlag = false,
                bkrepoApiUrl = bkRepoClientConfig.bkRepoApiUrl,
                userName = bkRepoClientConfig.bkRepoStaticUserName,
                password = bkRepoClientConfig.bkRepoStaticPassword,
                properties = metadata
            )
            val configUrl = bkRepoClientConfig.bkRepoStaticRepoPrefixUrl
            val staticRepoPrefixUrl = MessageFormat.format(configUrl, BKREPO_STATIC_PROJECT_ID, REPO_NAME_STATIC)
            val defaultUrl = "$staticRepoPrefixUrl/$destPath?v=${System.currentTimeMillis() / 1000}"
            if (fileChannelType == FileChannelTypeEnum.WEB_SHOW) {
                "$defaultUrl&preview=true"
            } else {
                defaultUrl
            }
        } else if (logo == true) {
            bkRepoClient.uploadLocalFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_STATIC,
                path = destPath,
                file = file,
                properties = metadata
            )
            generateFileDownloadUrl(fileChannelType, destPath).plus("?logo=true")
        } else {
            bkRepoClient.uploadLocalFile(userId, repoProjectId, repoName, destPath, file, properties = metadata)
            generateFileDownloadUrl(fileChannelType, "$repoProjectId/$repoName/$destPath")
        }
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
            bkRepoClient.downloadFile(userId, projectId, repoName, artifactUri, outputStream)
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
            bkRepoClient.downloadFile(
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

        val nodeList = bkRepoClient.queryByNameAndMetadata(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(REPO_NAME_PIPELINE, REPO_NAME_CUSTOM, REPO_NAME_IMAGE),
            fileNames = listOf(),
            metadata = searchProps.props,
            page = page ?: 1,
            pageSize = pageSize ?: DEFAULT_PAGE_SIZE
        ).records
        return Page(
            count = nodeList.size.toLong(),
            page = page ?: 1,
            pageSize = pageSize ?: DEFAULT_PAGE_SIZE,
            totalPages = 1,
            records = nodeList.map { buildFileInfo(it) }
        )
    }

    private fun buildFileInfo(it: QueryNodeInfo): FileInfo {
        return if (parseArtifactoryType(it.repoName) == ArtifactoryType.IMAGE) {
            val (imageName, version) = DefaultPathUtils.getImageNameAndVersion(it.fullPath)
            val packageVersion = bkRepoClient.getPackageVersionInfo(
                userId = it.createdBy,
                projectId = it.projectId,
                repoName = it.repoName,
                packageKey = "docker://$imageName",
                version = version
            )
            with(packageVersion) {
                FileInfo(
                    name = imageName,
                    fullName = "$imageName:${basic.version}",
                    path = basic.fullPath,
                    fullPath = basic.fullPath,
                    size = basic.size,
                    modifiedTime = LocalDateTime.parse(basic.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
                        .timestamp(),
                    folder = false,
                    artifactoryType = ArtifactoryType.IMAGE,
                    properties = metadata.map { m -> Property(m["key"].toString(), m["value"].toString()) },
                    registry = dockerRegistry
                )
            }
        } else {
            FileInfo(
                name = it.name,
                fullName = it.name,
                path = "${it.projectId}/${it.repoName}${it.fullPath}",
                fullPath = it.fullPath,
                size = it.size,
                folder = it.folder,
                properties = it.metadata?.map { m -> Property(m.key, m.value.toString()) },
                modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                artifactoryType = parseArtifactoryType(it.repoName)
            )
        }
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
            projectId = projectId,
            filePath = "/$filePath",
            artifactoryType = artifactoryType,
            fileChannelType = fileChannelType, fullUrl = fullUrl)
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        logger.info("getFileDownloadUrls, userId: $userId, projectId: $projectId, filePath: $filePath, " +
            "artifactoryType, $artifactoryType, fileChannelType, $fileChannelType")
        if (filePath.contains("..")) {
            logger.warn("getFileDownloadUrls, path contains '..'")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val repoName = BkRepoUtils.getRepoName(artifactoryType)
        val fileUrls = bkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            pathNamePairs = listOf(BkRepoUtils.parsePathNamePair(filePath)),
            metadata = mapOf(),
            page = 1,
            pageSize = DOWNLOAD_FILE_URL_LIMIT
        ).records.map {
            generateFileDownloadUrl(fileChannelType, "$projectId/$repoName/${it.fullPath}", fullUrl)
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
        val fileNodes = bkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            pathNamePairs = listOf(BkRepoUtils.parsePathNamePair(path)),
            metadata = emptyMap(),
            page = 1,
            pageSize = ACROSS_PROJECT_COPY_LIMIT
        ).records

        fileNodes.forEach {
            bkRepoClient.copy(
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
            bkRepoClient.delete(BKREPO_DEFAULT_USER, projectId, repoName, artifactUri)
        }
    }

    override fun listCustomFiles(
        userId: String,
        projectId: String,
        filePath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?,
        modifiedTimeDesc: Boolean?
    ): Page<FileInfo> {
        val data = bkRepoClient.listFilePage(
            userId = userId,
            projectId = projectId,
            repoName = REPO_NAME_CUSTOM,
            path = filePath,
            includeFolders = includeFolder ?: true,
            deep = deep ?: false,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            modifiedTimeDesc = modifiedTimeDesc ?: false
        )
        val fileInfoList = data.records.map { it.toFileInfo() }
        return Page(data.pageNumber, data.pageSize, data.totalRecords, fileInfoList)
    }

    override fun copyFile(
        userId: String,
        srcProjectId: String,
        srcArtifactoryType: ArtifactoryType,
        srcFullPath: String,
        dstProjectId: String,
        dstArtifactoryType: ArtifactoryType,
        dstFullPath: String
    ) {
        val srcRepo = BkRepoUtils.getRepoName(srcArtifactoryType)
        val dstRepo = BkRepoUtils.getRepoName(dstArtifactoryType)
        bkRepoClient.copy(
            userId = userId,
            fromProject = srcProjectId,
            fromRepo = srcRepo,
            fromPath = srcFullPath,
            toProject = dstProjectId,
            toRepo = dstRepo,
            toPath = dstFullPath
        )
    }

    override fun getFileContent(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                fullPath = filePath,
                destFile = tmpFile
            )
            tmpFile.readText(Charsets.UTF_8)
        } catch (e: NotFoundException) {
            logger.warn("file[$filePath] not exists")
            ""
        } catch (e: RemoteServiceException) {
            logger.warn("download file[$filePath] error: $e")
            ""
        } finally {
            tmpFile.delete()
        }
    }

    override fun listFileNamesByPath(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): List<String> {
        var page = 1
        val fileNames = mutableListOf<String>()
        do {
            val nodeInfos = bkRepoClient.listFilePage(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                path = filePath,
                page = page,
                pageSize = DEFAULT_PAGE_SIZE,
                modifiedTimeDesc = false
            ).records
            nodeInfos.forEach { nodeInfo ->
                if (!nodeInfo.folder) {
                    fileNames.add(nodeInfo.name)
                }
            }
            page += 1
        } while (nodeInfos.size == DEFAULT_PAGE_SIZE)
        return fileNames
    }

    companion object {
        private const val ACROSS_PROJECT_COPY_LIMIT = 1000
        private const val DOWNLOAD_FILE_URL_LIMIT = 1000
        private const val DEFAULT_PAGE_SIZE = 1000
        private val logger = LoggerFactory.getLogger(BkRepoArchiveFileServiceImpl::class.java)
    }
}
