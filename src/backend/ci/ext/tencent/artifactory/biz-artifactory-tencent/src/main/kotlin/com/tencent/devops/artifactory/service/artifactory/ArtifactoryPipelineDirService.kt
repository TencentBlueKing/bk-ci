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

import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.PipelineDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryPipelineDirService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val jFrogService: JFrogService
) : PipelineDirService {
    override fun list(userId: String, projectId: String, path: String, authPermission: AuthPermission): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalizedPath)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getPipelinePath(projectId, normalizedPath)
        val jFrogFileInfoList = jFrogService.list(realPath, false, 1)
        return when {
            pipelineService.isRootDir(normalizedPath) -> {
                pipelineService.getRootPathFileList(userId, projectId, normalizedPath, jFrogFileInfoList)
            }
            pipelineService.isPipelineDir(normalizedPath) -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getPipelinePathList(projectId, path, jFrogFileInfoList)
            }
            else -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getBuildPathList(projectId, normalizedPath, jFrogFileInfoList)
            }
        }
    }

    override fun show(userId: String, projectId: String, argPath: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, argPath: $argPath")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        // 项目目录不存在时，创建根目录
        val realPath = JFrogUtil.getPipelinePath(projectId, path)
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
                pipelineService.getDirectoryName(projectId, path),
                path,
                pipelineService.getFullName(projectId, path),
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums("", "", ""),
                jFrogPropertiesMap
            )
        } else {
            FileDetail(
                pipelineService.getName(projectId, path),
                path,
                pipelineService.getFullName(projectId, path),
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

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryPipelineDirService::class.java)
    }
}