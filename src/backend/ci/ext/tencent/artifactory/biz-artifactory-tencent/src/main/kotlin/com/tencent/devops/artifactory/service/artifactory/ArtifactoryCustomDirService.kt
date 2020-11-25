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

package com.tencent.devops.artifactory.service.artifactory

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.CustomDirService
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryCustomDirService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val jFrogService: JFrogService
) : CustomDirService {
    override fun list(userId: String, projectId: String, argPath: String): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, argPath: $argPath")
        pipelineService.validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getCustomDirPath(projectId, path)
        val jFrogFileInfoList = jFrogService.list(realPath, false, 1)

        val fileInfoList = jFrogFileInfoList.map {
            val name = it.uri.removePrefix("/")
            val fullPath = JFrogUtil.compose(path, name, it.folder)
            FileInfo(
                name = name,
                fullName = fullPath,
                path = it.uri,
                fullPath = fullPath,
                size = it.size,
                folder = it.folder,
                modifiedTime = LocalDateTime.parse(it.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                artifactoryType = ArtifactoryType.CUSTOM_DIR
            )
        }
        return JFrogUtil.sort(fileInfoList)
    }

    override fun show(userId: String, projectId: String, argPath: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, argPath: $argPath")
        pipelineService.validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        // 项目目录不存在时，创建根目录
        val realPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (JFrogUtil.isRoot(path) && !jFrogService.exist(realPath)) {
            jFrogService.mkdir(realPath)
        }

        val jFrogFileInfo = jFrogService.file(realPath)
        val jFrogProperties = jFrogPropertiesApi.getProperties(realPath)
        val jFrogPropertiesMap = mutableMapOf<String, String>()
        jFrogProperties.map {
            jFrogPropertiesMap[it.key] = it.value.joinToString(",")
        }
        if (jFrogProperties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineId = jFrogProperties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
            jFrogPropertiesMap[ARCHIVE_PROPS_PIPELINE_NAME] = pipelineName
        }
        val checksums = jFrogFileInfo.checksums
        return if (checksums == null) {
            FileDetail(
                JFrogUtil.getFileName(path),
                path,
                path,
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums("", "", ""),
                jFrogPropertiesMap
            )
        } else {
            FileDetail(
                JFrogUtil.getFileName(path),
                path,
                path,
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums(
                    checksums.sha256,
                    checksums.sha1,
                    checksums.md5
                ),
                jFrogPropertiesMap
            )
        }
    }

    override fun deploy(userId: String, projectId: String, argPath: String, inputStream: InputStream, disposition: FormDataContentDisposition, fileSizeLimitInMB: Int) {
        logger.info("deploy, userId: $userId, projectId: $projectId, argPath: $argPath, fileSizeLimitInMB: $fileSizeLimitInMB")
        pipelineService.validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val folderPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (!(JFrogUtil.isRoot(path) || jFrogService.exist(folderPath))) {
            logger.error("Destination path $path doesn't exist")
            throw BadRequestException("文件夹($path)不存在")
        }

        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val relativePath = JFrogUtil.compose(path, fileName)
        val realPath = JFrogUtil.getCustomDirPath(projectId, relativePath)
        if (jFrogService.exist(realPath)) {
            val detail = jFrogService.file(realPath)
            if (detail.checksums == null) {
                logger.error("Destination path $path has same name folder")
                throw BadRequestException("文件($fileName)已存在同名文件夹")
            }
        }

        val properties = mapOf(
            "userId" to userId,
            "projectId" to projectId
        )
        jFrogService.deploy(realPath, inputStream, properties)
    }

    override fun mkdir(userId: String, projectId: String, argPath: String) {
        logger.info("mkdir, userId: $userId, projectId: $projectId, argPath: $argPath")
        pipelineService.validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val name = JFrogUtil.getFileName(path)
        val folderPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (jFrogService.exist(folderPath)) {
            val detail = jFrogService.file(folderPath)
            if (detail.checksums != null) {
                logger.error("Destination path $path has same name file")
                throw BadRequestException("文件($name)已存在同名文件")
            } else {
                logger.error("Destination path $path has same name folder")
                throw BadRequestException("文件($name)已存在同名文件夹")
            }
        }

        jFrogService.mkdir(folderPath)
    }

    override fun rename(userId: String, projectId: String, argSrcPath: String, argDestPath: String) {
        logger.info("rename, userId: $userId, projectId: $projectId, argSrcPath: $argSrcPath, argDestPath: $argDestPath")
        pipelineService.validatePermission(userId, projectId)
        val srcPath = JFrogUtil.normalize(argSrcPath)
        val destPath = JFrogUtil.normalize(argDestPath)
        if (!JFrogUtil.isValid(srcPath) || !JFrogUtil.isValid(destPath)) {
            logger.error("Path $srcPath or $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        val name = JFrogUtil.getFileName(destPath)
        val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
        val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
        if (jFrogService.exist(realDestPath)) {
            logger.error("Destination path $destPath already exist")
            throw OperationException("文件或者文件夹($name)已经存在")
        }

        jFrogService.move(realSrcPath, realDestPath)
    }

    override fun copy(userId: String, projectId: String, combinationPath: CombinationPath) {
        logger.info("copy, userId: $userId, projectId: $projectId, combinationPath: $combinationPath")
        pipelineService.validatePermission(userId, projectId)
        val destPath = JFrogUtil.normalize(combinationPath.destPath)
        if (!JFrogUtil.isValid(destPath)) {
            logger.error("Path $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        val folderPath = JFrogUtil.getCustomDirPath(projectId, destPath)
        if (!jFrogService.exist(folderPath)) {
            logger.error("Destination path $destPath doesn't exist")
            throw BadRequestException("文件夹($destPath)不存在")
        }

        combinationPath.srcPaths.map {
            val srcPath = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(srcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("非法路径")
            }

            if (JFrogUtil.getParentFolder(srcPath) == destPath) {
                logger.error("Cannot copy in same path ($srcPath, $destPath)")
                throw BadRequestException("不能在拷贝到当前目录")
            }

            val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
            if (!jFrogService.exist(realSrcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("文件($srcPath)不存在")
            }

            val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
            jFrogService.copy(realSrcPath, realDestPath)
        }
    }

    override fun move(userId: String, projectId: String, combinationPath: CombinationPath) {
        logger.info("move, userId: $userId, projectId: $projectId, combinationPath: $combinationPath")
        pipelineService.validatePermission(userId, projectId)
        val destPath = JFrogUtil.normalize(combinationPath.destPath)
        if (!JFrogUtil.isValid(destPath)) {
            logger.error("Path $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        combinationPath.srcPaths.map {
            val srcPath = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(srcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("非法路径")
            }

            if (srcPath == destPath || JFrogUtil.getParentFolder(srcPath) == destPath) {
                logger.error("Cannot move in same path ($srcPath, $destPath)")
                throw BadRequestException("不能移动到当前目录")
            }

            if (destPath.startsWith(srcPath)) {
                logger.error("Cannot move parent path to sub path ($srcPath, $destPath)")
                throw BadRequestException("不能将父目录移动到子目录")
            }

            val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
            val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
            jFrogService.move(realSrcPath, realDestPath)
        }
    }

    override fun delete(userId: String, projectId: String, pathList: PathList) {
        logger.info("delete, userId: $userId, projectId: $projectId, pathList: $pathList")
        pipelineService.validatePermission(userId, projectId)
        pathList.paths.map {
            val path = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(path)) {
                logger.error("Path $path is not valid")
                throw BadRequestException("非法路径")
            }

            val realPath = JFrogUtil.getCustomDirPath(projectId, path)
            jFrogService.delete(realPath)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryCustomDirService::class.java)
    }
}