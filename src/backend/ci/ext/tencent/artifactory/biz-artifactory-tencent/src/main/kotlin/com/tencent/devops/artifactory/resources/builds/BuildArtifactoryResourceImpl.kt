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

package com.tencent.devops.artifactory.resources.builds

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.BkRepoDownloadService
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val bkRepoService: BkRepoService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val jfrogService: JfrogService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val gray: Gray
) : BuildArtifactoryResource {

    override fun getOwnFileList(userId: String, projectId: String): Result<FileInfoPage<FileInfo>> {
        return if (repoGray.isGray(projectId, redisOperation)) {
            val result = bkRepoService.getOwnFileList(userId, projectId, 0, -1)
            Result(FileInfoPage(0L, 1, -1, result.second, result.first))
        } else {
            val result = artifactoryService.getOwnFileList(userId, projectId, 0, -1)
            Result(FileInfoPage(0L, 1, -1, result.second, result.first))
        }
    }
    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<Boolean> {
        checkParam(projectId, path)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.check(projectId, artifactoryType, path))
        } else {
            Result(artifactoryService.check(projectId, artifactoryType, path))
        }
    }

    override fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        properties: Map<String, String>
    ): Result<Boolean> {
        checkParam(projectId, path)
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoService.setProperties(projectId, artifactoryType, path, properties)
        } else {
            artifactoryService.setProperties(projectId, artifactoryType, path, properties)
        }
        return Result(true)
    }

    override fun getProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParam(projectId, path)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.getProperties(projectId, artifactoryType, path))
        } else {
            Result(artifactoryService.getProperties(projectId, artifactoryType, path))
        }
    }

    override fun getPropertiesByRegex(projectId: String, pipelineId: String, buildId: String, artifactoryType: ArtifactoryType, path: String): Result<List<FileDetail>> {
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.getPropertiesByRegex(projectId, pipelineId, buildId, artifactoryType, path))
        } else {
            Result(artifactoryService.getPropertiesByRegex(projectId, pipelineId, buildId, artifactoryType, path))
        }
    }

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?
    ): Result<List<String>> {
        checkParam(projectId, path)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoDownloadService.getThirdPartyDownloadUrl(projectId, pipelineId, buildId, artifactoryType, path, ttl))
        } else {
            Result(artifactoryDownloadService.getThirdPartyDownloadUrl(projectId, pipelineId, buildId, artifactoryType, path, ttl))
        }
    }

    override fun getFileDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<String>> {
        val param = ArtifactorySearchParam(
            projectId,
            pipelineId,
            buildId,
            path,
            artifactoryType == ArtifactoryType.CUSTOM_DIR,
            1
        )
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.getFileDownloadUrl(param))
        } else {
            Result(jfrogService.getFileDownloadUrl(param))
        }
    }

    private fun checkParam(projectId: String, path: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    override fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        checkParam(projectId)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.acrossProjectCopy(projectId, artifactoryType, path, targetProjectId, targetPath))
        } else {
            Result(artifactoryService.acrossProjectCopy(projectId, artifactoryType, path, targetProjectId, targetPath))
        }
    }

    override fun checkRepoGray(projectId: String): Result<Boolean> {
        return Result(repoGray.isGray(projectId, redisOperation))
    }

    override fun checkGrayProject(projectId: String): Result<Boolean> {
        return Result(gray.isGrayProject(projectId, redisOperation))
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}