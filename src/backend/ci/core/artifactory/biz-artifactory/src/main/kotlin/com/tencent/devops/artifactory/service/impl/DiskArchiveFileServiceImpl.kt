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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.util.MimeUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.FileCopyUtils
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response

@Service
class DiskArchiveFileServiceImpl : ArchiveFileService, ArchiveFileServiceImpl() {

    @Value("\${artifactory.archiveLocalBasePath:/data/bkee/public/ci/artifactory/}")
    private lateinit var archiveLocalBasePath: String

    private val matcher = AntPathMatcher()

    override fun uploadFileToRepo(destPath: String, file: File) {
        val targetFile = File(destPath)
        val parentFile = targetFile.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        FileCopyUtils.copy(file, targetFile)
    }

    override fun getCommonFileFolderName(): String {
        return "file"
    }

    override fun getInputStreamByFilePath(filePath: String): InputStream {
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        return FileInputStream(file)
    }

    override fun downloadFile(filePath: String, response: HttpServletResponse) {
        logger.info("downloadFile filePath is:$filePath")
        if (filePath.contains("..")) {
            // 非法路径则抛出错误提示
            val result = MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath),
                data = null
            )
            response.writer.println(JsonUtil.toJson(result))
            return
        }
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        response.contentType = MimeUtil.mediaType(filePath)
        FileCopyUtils.copy(FileInputStream(file), response.outputStream)
    }

    override fun downloadFile(filePath: String): Response {
        logger.info("downloadFile, filePath: $filePath")
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        // 如果文件不存在，提示404
        if (!file.exists()) {
            logger.warn("file not found, filePath: $filePath")
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        val fileName: String?
        try {
            fileName = URLEncoder.encode(file.name, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

        return Response
            .ok(file)
            .header("Content-Type", MimeUtil.STREAM_MIME_TYPE)
            .header("Content-disposition", "attachment;filename=" + fileName!!)
            .header("Cache-Control", "no-cache").build()
    }

    override fun getBasePath(): String {
        return if (archiveLocalBasePath.endsWith("/")) {
            archiveLocalBasePath
        } else {
            "$archiveLocalBasePath/"
        }
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        customFilePath: String?,
        fileChannelType: FileChannelTypeEnum
    ): Result<GetFileDownloadUrlsResponse?> {
        logger.info("[$buildId]|getFileDownloadUrls|fileChannelType=$fileChannelType|userId=$userId|projectId=$projectId|pipelineId=$pipelineId" +
            "|artifactoryType=$artifactoryType|customFilePath=$customFilePath")
        val fileType = if (artifactoryType == ArtifactoryType.PIPELINE) FileTypeEnum.BK_ARCHIVE else FileTypeEnum.BK_CUSTOM
        val result = generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        logger.info("generateDestPath result is:$result")
        if (result.isNotOk()) {
            return Result(result.status, result.message, null)
        }
        val filePath = result.data!!
        return getFileDownloadUrls(
            filePath = filePath,
            artifactoryType = artifactoryType,
            fileChannelType = fileChannelType
        )
    }

    override fun getFileDownloadUrls(
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum
    ): Result<GetFileDownloadUrlsResponse?> {
        logger.info("getFileDownloadUrls filePath is:$filePath,artifactoryType is:$artifactoryType")
        if (filePath.contains("..")) {
            // 非法路径则抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath),
                data = null
            )
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
            // 目录或者文件不存在则抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath),
                data = null
            )
        }
        val fileType = if (artifactoryType == ArtifactoryType.PIPELINE) FileTypeEnum.BK_ARCHIVE else FileTypeEnum.BK_CUSTOM
        val fileUrlList = traverseFolder(
            file = file,
            fileType = fileType,
            pathPattern = pathPattern,
            wildFlag = true,
            fileChannelType = fileChannelType
        )
        logger.info("getFileDownloadUrls fileUrlList is:$fileUrlList")
        return Result(fileUrlList)
    }

    private fun traverseFolder(
        file: File,
        fileType: FileTypeEnum,
        pathPattern: String,
        wildFlag: Boolean,
        fileChannelType: FileChannelTypeEnum
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
                        filePath = subFile.absolutePath
                    )
                    if (!url.isNullOrBlank()) {
                        fileUrlList.add(url!!)
                    }
                }
            }
        } else {
            val url = transformFileUrl(
                fileType = fileType,
                wildFlag = wildFlag,
                pathPattern = pathPattern,
                fileChannelType = fileChannelType,
                filePath = file.absolutePath
            )
            if (!url.isNullOrBlank()) {
                fileUrlList.add(url!!)
            }
        }
        return GetFileDownloadUrlsResponse(fileUrlList)
    }

    override fun generateFileDownloadPath(
        fileChannelType: FileChannelTypeEnum,
        commonConfig: CommonConfig,
        fileType: String,
        destPath: String
    ): String {
        logger.info("generateFileDownloadPath fileChannelType is: $fileChannelType,fileType is: $fileType,destPath is: $destPath")
        val urlPrefix = when (fileChannelType) {
            FileChannelTypeEnum.WEB_SHOW -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/ms/artifactory/api/user/artifactories/file/download"
            FileChannelTypeEnum.WEB_DOWNLOAD -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/ms/artifactory/api/user/artifactories/file/download/local"
            FileChannelTypeEnum.SERVICE -> "${HomeHostUtil.getHost(commonConfig.devopsApiGateway!!)}/ms/artifactory/api/service/artifactories/file/download"
            FileChannelTypeEnum.BUILD -> "${HomeHostUtil.getHost(commonConfig.devopsBuildGateway!!)}/ms/artifactory/api/build/artifactories/file/download"
        }
        val filePath = URLEncoder.encode("/$destPath", "UTF-8")
        return if (fileChannelType == FileChannelTypeEnum.WEB_SHOW) {
            "$urlPrefix/${URLEncoder.encode(filePath, "UTF-8")}"
        } else {
            "$urlPrefix?filePath=$filePath"
        }
    }

    override fun doAcrossProjectCopy(
        sourceParentPath: String,
        sourcePathPattern: String,
        destPath: String,
        targetProjectId: String
    ): Result<Count> {
        val sourceFile = File(sourceParentPath)
        if (!sourceFile.exists()) {
            logger.info("acrossProjectCopy source file not exist, $sourceParentPath")
            return Result(Count(0))
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
                userId = "",
                file = it,
                projectId = targetProjectId,
                filePath = "$destPath$fileSeparator${it.name}",
                fileType = FileTypeEnum.BK_CUSTOM,
                fileChannelType = FileChannelTypeEnum.BUILD
            )
        }
        return Result(Count(fileList.size))
    }

    override fun deleteFile(filePath: String): Result<Boolean> {
        FileSystemUtils.deleteRecursively(File("$archiveLocalBasePath/$filePath"))
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DiskArchiveFileServiceImpl::class.java)
    }
}
