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

import com.tencent.devops.artifactory.api.builds.BuildCustomDirResource
import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.PathPair
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryBuildCustomDirService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoBuildCustomDirService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildCustomDirResourceImpl @Autowired constructor(
    private val artifactoryBuildCustomDirService: ArtifactoryBuildCustomDirService,
    private val bkRepoBuildCustomDirService: BkRepoBuildCustomDirService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : BuildCustomDirResource {
    override fun list(projectId: String, path: String): List<FileInfo> {
        if (path.contains(".")) {
            throw RuntimeException("please confirm the param is directory...")
        }

        return if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.list(projectId, path)
        } else {
            artifactoryBuildCustomDirService.list(projectId, path)
        }
    }

    override fun mkdir(projectId: String, path: String): Result<Boolean> {
        logger.info("mkdir, projectId: $projectId")
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.mkdir(projectId, path)
        } else {
            artifactoryBuildCustomDirService.mkdir(projectId, path)
        }
        return Result(true)
    }

    override fun rename(projectId: String, pathPair: PathPair): Result<Boolean> {
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.rename(projectId, pathPair.srcPath, pathPair.destPath)
        } else {
            artifactoryBuildCustomDirService.rename(projectId, pathPair.srcPath, pathPair.destPath)
        }
        return Result(true)
    }

    override fun copy(projectId: String, combinationPath: CombinationPath): Result<Boolean> {
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.copy(projectId, combinationPath)
        } else {
            artifactoryBuildCustomDirService.copy(projectId, combinationPath)
        }
        return Result(true)
    }

    override fun move(projectId: String, combinationPath: CombinationPath): Result<Boolean> {
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.move(projectId, combinationPath)
        } else {
            artifactoryBuildCustomDirService.move(projectId, combinationPath)
        }
        return Result(true)
    }

    override fun delete(projectId: String, pathList: PathList): Result<Boolean> {
        if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoBuildCustomDirService.delete(projectId, pathList)
        } else {
            artifactoryBuildCustomDirService.delete(projectId, pathList)
        }
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}