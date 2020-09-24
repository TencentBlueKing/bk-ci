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

import com.tencent.devops.artifactory.client.JFrogServiceClient
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.util.JFrogUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
class JFrogArchiveFileServiceImpl : ArchiveFileService, ArchiveFileServiceImpl() {

    @Autowired
    lateinit var jFrogService: JFrogService

    @Autowired
    lateinit var jFrogServiceClient: JFrogServiceClient

    override fun uploadFileToRepo(destPath: String, file: File) {
        jFrogService.deploy(destPath, file.inputStream())
    }

    override fun getCommonFileFolderName(): String {
        return "bk-file"
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
        val httpResponse = getFileHttpResponse(filePath)
        FileCopyUtils.copy(httpResponse.body()!!.byteStream(), response.outputStream)
    }

    override fun getInputStreamByFilePath(filePath: String): InputStream {
        val httpResponse = getFileHttpResponse(filePath)
        return httpResponse.body()!!.byteStream()
    }

    private fun getFileHttpResponse(filePath: String): okhttp3.Response {
        // 解析文件类型
        val filePathParts = filePath.split("/")
        var fileType = ""
        loop@ for (data in filePathParts) {
            if (!data.isBlank()) {
                fileType = data
                break@loop
            }
        }
        val folderName = generateFolderName(fileType)
        val handleDestPath = filePath.removePrefix(fileSeparator)
        val finalFilePath = URLDecoder.decode(handleDestPath.substring(handleDestPath.indexOf(fileSeparator) + 1), "UTF-8")
        val fileUrl = "${HomeHostUtil.getHost(commonConfig.devopsApiGateway!!)}/artifactory/$folderName/download/service/${URLDecoder.decode(finalFilePath, "UTF-8")}"
        logger.info("getFileHttpResponse fileUrl is:$fileUrl")
        val request = Request.Builder().url(fileUrl).get().build()
        val httpResponse = OkhttpUtils.doHttp(request)
        if (!httpResponse.isSuccessful) {
            logger.error("FAIL|Download file from $fileUrl| message=${httpResponse.message()}| code=${httpResponse.code()}")
            throw RemoteServiceException(httpResponse.message())
        }
        return httpResponse
    }

    override fun downloadFile(filePath: String): Response {
        val httpResponse = getFileHttpResponse(filePath)
        val fileName: String?
        try {
            fileName = URLEncoder.encode(File(filePath).name, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }
        return Response
            .ok(httpResponse.body()!!.byteStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("Content-disposition", "attachment;filename=" + fileName!!)
            .header("Cache-Control", "no-cache").build()
    }

    override fun getBasePath(): String {
        return JFrogUtil.getRepoPath()
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
        logger.info("getFileDownloadUrls fileChannelType is:$fileChannelType")
        logger.info("getFileDownloadUrls userId is:$userId,projectId is:$projectId,pipelineId is:$pipelineId")
        logger.info("getFileDownloadUrls buildId is:$buildId,artifactoryType is:$artifactoryType,customFilePath is:$customFilePath")
        val param = ArtifactorySearchParam(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            regexPath = customFilePath ?: "",
            custom = artifactoryType == ArtifactoryType.CUSTOM_DIR,
            executeCount = 1
        )
        val fileUrlList = jFrogServiceClient.getFileDownloadUrl(param)
        logger.info("getFileDownloadUrls fileUrlList is:$fileUrlList")
        return Result(GetFileDownloadUrlsResponse(fileUrlList))
    }

    override fun getFileDownloadUrls(
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum
    ): Result<GetFileDownloadUrlsResponse?> {
        logger.info("getFileDownloadUrls filePath is:$filePath,artifactoryType is:$artifactoryType,fileChannelType is:$fileChannelType")
        val handleFilePath = filePath.replace(getBasePath(), "")
        val filePathParts = handleFilePath.split(fileSeparator)
        val dataList = mutableListOf<String>()
        val partNum = if (isCustom(artifactoryType)) 3 else 5
        var num = 0
        filePathParts.forEach {
            if (num == partNum) {
                return@forEach
            }
            if (it.isNotBlank()) {
                dataList.add(it)
                num++
            }
        }
        val projectId = dataList[1]
        val pipelineId = if (isCustom(artifactoryType)) "" else dataList[2]
        val buildId = if (isCustom(artifactoryType)) "" else dataList[3]
        val customFilePath = if (isCustom(artifactoryType)) dataList[2] else dataList[4]
        return getFileDownloadUrls(
            userId = "",
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = artifactoryType,
            customFilePath = customFilePath,
            fileChannelType = fileChannelType
        )
    }

    private fun isCustom(artifactoryType: ArtifactoryType): Boolean {
        return artifactoryType == ArtifactoryType.CUSTOM_DIR
    }

    override fun generateFileDownloadPath(
        fileChannelType: FileChannelTypeEnum,
        commonConfig: CommonConfig,
        fileType: String,
        destPath: String
    ): String {
        logger.info("generateFileDownloadPath fileChannelType is: $fileChannelType,fileType is: $fileType,destPath is: $destPath")
        val handleDestPath = destPath.removePrefix(fileSeparator)
        val filePath = URLDecoder.decode(handleDestPath.substring(handleDestPath.indexOf(fileSeparator) + 1), "UTF-8")
        val folderName = generateFolderName(fileType)
        val fileDownloadPath = when (fileChannelType) {
            FileChannelTypeEnum.WEB_SHOW -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/artifactory/$folderName/view/user/$filePath"
            FileChannelTypeEnum.WEB_DOWNLOAD -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/artifactory/$folderName/download/user/$filePath"
            FileChannelTypeEnum.SERVICE -> "${HomeHostUtil.getHost(commonConfig.devopsApiGateway!!)}/artifactory/$folderName/download/service/$filePath"
            FileChannelTypeEnum.BUILD -> "${HomeHostUtil.getHost(commonConfig.devopsBuildGateway!!)}/artifactory/$folderName/download/build/$filePath"
        }
        logger.info("generateFileDownloadPath fileDownloadPath is: $fileDownloadPath")
        return fileDownloadPath
    }

    private fun generateFolderName(fileType: String): String {
        return when (fileType) {
            FileTypeEnum.BK_REPORT.fileType -> "report"
            FileTypeEnum.BK_CUSTOM.fileType -> "custom"
            FileTypeEnum.BK_ARCHIVE.fileType -> "archive"
            BK_CI_ATOM_DIR -> "atom"
            else -> "file"
        }
    }

    override fun doAcrossProjectCopy(
        sourceParentPath: String,
        sourcePathPattern: String,
        destPath: String,
        targetProjectId: String
    ): Result<Count> {
        return Result(Count(0))
    }

    override fun deleteFile(filePath: String): Result<Boolean> {
        jFrogService.tryDelete("${getBasePath()}$filePath")
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogArchiveFileServiceImpl::class.java)
    }
}
