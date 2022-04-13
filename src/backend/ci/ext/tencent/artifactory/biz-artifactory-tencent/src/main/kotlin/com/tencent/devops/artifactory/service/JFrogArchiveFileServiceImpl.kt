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

import com.tencent.devops.artifactory.client.JFrogServiceClient
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.impl.ArchiveFileServiceImpl
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.util.JFrogUtil
import com.tencent.devops.common.service.utils.HomeHostUtil
import okhttp3.Request
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import javax.servlet.http.HttpServletResponse

@Service
class JFrogArchiveFileServiceImpl : ArchiveFileServiceImpl() {

    @Autowired
    lateinit var jFrogService: JFrogService

    @Autowired
    lateinit var jFrogServiceClient: JFrogServiceClient

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
        logger.info("uploadFile, userId: $userId, file: $file, projectId: $projectId, filePath: $filePath, " +
            "fileName: $fileName, fileType: $fileType, props: $props, fileChannelType: $fileChannelType")
        val uploadFileName = fileName ?: file.name
        val fileTypeStr = fileType?.fileType ?: "file"
        val destPath = if (null == filePath) {
            "${getBasePath()}${fileSeparator}file$fileSeparator$${DefaultPathUtils.randomFileName()}"
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
        logger.info("$uploadFileName destPath is:$destPath")
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
        return generateFileDownloadUrl(fileChannelType, fileType?.fileType ?: "file", path)
    }

    private fun uploadFileToRepo(destPath: String, file: File) {
        jFrogService.deploy(destPath, file.inputStream())
    }

    override fun getReportRootUrl(projectId: String, pipelineId: String, buildId: String, taskId: String): String {
        return ""
    }

    override fun downloadFileToLocal(userId: String, filePath: String, response: HttpServletResponse) {
        TODO("not implemented")
    }

    override fun downloadReport(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ) {
        TODO("not implemented")
    }

    override fun searchFileList(
        userId: String,
        projectId:
        String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Page<FileInfo> {
        TODO("not implemented")
    }

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        TODO("not implemented")
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
        val httpResponse = getFileHttpResponse(filePath)
        return httpResponse.body()!!.byteStream()
    }

