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

import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.FileDigestUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.AntPathMatcher
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.LocalDateTime
import javax.servlet.http.HttpServletResponse

abstract class ArchiveFileServiceImpl : ArchiveFileService {

    @Autowired
    lateinit var fileDao: FileDao

    @Autowired
    lateinit var pipelineAuthServiceCode: PipelineAuthServiceCode

    @Autowired
    lateinit var authPermissionApi: AuthPermissionApi

    @Autowired
    lateinit var commonConfig: CommonConfig

    @Autowired
    lateinit var dslContext: DSLContext

    private val matcher = AntPathMatcher()

    protected val fileSeparator: String = System.getProperty("file.separator")!!

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
                checksums = FileChecksums(
                    sha256 = "",
                    sha1 = "",
                    md5 = ""
                ),
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

    override fun getRealPath(filePath: String) = "${getBasePath()}$fileSeparator$filePath"

    override fun uploadFile(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        projectId: String?,
        filePath: String?,
        fileType: FileTypeEnum?,
        props: Map<String, String?>?,
        fileChannelType: FileChannelTypeEnum
    ): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        val result: Result<String?>
        try {
            result = uploadFile(
                userId = userId,
                projectId = projectId,
                file = file,
                filePath = filePath,
                fileName = fileName,
                fileType = fileType,
                props = props,
                fileChannelType = fileChannelType
            )
        } finally {
            file.delete()
        }
        return result
    }

    abstract fun getInputStreamByFilePath(filePath: String): InputStream

    override fun downloadFile(filePath: String, outputStream: OutputStream) {
        logger.info("downloadFile filePath is:$filePath")
        if (filePath.contains("..")) {
            // 非法路径则抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                defaultMessage = "filePath is invalid",
                params = arrayOf(filePath)
            )
        }
        val inputStream = getInputStreamByFilePath(filePath)
        FileCopyUtils.copy(inputStream, outputStream)
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
    ): Result<String?> {
        logger.info("uploadFile userId is:$userId,fileInfo:${file.name}")
        logger.info("uploadFile projectId is:$projectId,filePath:$filePath,fileName:$fileName,fileType:$fileType,props:$props")
        val uploadFileName = fileName ?: file.name
        val index = uploadFileName.lastIndexOf(".")
        val fileSuffix = uploadFileName.substring(index + 1)
        val fileTypeStr = fileType?.fileType ?: "file"
        val destPath = if (null == filePath) {
            val saveFilename = "${UUIDUtil.generate()}.$fileSuffix" // 避免文件被他人覆盖，文件名用唯一数替换
            "${getBasePath()}$fileSeparator${getCommonFileFolderName()}$fileSeparator$fileSuffix$fileSeparator$saveFilename"
        } else {
            if (filePath.startsWith(getBasePath())) "$filePath" else "${getBasePath()}$fileSeparator$filePath"
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
        return Result(generateFileDownloadPath(fileChannelType, commonConfig, fileTypeStr, path))
    }

    abstract fun getCommonFileFolderName(): String

    override fun downloadArchiveFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String,
        response: HttpServletResponse
    ) {
        logger.info("[$buildId]|downloadArchiveFile|userId=$userId,projectId=$projectId,pipelineId=$pipelineId,fileType=$fileType,customFilePath=$customFilePath")
        val result = generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = customFilePath,
            pipelineId = pipelineId,
            buildId = buildId
        )
        logger.info("[$buildId]|generateDestPath result=$result")
        if (result.isNotOk()) {
            response.writer.println(JsonUtil.toJson(result))
            return
        }
        val destPath = result.data!!.substring(getBasePath().length)
        downloadFile(destPath, response)
    }

    override fun searchFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<Page<FileInfo>> {
        logger.info("searchFileList userId=$userId,projectId=$projectId,page=$page,pageSize=$pageSize,searchProps=$searchProps")
        val props = searchProps.props
        val fileTypeList = listOf(FileTypeEnum.BK_ARCHIVE.fileType, FileTypeEnum.BK_CUSTOM.fileType)
        val fileInfoRecords = fileDao.getFileListByProps(dslContext, projectId, fileTypeList, props, page, pageSize)
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
        return Result(
            data = Page(
                count = fileCount,
                page = page ?: 1,
                pageSize = pageSize ?: -1,
                totalPages = totalPages,
                records = fileInfoList
            )
        )
    }

    abstract fun uploadFileToRepo(destPath: String, file: File)

    abstract fun generateFileDownloadPath(
        fileChannelType: FileChannelTypeEnum,
        commonConfig: CommonConfig,
        fileType: String,
        destPath: String
    ): String

    override fun archiveFile(
        userId: String,
        projectId: String?,
        pipelineId: String?,
        buildId: String?,
        fileType: FileTypeEnum,
        customFilePath: String?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        fileChannelType: FileChannelTypeEnum
    ): Result<String?> {
        logger.info("[$buildId]|archiveFile userId=$userId,projectId=$projectId,pipelineId=$pipelineId,fileType=$fileType,filePath=$customFilePath")
        val result = generateDestPath(fileType, projectId, customFilePath, pipelineId, buildId)
        logger.info("[$buildId]|generateDestPath result=$result")
        if (result.isNotOk()) {
            return result
        }
        var destPath = result.data!!
        if (!destPath.endsWith(disposition.fileName)) {
            destPath = result.data + fileSeparator + disposition.fileName
        }
        val props: Map<String, String?>? = mapOf("pipelineId" to pipelineId, "buildId" to buildId)
        return uploadFile(
            userId = userId,
            projectId = projectId,
            inputStream = inputStream,
            disposition = disposition,
            filePath = destPath,
            fileType = fileType,
            props = props,
            fileChannelType = fileChannelType
        )
    }

    override fun generateDestPath(
        fileType: FileTypeEnum,
        projectId: String?,
        customFilePath: String?,
        pipelineId: String?,
        buildId: String?
    ): Result<String> {
        val destPathBuilder = StringBuilder(getBasePath()).append(fileType.fileType).append(fileSeparator)
        if (FileTypeEnum.BK_PLUGIN_FE != fileType) {
            if (!projectId.isNullOrBlank()) {
                destPathBuilder.append(projectId).append(fileSeparator)
            }
        }

        if (FileTypeEnum.BK_CUSTOM == fileType || FileTypeEnum.BK_PLUGIN_FE == fileType) {
            if (customFilePath == null) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("customFilePath")
                )
            }
            if (customFilePath.contains("..")) {
                // 非法路径则抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(customFilePath),
                    data = null
                )
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
        return Result(destPath)
    }

    override fun transformFileUrl(
        fileType: FileTypeEnum,
        wildFlag: Boolean,
        pathPattern: String,
        fileChannelType: FileChannelTypeEnum,
        filePath: String
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
            return generateFileDownloadPath(
                fileChannelType = fileChannelType,
                commonConfig = commonConfig,
                fileType = fileType.fileType,
                destPath = destPath
            )
        }
        return null
    }

    /**
     * return the archive root base path which end with / symbol
     * @return must be end with / symbol (file sperator)
     */
    abstract override fun getBasePath(): String

    override fun validateUserDownloadFilePermission(userId: String, filePath: String): Result<Boolean> {
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
        logger.info("validateUserDownloadFilePermission|userId=$userId|filePath=$filePath|realFilePathParts=$realFilePathParts")
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
        logger.info("validateUserDownloadFilePermission|flag=$flag")
        return Result(flag)
    }

    override fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        val sourcePathPattern = getSourcePath(
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        )
        logger.info("acrossProjectCopy source path pattern:$sourcePathPattern")
        val sourceParentPath = sourcePathPattern.substring(0, sourcePathPattern.lastIndexOf(fileSeparator))
        logger.info("acrossProjectCopy source parent path:$sourceParentPath")
        val destPath = getTargetPath(
            targetProjectId = targetProjectId,
            targetPath = targetPath
        )
        logger.info("acrossProjectCopy dest path:$destPath")
        return doAcrossProjectCopy(
            sourceParentPath = sourceParentPath,
            sourcePathPattern = sourcePathPattern,
            destPath = destPath,
            targetProjectId = targetProjectId
        )
    }

    private fun getSourcePath(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): String {
        val pathBuilder = StringBuilder(getBasePath())
        if (artifactoryType == ArtifactoryType.CUSTOM_DIR) {
            pathBuilder.append(FileTypeEnum.BK_CUSTOM.fileType).append(fileSeparator)
        } else {
            pathBuilder.append(FileTypeEnum.BK_ARCHIVE.fileType).append(fileSeparator)
        }
        pathBuilder.append(projectId).append(fileSeparator)
        if (path == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("path")
            )
        }
        if (path.contains("..")) {
            // 非法路径则抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(path)
            )
        }
        return pathBuilder.append(path.removePrefix("/")).toString()
    }

    private fun getTargetPath(
        targetProjectId: String,
        targetPath: String
    ): String {
        val pathBuilder = StringBuilder(getBasePath())
            .append(FileTypeEnum.BK_CUSTOM.fileType).append(fileSeparator)
            .append(targetProjectId).append(fileSeparator)
        if (targetPath == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("targetPath")
            )
        }
        if (targetPath.contains("..")) {
            // 非法路径则抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(targetPath)
            )
        }
        return pathBuilder.append(targetPath.removePrefix("/")).toString()
    }

    abstract fun doAcrossProjectCopy(
        sourceParentPath: String,
        sourcePathPattern: String,
        destPath: String,
        targetProjectId: String
    ): Result<Count>

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveFileServiceImpl::class.java)
    }
}
