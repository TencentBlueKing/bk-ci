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

import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.REPO_NAME_PLUGIN
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.FileDigestUtils
import com.tencent.devops.common.archive.util.MimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.FileCopyUtils
import org.springframework.util.FileSystemUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDateTime
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.NotFoundException

@Service
@Suppress("UNUSED", "TooManyFunctions", "UnusedPrivateMember", "NestedBlockDepth", "MagicNumber")
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = "local")
class DiskArchiveFileServiceImpl : ArchiveFileServiceImpl() {
    @Value("\${artifactory.archiveLocalBasePath:/data/bkee/public/ci/artifactory/}")
    private lateinit var archiveLocalBasePath: String

    private val matcher = AntPathMatcher()

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        val infoRecord = fileDao.getFileInfo(dslContext, path)
        return if (infoRecord == null) {
            FileDetail(
                name = "",
                path = path,
                fullName = "",
                fullPath = path,
                size = 0,
                createdTime = 0,
                modifiedTime = 0,
                checksums = FileChecksums(sha256 = "", sha1 = "", md5 = ""),
                meta = emptyMap()
            )
        } else {
            val filePath = getRealPath(path)
            val file = File(filePath)
            val inputFiles = arrayOf(file.absolutePath)
            return FileDetail(
                name = infoRecord.fileName,
                path = path,
                fullName = infoRecord.fileName,
                fullPath = path,
                size = file.length(),
                createdTime = infoRecord.createTime.timestamp(),
                modifiedTime = infoRecord.updateTime.timestamp(),
                checksums = FileChecksums(
                    sha256 = FileDigestUtils.fileSha256(inputFiles) ?: "",
                    sha1 = FileDigestUtils.fileSha1(inputFiles) ?: "",
                    md5 = FileDigestUtils.fileMD5(inputFiles) ?: ""
                ),
                meta = fileDao.getFileMeta(dslContext, infoRecord.id)
            )
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
        val path = generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        val destPath = path.substring(getBasePath().length)
        downloadFileToLocal(userId, destPath, response)
    }