    override fun downloadFile(userId: String, filePath: String, response: HttpServletResponse, logo: Boolean?) {
        logger.info("downloadFile filePath: $filePath")
        if (filePath.contains("..")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("filePath"))
        }
        val httpResponse = getFileHttpResponse(filePath)
        FileCopyUtils.copy(httpResponse.body()!!.byteStream(), response.outputStream)
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
        logger.info("downloadArchiveFile, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, " +
            "buildId: $buildId, fileType: $fileType, customFilePath: $customFilePath")
        val result = generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        val destPath = result.substring(getBasePath().length)
        downloadFile(userId, destPath, response)
    }

    private fun getFileHttpResponse(filePath: String): okhttp3.Response {
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
        val finalFilePath = URLDecoder.decode(
            handleDestPath.substring(handleDestPath.indexOf(fileSeparator) + 1), "UTF-8"
        )
        val host = HomeHostUtil.getHost(commonConfig.devopsApiGateway!!)
        val path = URLDecoder.decode(finalFilePath, "UTF-8")
        val fileUrl = "$host/artifactory/$folderName/download/service/$path"
        logger.info("getFileHttpResponse fileUrl is:$fileUrl")
        val request = Request.Builder().url(fileUrl).get().build()
        val httpResponse = OkhttpUtils.doHttp(request)
        if (!httpResponse.isSuccessful) {
            logger.error("FAIL|Download file from $fileUrl| " +
                "message=${httpResponse.message()}| code=${httpResponse.code()}")
            throw RemoteServiceException(httpResponse.message())
        }
        return httpResponse
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
        logger.info("getFileDownloadUrls userId: $userId, projectId: $projectId, pipelineId:$pipelineId, " +
            "buildId: $buildId, artifactoryType: $artifactoryType, customFilePath : $customFilePath, " +
            "fileChannelType :$fileChannelType")
        val param = ArtifactorySearchParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            regexPath = customFilePath ?: "",
            custom = artifactoryType == ArtifactoryType.CUSTOM_DIR,
            executeCount = 1
        )
        val fileUrlList = jFrogServiceClient.getFileDownloadUrl(param)
        logger.info("getFileDownloadUrls fileUrlList is:$fileUrlList")
        return GetFileDownloadUrlsResponse(fileUrlList)
    }

    override fun getFileDownloadUrls(
        userId: String,
        filePath: String,
        artifactoryType: ArtifactoryType,
        fileChannelType: FileChannelTypeEnum,
        fullUrl: Boolean
    ): GetFileDownloadUrlsResponse {
        logger.info("getFileDownloadUrls, filePath: $filePath, artifactoryType, $artifactoryType, " +
            "fileChannelType, $fileChannelType")
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
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = artifactoryType,
            customFilePath = customFilePath,
            fileChannelType = fileChannelType,
            fullUrl = fullUrl
        )
    }

    override fun generateDestPath(
        fileType: FileTypeEnum,
        projectId: String,
        customFilePath: String?,
        pipelineId: String?,
        buildId: String?
    ): String {
        val destPathBuilder = StringBuilder(getBasePath()).append(fileType.fileType).append(fileSeparator)
        if (FileTypeEnum.BK_PLUGIN_FE != fileType) {
            if (!projectId.isNullOrBlank()) {
                destPathBuilder.append(projectId).append(fileSeparator)
            }
        }

        if (FileTypeEnum.BK_CUSTOM == fileType || FileTypeEnum.BK_PLUGIN_FE == fileType) {
            if (customFilePath == null) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("customFilePath"))
            }
            if (customFilePath.contains("..")) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("customFilePath"))
            }
            destPathBuilder.append(customFilePath.removePrefix(fileSeparator)) // 自定义方式归档文件
        } else {
            destPathBuilder.append(pipelineId).append(fileSeparator).append(buildId)
            if (!customFilePath.isNullOrBlank()) {
                destPathBuilder.append(fileSeparator).append(customFilePath!!.removePrefix(fileSeparator))
            }
        }
        val destPath = destPathBuilder.toString()
        logger.info("[$buildId]|archiveFile destPath=$destPath")
        return destPath
    }

    override fun acrossProjectCopy(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count {
        return Count(0)
    }

    private fun isCustom(artifactoryType: ArtifactoryType): Boolean {
        return artifactoryType == ArtifactoryType.CUSTOM_DIR
    }

    fun generateFileDownloadUrl(
        fileChannelType: FileChannelTypeEnum,
        fileType: String,
        destPath: String
    ): String {
        logger.info("generateFileDownloadUrl, fileChannelType: $fileChannelType, destPath: $destPath")
        val handleDestPath = destPath.removePrefix(fileSeparator)
        val filePath = URLDecoder.decode(
            handleDestPath.substring(handleDestPath.indexOf(fileSeparator) + 1), "UTF-8")
        val folderName = generateFolderName(fileType)
        val fileDownloadPath = when (fileChannelType) {
            FileChannelTypeEnum.WEB_SHOW -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/" +
                "artifactory/$folderName/view/user/$filePath"
            FileChannelTypeEnum.WEB_DOWNLOAD -> "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/" +
                "artifactory/$folderName/download/user/$filePath"
            FileChannelTypeEnum.SERVICE -> "${HomeHostUtil.getHost(commonConfig.devopsApiGateway!!)}/" +
                "artifactory/$folderName/download/service/$filePath"
            FileChannelTypeEnum.BUILD -> "${HomeHostUtil.getHost(commonConfig.devopsBuildGateway!!)}/" +
                "artifactory/$folderName/download/build/$filePath"
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

    override fun deleteFile(userId: String, filePath: String) {
        jFrogService.tryDelete("${getBasePath()}$filePath")
    }

    override fun listCustomFiles(
        userId: String,
        projectId: String,
        filePath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Page<FileInfo> {
        TODO("Not yet implemented")
    }

    private fun getBasePath(): String {
        return JFrogUtil.getRepoPath()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogArchiveFileServiceImpl::class.java)
    }
}
