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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.service.PipelineDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.archive.pojo.JFrogFileInfo
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class BkRepoPipelineDirService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoClient: BkRepoClient
) : PipelineDirService {
    override fun list(userId: String, projectId: String, path: String): List<FileInfo> {
        return list(userId, projectId, path, AuthPermission.VIEW)
    }

    override fun list(userId: String, projectId: String, path: String, authPermission: AuthPermission): List<FileInfo> {
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val jFrogFileInfoList = bkRepoClient.listFile(userId, projectId, RepoUtils.PIPELINE_REPO, normalizedPath).map {
            JFrogFileInfo(
                uri = it.fullPath.removePrefix("/"),
                size = it.size,
                lastModified = it.lastModifiedDate,
                folder = it.folder
            )
        }

        return when {
            pipelineService.isRootDir(normalizedPath) -> {
                pipelineService.getRootPathFileList(userId, projectId, normalizedPath, jFrogFileInfoList, authPermission)
            }
            pipelineService.isPipelineDir(normalizedPath) -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getPipelinePathList(projectId, normalizedPath, jFrogFileInfoList)
            }
            else -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getBuildPathList(projectId, normalizedPath, jFrogFileInfoList)
            }
        }
    }

    override fun show(userId: String, projectId: String, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail(userId, projectId, RepoUtils.PIPELINE_REPO, normalizedPath)
            ?: throw NotFoundException("文件不存在")
        return RepoUtils.toFileDetail(fileDetail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoPipelineDirService::class.java)
    }
}