    override fun acrossProjectCopy(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count {
        val sourcePathPattern = getSourcePath(projectId, artifactoryType, path)
        val sourceParentPath = sourcePathPattern.substring(0, sourcePathPattern.lastIndexOf(fileSeparator))
        return doAcrossProjectCopy(
            userId = userId,
            sourceParentPath = sourceParentPath,
            sourcePathPattern = sourcePathPattern,
            destPath = targetPath,
            targetProjectId = targetProjectId
        )
    }

    private fun getSourcePath(projectId: String, artifactoryType: ArtifactoryType, path: String?): String {
        val pathBuilder = StringBuilder(getBasePath())
        if (artifactoryType == ArtifactoryType.CUSTOM_DIR) {
            pathBuilder.append(FileTypeEnum.BK_CUSTOM.fileType).append(fileSeparator)
        } else {
            pathBuilder.append(FileTypeEnum.BK_ARCHIVE.fileType).append(fileSeparator)
        }
        pathBuilder.append(projectId).append(fileSeparator)
        if (path == null) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_NULL, params = arrayOf("path"))
        }
        if (path.contains("..")) {
            // 非法路径则抛出错误提示
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(path))
        }
        return pathBuilder.append(path.removePrefix("/")).toString()
    }

    fun uploadFileToRepo(destPath: String, file: File) {
        logger.info("uploadFileToRepo: destPath: $destPath, file: ${file.absolutePath}")
        val targetFile = File(destPath)
        val parentFile = targetFile.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        FileCopyUtils.copy(file, targetFile)
    }

    override fun getReportRootUrl(projectId: String, pipelineId: String, buildId: String, taskId: String): String {
        val filePath = generateDestPath(
            fileType = FileTypeEnum.BK_REPORT,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            customFilePath = taskId
        )
        return transformFileUrl(
            fileType = FileTypeEnum.BK_REPORT,
            wildFlag = false,
            pathPattern = filePath,
            fileChannelType = FileChannelTypeEnum.WEB_SHOW,
            filePath = filePath
        ) ?: ""
    }

    override fun downloadFile(userId: String, filePath: String, outputStream: OutputStream) {
        logger.info("downloadFile, filePath: $filePath")
        if (filePath.contains("..")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("filePath"))
        }
        val inputStream = getInputStreamByFilePath(filePath)
        FileCopyUtils.copy(inputStream, outputStream)
    }

    private fun getInputStreamByFilePath(filePath: String): InputStream {
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        return FileInputStream(file)
    }

    override fun searchFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Page<FileInfo> {
        val props = searchProps.props
        val fileTypeList = listOf(FileTypeEnum.BK_ARCHIVE.fileType, FileTypeEnum.BK_CUSTOM.fileType)
        val fileInfoRecords = fileDao.getFileListByProps(
            dslContext = dslContext,
            projectId = projectId,
            fileTypeList = fileTypeList,
            props = props,
            page = page,
            pageSize = pageSize
        )
        val fileCount = fileDao.getFileCountByProps(dslContext, projectId, fileTypeList, props)
        val fileInfoList = mutableListOf<FileInfo>()
        fileInfoRecords?.forEach {
            var artifactoryType = ArtifactoryType.PIPELINE
            if (it["fileType"] == FileTypeEnum.BK_CUSTOM.fileType) {
                artifactoryType = ArtifactoryType.CUSTOM_DIR
            }
            fileInfoList.add(
                FileInfo(
                    name = it["fileName"] as String,
                    fullName = it["fileName"] as String,
                    path = it["filePath"] as String,
                    fullPath = it["filePath"] as String,
                    size = it["fileSize"] as Long,
                    folder = false,
                    modifiedTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    artifactoryType = artifactoryType
                )
            )
        }
        val totalPages = PageUtil.calTotalPage(pageSize, fileCount)
        return Page(
            count = fileCount,
            page = page ?: 1,
            pageSize = pageSize ?: DEFAULT_PAGESIZE,
            totalPages = totalPages,
            records = fileInfoList
        )
    }

    private fun getRealPath(filePath: String) = "${getBasePath()}$fileSeparator$filePath"

    override fun generateDestPath(
        fileType: FileTypeEnum,
        projectId: String,
        customFilePath: String?,
        pipelineId: String?,
        buildId: String?
    ): String {
        val destPathBuilder = StringBuilder(getBasePath()).append(fileType.fileType).append(fileSeparator)
        if (projectId.isNotBlank()) {
            destPathBuilder.append(projectId).append(fileSeparator)
        }

        if (FileTypeEnum.BK_CUSTOM == fileType) {
            if (customFilePath == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("customFilePath")
                )
            }
            if (customFilePath.contains("..")) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("customFilePath")
                )
            }
            destPathBuilder.append(customFilePath.removePrefix(fileSeparator)) // 自定义方式归档文件
        } else {
            destPathBuilder.append(pipelineId).append(fileSeparator).append(buildId)
            if (!customFilePath.isNullOrBlank()) {
                destPathBuilder.append(fileSeparator).append(customFilePath.removePrefix(fileSeparator))
            }
        }
        val destPath = destPathBuilder.toString()
        logger.info("[$buildId]|archiveFile destPath=$destPath")
        return destPath
    }

    override fun downloadFile(userId: String, filePath: String, response: HttpServletResponse, logo: Boolean?) {
        logger.info("downloadFile, filePath: $filePath")
        if (filePath.contains("..")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        response.contentType = MimeUtil.mediaType(filePath)
        FileCopyUtils.copy(FileInputStream(file), response.outputStream)
    }

    override fun downloadFileToLocal(userId: String, filePath: String, response: HttpServletResponse) {
        logger.info("downloadFileToLocal, filePath: $filePath")
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        // 如果文件不存在，提示404
        if (!file.exists()) {
            logger.info("file($filePath) not found")
            throw NotFoundException("file not found")
        }
        val fileName = URLEncoder.encode(file.name, "UTF-8")

        response.setHeader("Content-Type", MimeUtil.mediaType(file.name))
        response.setHeader("Content-disposition", "attachment;filename=$fileName")
        response.setHeader("Cache-Control", "no-cache")
        FileCopyUtils.copy(file.inputStream(), response.outputStream)
    }

    override fun downloadReport(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ) {
        val filePath = "${FileTypeEnum.BK_REPORT.fileType}/$projectId/$pipelineId/$buildId/$elementId/$path"
        val response = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).response!!
        downloadFile(userId, filePath, response)
    }

    fun getBasePath(): String {
        return if (archiveLocalBasePath.endsWith("/")) {
            archiveLocalBasePath
        } else {
            "$archiveLocalBasePath/"
        }
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
        logger.info("uploadFile|filePath=$filePath|fileName=$fileName|props=$props")
        val uploadFileName = fileName ?: file.name
        val fileTypeStr = fileType?.fileType ?: "file"
        val fileTypeName = file.name.substring(file.name.indexOf(".") + 1)
        val destPath = if (null == filePath) {
            "${getBasePath()}$fileSeparator$fileTypeStr$fileSeparator$${DefaultPathUtils.randomFileName(fileTypeName)}"
        } else {
            // #5176 修正未对上传类型来决定存放路径的问题，统一在此生成归档路径，而不是由外部指定会存在内部路径泄露风险
            if (fileType != null && !projectId.isNullOrBlank()) {
                generateDestPath(
                    fileType = fileType,
                    projectId = projectId,
                    customFilePath = filePath,
                    pipelineId = props?.get("pipelineId"),
                    buildId = props?.get("buildId")
                )
            } else {
                "${getBasePath()}$fileSeparator$filePath"
            }
        }
        logger.info("uploadFile|$uploadFileName destPath is:$destPath")
        uploadFileToRepo(destPath, file)
        val shaContent = file.inputStream().use { ShaUtils.sha1InputStream(it) }
        var fileProps: Map<String, String?> = props ?: mapOf()
        fileProps = fileProps.plus("shaContent" to shaContent)
        val path = destPath.substring(getBasePath().length)
        val fileId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            fileDao.addFileInfo(
                dslContext = context,
                userId = userId,
                fileId = fileId,
                projectId = projectId,
                fileType = fileTypeStr,
                filePath = path,
                fileName = uploadFileName,
                fileSize = file.length()
            )
            if (null != props) {
                fileDao.batchAddFileProps(context, userId, fileId, fileProps)
            }
        }
        return generateFileDownloadUrl(fileChannelType, path)
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
        return getFileDownloadUrls(userId, projectId, filePath, artifactoryType, fileChannelType, fullUrl = fullUrl)
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        if (filePath.contains("..")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }

        var pathPattern: String = filePath
        // 通配符方式的路径遍历父目录下的所有文件
        val file = if (filePath.startsWith(getBasePath())) {
            File(filePath).parentFile
        } else {
            pathPattern = "${getBasePath()}/${filePath.removePrefix("/")}"
            File(getBasePath(), filePath).parentFile
        }

        if (!file.exists()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val response = traverseFolder(
            file = file,
            fileType = artifactoryType.toFileType(),
            pathPattern = pathPattern,
            wildFlag = true,
            fileChannelType = fileChannelType,
            fullUrl = fullUrl
        )
        if (response.fileUrlList.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        logger.info("getFileDownloadUrls, response: $response")
        return response
    }

    private fun traverseFolder(
        file: File,
        fileType: FileTypeEnum,
        pathPattern: String,
        wildFlag: Boolean,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        val fileUrlList = mutableListOf<String>()
        if (file.isDirectory) {
            file.listFiles()?.forEach { subFile ->
                // 考虑到文件夹下面层级太深的问题，只支持遍历当前文件夹下的文件
                if (!subFile.isDirectory) {
                    val url = transformFileUrl(
                        fileType = fileType,
                        wildFlag = wildFlag,
                        pathPattern = pathPattern,
                        fileChannelType = fileChannelType,
                        filePath = subFile.absolutePath,
                        fullUrl = fullUrl
                    )
                    if (!url.isNullOrBlank()) {
                        fileUrlList.add(url)
                    }
                }
            }
        } else {
            val url = transformFileUrl(
                fileType = fileType,
                wildFlag = wildFlag,
                pathPattern = pathPattern,
                fileChannelType = fileChannelType,
                filePath = file.absolutePath,
                fullUrl = fullUrl
            )
            if (!url.isNullOrBlank()) {
                fileUrlList.add(url)
            }
        }
        return GetFileDownloadUrlsResponse(fileUrlList)
    }

    private fun transformFileUrl(
        fileType: FileTypeEnum,
        wildFlag: Boolean,
        pathPattern: String,
        fileChannelType: FileChannelTypeEnum,
        filePath: String,
        fullUrl: Boolean = true
    ): String? {
        var flag = false
        if (wildFlag) {
            if (matcher.match(pathPattern, filePath)) {
                flag = true
            }
        } else {
            flag = true
        }
        if (flag) {
            val destPath = filePath.substring(getBasePath().length)
            return generateFileDownloadUrl(fileChannelType, destPath, fullUrl = fullUrl)
        }
        return null
    }

    private fun doAcrossProjectCopy(
        userId: String,
        sourceParentPath: String,
        sourcePathPattern: String,
        destPath: String,
        targetProjectId: String
    ): Count {
        val sourceFile = File(sourceParentPath)
        if (!sourceFile.exists()) {
            logger.info("acrossProjectCopy source file not exist, $sourceParentPath")
            return Count(0)
        }
        val fileList = mutableListOf<File>()
        if (sourceFile.isDirectory) {
            sourceFile.listFiles()?.forEach {
                if (it.isDirectory) {
                    return@forEach
                }
                if (matcher.match(sourcePathPattern, it.absolutePath)) {
                    fileList.add(it)
                }
            }
        } else {
            if (matcher.match(sourcePathPattern, sourceFile.absolutePath)) {
                fileList.add(sourceFile)
            }
        }
        fileList.forEach {
            uploadFile(
                userId = userId,
                file = it,
                projectId = targetProjectId,
                filePath = "$destPath$fileSeparator${it.name}",
                fileType = FileTypeEnum.BK_CUSTOM,
                fileChannelType = FileChannelTypeEnum.BUILD
            )
        }
        return Count(fileList.size)
    }

    override fun validateUserDownloadFilePermission(userId: String, filePath: String): Boolean {
        logger.info("validateUserDownloadFilePermission, userId: =$userId, filePath: $filePath")
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
        FileSystemUtils.deleteRecursively(File("$archiveLocalBasePath/$filePath"))
    }

    override fun listCustomFiles(
        userId: String,
        projectId: String,
        filePath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?,
        modifiedTime: Boolean?
    ): Page<FileInfo> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun getFileContent(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): String {
        if (filePath.contains("../")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val bkRepoName = if (repoName == REPO_NAME_PLUGIN) BK_CI_ATOM_DIR else repoName
        val decodeFilePath = URLDecoder.decode(filePath, Charsets.UTF_8.name())
        val file = File("$archiveLocalBasePath/$bkRepoName/$decodeFilePath")
        return if (file.exists()) file.readText((Charsets.UTF_8)) else ""
    }

    override fun listFileNamesByPath(
        userId: String,
        projectId: String,
        repoName: String,
        filePath: String
    ): List<String> {
        val bkRepoName = if (repoName == REPO_NAME_PLUGIN) BK_CI_ATOM_DIR else repoName
        val decodeFilePath = URLDecoder.decode(filePath, Charsets.UTF_8.name())
        val file = File("$archiveLocalBasePath/$bkRepoName/$decodeFilePath")
        val fileNames = mutableListOf<String>()
        file.listFiles()?.forEach { tmpFile ->
            if (tmpFile.isFile) {
                fileNames.add(file.name)
            }
        }
        return fileNames
    }

    companion object {
        private const val DEFAULT_PAGESIZE = 100
        private val logger = LoggerFactory.getLogger(DiskArchiveFileServiceImpl::class.java)
    }
}
