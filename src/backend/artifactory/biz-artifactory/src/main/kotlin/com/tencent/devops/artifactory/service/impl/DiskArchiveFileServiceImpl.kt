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

import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileInputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response

@Service
class DiskArchiveFileServiceImpl : ArchiveFileService, ArchiveFileServiceImpl() {

    @Value("\${artifactory.archiveLocalBasePath}")
    private lateinit var archiveLocalBasePath: String

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

    override fun downloadFile(filePath: String, response: HttpServletResponse) {
        logger.info("downloadFile filePath is:$filePath")
        if (filePath.contains("..")) {
            // 非法路径则抛出错误提示
            val result = MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(filePath),
                null
            )
            response.writer.println(JsonUtil.toJson(result))
            return
        }
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        FileCopyUtils.copy(FileInputStream(file), response.outputStream)
    }

    override fun downloadFile(filePath: String): Response {
        val file = File("${getBasePath()}$fileSeparator${URLDecoder.decode(filePath, "UTF-8")}")
        // 如果文件不存在，提示404
        if (!file.exists()) {
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
            .header("Content-disposition", "attachment;filename=" + fileName!!)
            .header("Cache-Control", "no-cache").build()
    }

    override fun getBasePath(): String {
        return archiveLocalBasePath
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        customFilePath: String?,
        fileChannelType: FileChannelTypeEnum
    ): Result<GetFileDownloadUrlsResponse?> {
        logger.info("getFileDownloadUrls fileChannelType is:$fileChannelType")
        logger.info("getFileDownloadUrls userId is:$userId,projectCode is:$projectCode,pipelineId is:$pipelineId")
        logger.info("getFileDownloadUrls buildId is:$buildId,artifactoryType is:$artifactoryType,customFilePath is:$customFilePath")
        val fileType = if (artifactoryType == ArtifactoryType.PIPELINE) FileTypeEnum.BK_ARCHIVE else FileTypeEnum.BK_CUSTOM
        val result = generateDestPath(fileType, projectCode, customFilePath, pipelineId, buildId)
        logger.info("generateDestPath result is:$result")
        if (result.isNotOk()) {
            return Result(result.status, result.message, null)
        }
        val filePath = result.data!!
        return getFileDownloadUrls(filePath, artifactoryType, fileChannelType)
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
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(filePath),
                null
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
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(filePath),
                null
            )
        }
        val fileType = if (artifactoryType == ArtifactoryType.PIPELINE) FileTypeEnum.BK_ARCHIVE else FileTypeEnum.BK_CUSTOM
        val fileUrlList = traverseFolder(file, fileType, pathPattern, true, fileChannelType)
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
                    val url = transformFileUrl(fileType, wildFlag, pathPattern, fileChannelType, subFile.absolutePath)
                    if (!url.isNullOrBlank()) {
                        fileUrlList.add(url!!)
                    }
                }
            }
        } else {
            val url = transformFileUrl(fileType, wildFlag, pathPattern, fileChannelType, file.absolutePath)
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
            FileChannelTypeEnum.WEB_SHOW -> "${commonConfig.devopsHostGateway}/ms/artifactory/api/user/artifactories/file/download"
            FileChannelTypeEnum.WEB_DOWNLOAD -> "${commonConfig.devopsHostGateway}/ms/artifactory/api/user/artifactories/file/download/local"
            FileChannelTypeEnum.SERVICE -> "${commonConfig.devopsApiGateway}/ms/artifactory/api/service/artifactories/file/download"
            FileChannelTypeEnum.BUILD -> "${commonConfig.devopsBuildGateway}/ms/artifactory/api/build/artifactories/file/download"
        }
        val filePath = URLEncoder.encode("/$destPath", "UTF-8")
        return if (fileChannelType == FileChannelTypeEnum.WEB_SHOW) {
            "$urlPrefix/${URLEncoder.encode(filePath, "UTF-8")}"
        } else {
            "$urlPrefix?filePath=$filePath"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DiskArchiveFileServiceImpl::class.java)
    }
}
