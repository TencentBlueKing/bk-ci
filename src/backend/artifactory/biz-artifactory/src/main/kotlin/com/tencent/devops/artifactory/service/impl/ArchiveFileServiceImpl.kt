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
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.FileDigestUtils
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.file.Files

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
            val filePath = "${getBasePath()}$fileSeparator$path"
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

    override fun uploadFile(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        projectCode: String?,
        filePath: String?,
        fileType: FileTypeEnum?,
        props: Map<String, String>?,
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
                projectCode = projectCode,
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

    override fun archiveFile(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        fileChannelType: FileChannelTypeEnum
    ): Result<String?> {
        logger.info("archiveFile userId is:$userId,projectCode is:$projectCode,pipelineId is:$pipelineId,buildId is:$buildId,fileType is:$fileType,filePath is:$customFilePath")
        val result = generateDestPath(fileType, projectCode, customFilePath, pipelineId, buildId)
        logger.info("generateDestPath result is:$result")
        if (result.isNotOk()) {
            return result
        }
        val destPath = result.data + fileSeparator + disposition.fileName
        val props = mapOf("pipelineId" to pipelineId, "buildId" to buildId)
        return uploadFile(
            userId = userId,
            projectCode = projectCode,
            inputStream = inputStream,
            disposition = disposition,
            filePath = destPath.substring(getBasePath().length + 1),
            fileType = fileType,
            props = props,
            fileChannelType = fileChannelType
        )
    }

    override fun generateDestPath(
        fileType: FileTypeEnum,
        projectCode: String,
        customFilePath: String?,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        val destPathBuilder =
            StringBuilder(getBasePath()).append(fileSeparator).append(fileType.fileType).append(fileSeparator)
                .append(projectCode).append(fileSeparator)
        if (FileTypeEnum.BK_CUSTOM == fileType) {
            if (customFilePath == null) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_NULL,
                    arrayOf("customFilePath")
                )
            }
            if (customFilePath.contains("..")) {
                // 非法路径则抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf(customFilePath),
                    null
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
        logger.info("archiveFile destPath is:$destPath")
        return Result(destPath)
    }

    abstract fun getBasePath(): String

    override fun validateUserDownloadFilePermission(userId: String, filePath: String): Result<Boolean> {
        logger.info("validateUserDownloadFilePermission userId is:$userId，filePath is:$filePath")
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
        logger.info("validateUserDownloadFilePermission realFilePathParts is:$realFilePathParts")
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
                resourceType = BkAuthResourceType.PIPELINE_DEFAULT,
                projectCode = dataList[1],
                resourceCode = dataList[2],
                permission = BkAuthPermission.DOWNLOAD
            )
        }
        logger.info("validateUserDownloadFilePermission flag is:$flag")
        return Result(flag)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveFileServiceImpl::class.java)
    }
}
