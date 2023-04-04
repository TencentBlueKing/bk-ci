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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.FILE_NOT_EXIST
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.NotFoundException

@Service
class BkRepoPipelineDirService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoClient: BkRepoClient
) : PipelineDirService {
    override fun list(userId: String, projectId: String, path: String, authPermission: AuthPermission): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val isRootDir = pipelineService.isRootDir(normalizedPath)
        val isPipelineDir = pipelineService.isPipelineDir(normalizedPath)
        val pageSize = when {
            isRootDir -> 500      // pipeline
            isPipelineDir -> 200  // build
            else -> 5000          // others
        }
        val fileList = bkRepoClient.listFileByQuery(
            userId = userId,
            projectId = projectId,
            repoName = RepoUtils.PIPELINE_REPO,
            path = normalizedPath,
            includeFolders = true,
            page = 1,
            pageSize = pageSize
        ).records.map {
            it.toFileInfo()
        }

        return when {
            isRootDir -> {
                getRootPathFileList(userId, projectId, normalizedPath, fileList)
            }
            isPipelineDir -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission,
                        MessageUtil.getMessageByLocale(
                            messageCode = USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                            language = I18nUtil.getLanguage(userId),
                            params = arrayOf(userId, projectId, authPermission.alias)
                        )
                   )
                getPipelinePathList(projectId, normalizedPath, fileList)
            }
            else -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission,
                        MessageUtil.getMessageByLocale(
                            messageCode = USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                            language = I18nUtil.getLanguage(userId),
                            params = arrayOf(userId, projectId, authPermission.alias)
                        ))
                getBuildPathList(projectId, normalizedPath, fileList)
            }
        }
    }

    fun getRootPathFileList(userId: String, projectId: String, path: String, fileList: List<com.tencent.bkrepo.generic.pojo.FileInfo>): List<FileInfo> {
        logger.info("getRootPathFileList: userId: $userId, projectId: $projectId, path: $path, fileList: $fileList")
        val hasPermissionList = pipelineService.filterPipeline(userId, projectId)
        val pipelineIdToNameMap = pipelineService.getPipelineNames(projectId, hasPermissionList.toSet())

        val fileInfoList = mutableListOf<FileInfo>()
        fileList.forEach {
            val fullPath = it.fullPath
            val pipelineId = pipelineService.getPipelineId(fullPath)
            if (pipelineIdToNameMap.containsKey(pipelineId)) {
                val pipelineName = pipelineIdToNameMap[pipelineId]!!
                val fullName = pipelineService.getFullName(fullPath, pipelineId, pipelineName)
                fileInfoList.add(
                    FileInfo(
                        name = pipelineName,
                        fullName = fullName,
                        path = it.path,
                        fullPath = fullPath,
                        size = it.size,
                        folder = it.folder,
                        modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                        artifactoryType = ArtifactoryType.PIPELINE
                    )
                )
            }
        }
        return PathUtils.sort(fileInfoList)
    }

    fun getPipelinePathList(projectId: String, path: String, fileList: List<com.tencent.bkrepo.generic.pojo.FileInfo>): List<FileInfo> {
        logger.info("getPipelinePathList: projectId: $projectId, path: $path, fileList: $fileList")
        val pipelineId = pipelineService.getPipelineId(path)
        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
        val buildIdList = fileList.map { pipelineService.getBuildId(it.fullPath) }
        val buildIdToNameMap = pipelineService.getBuildNames(projectId, buildIdList.toSet())

        val fileInfoList = mutableListOf<FileInfo>()
        fileList.forEach {
            val fullPath = it.fullPath
            val buildId = pipelineService.getBuildId(fullPath)
            if (buildIdToNameMap.containsKey(buildId)) {
                val buildName = buildIdToNameMap[buildId]!!
                val fullName = pipelineService.getFullName(fullPath, pipelineId, pipelineName, buildId, buildName)
                fileInfoList.add(
                    FileInfo(
                        name = buildName,
                        fullName = fullName,
                        path = it.path,
                        fullPath = fullPath,
                        size = it.size,
                        folder = it.folder,
                        modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                        artifactoryType = ArtifactoryType.PIPELINE
                    )
                )
            }
        }
        return PathUtils.sort(fileInfoList)
    }

    fun getBuildPathList(projectId: String, path: String, fileList: List<com.tencent.bkrepo.generic.pojo.FileInfo>): List<FileInfo> {
        logger.info("getBuildPathList: projectId: $projectId, path: $path, fileList: $fileList")
        val pipelineId = pipelineService.getPipelineId(path)
        val buildId = pipelineService.getBuildId(path)
        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
        val buildName = pipelineService.getBuildName(projectId, buildId)

        val fileInfoList = fileList.map {
            val fullPath = it.fullPath
            val fullName = pipelineService.getFullName(fullPath, pipelineId, pipelineName, buildId, buildName)
            FileInfo(
                name = it.name,
                fullName = fullName,
                path = it.path,
                fullPath = fullPath,
                size = it.size,
                folder = it.folder,
                modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                artifactoryType = ArtifactoryType.PIPELINE
            )
        }
        return PathUtils.sort(fileInfoList)
    }

    override fun show(userId: String, projectId: String, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail(userId, projectId, RepoUtils.PIPELINE_REPO, normalizedPath)
            ?: throw NotFoundException( MessageUtil.getMessageByLocale(
                    messageCode = FILE_NOT_EXIST,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        return RepoUtils.toFileDetail(fileDetail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoPipelineDirService::class.java)
    }
}
