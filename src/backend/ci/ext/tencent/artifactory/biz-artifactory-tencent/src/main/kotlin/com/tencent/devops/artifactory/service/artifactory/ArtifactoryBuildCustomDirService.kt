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

import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.BuildCustomDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryBuildCustomDirService @Autowired constructor(
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val pipelineService: PipelineService,
    private val jFrogService: JFrogService
) : BuildCustomDirService {
    override fun list(projectId: String, path: String): List<FileInfo> {
        logger.info("list, projectId: $projectId, path: $path")
        val normalizePath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalizePath)) {
            logger.error("Path $normalizePath is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getCustomDirPath(projectId, normalizePath)
        val jFrogFileInfoList = jFrogService.list(realPath, false, 1)

        val fileInfoList = jFrogFileInfoList.map {
            val name = it.uri.removePrefix("/")
            val fullPath = JFrogUtil.compose(normalizePath, name, it.folder)
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

    override fun show(projectId: String, path: String): FileDetail {
        logger.info("show, projectId: $projectId, path: $path")
        val normalizePath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalizePath)) {
            logger.error("Path $normalizePath is not valid")
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
                JFrogUtil.getFileName(normalizePath),
                normalizePath,
                normalizePath,
                normalizePath,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums("", "", ""),
                jFrogPropertiesMap
            )
        } else {
            FileDetail(
                JFrogUtil.getFileName(normalizePath),
                normalizePath,
                normalizePath,
                normalizePath,
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

    override fun mkdir(projectId: String, path: String) {
        logger.info("mkdir, projectId: $projectId, path: $path")
        val normalizedPath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalizedPath)) {
            logger.error("Path $normalizedPath is not valid")
            throw BadRequestException("非法路径")
        }

        val name = JFrogUtil.getFileName(normalizedPath)
        val folderPath = JFrogUtil.getCustomDirPath(projectId, normalizedPath)
        if (jFrogService.exist(folderPath)) {
            val detail = jFrogService.file(folderPath)
            if (detail.checksums != null) {
                logger.error("Destination path $normalizedPath has same name file")
                throw BadRequestException("文件($name)已存在同名文件")
            } else {
                logger.error("Destination path $normalizedPath has same name folder")
                throw BadRequestException("文件($name)已存在同名文件夹")
            }
        }

        jFrogService.mkdir(folderPath)
    }

    override fun rename(projectId: String, fromPath: String, toPath: String) {
        logger.info("rename, projectId: $projectId, fromPath: $fromPath, toPath: $toPath")
        val srcPath = JFrogUtil.normalize(fromPath)
        val destPath = JFrogUtil.normalize(toPath)
        if (!JFrogUtil.isValid(srcPath) || !JFrogUtil.isValid(destPath)) {
            logger.error("srcPath[$srcPath] or destPath[$destPath] invalid")
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

    override fun copy(projectId: String, combinationPath: CombinationPath) {
        logger.info("copy, projectId: $projectId, combinationPath: $combinationPath")
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

    override fun move(projectId: String, combinationPath: CombinationPath) {
        logger.info("move, projectId: $projectId, combinationPath: $combinationPath")
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

    override fun delete(projectId: String, pathList: PathList) {
        logger.info("delete, projectId: $projectId, pathList: $pathList")
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