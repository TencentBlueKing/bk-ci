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

package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceArtifactoryDownLoadResourceImpl @Autowired constructor(
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val redisOperation: RedisOperation,
    private val commonConfig: CommonConfig,
    private val repoGray: RepoGray
) : ServiceArtifactoryDownLoadResource {

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String?,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?,
        region: String?
    ): Result<List<String>> {
        checkParam(projectId, path)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(
                bkRepoDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    artifactoryType,
                    path,
                    ttl,
                    crossProjectId,
                    crossPipineId,
                    crossBuildNo,
                    region,
                    userId
                )
            )
        } else {
            Result(
                artifactoryDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    artifactoryType,
                    path,
                    ttl,
                    crossProjectId,
                    crossPipineId,
                    crossBuildNo,
                    region,
                    userId
                )
            )
        }
    }

    override fun downloadUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId, path)
        val isDirected = directed ?: false
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoDownloadService.serviceGetInnerDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
        } else {
            Result(artifactoryDownloadService.serviceGetInnerDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
        }
    }

    override fun downloadIndexUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId, path)
        val isDirected = directed ?: false
        return if (repoGray.isGray(projectId, redisOperation)) {
            // 返回临时分享链接，仅支持额外分享一次
            val url = bkRepoDownloadService.serviceGetInternalTemporaryAccessDownloadUrls(
                userId = userId,
                projectId = projectId,
                artifactoryType = artifactoryType,
                argPathSet = setOf(path),
                ttl = ttl,
                permits = 1
            ).first()
            Result(Url(url.url, url.url2))
        } else {
            val url = artifactoryDownloadService.serviceGetInnerDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected)
            Result(Url(getIndexUrl(url.url)!!, getIndexUrl(url.url2)))
        }
    }

    private fun getIndexUrl(url: String?): String? {
        if (url == null) {
            return null
        }
        if (url.startsWith(HomeHostUtil.getHost(commonConfig.devopsHostGateway!!))) {
            return url.removePrefix(HomeHostUtil.getHost(commonConfig.devopsHostGateway!!))
        }
        return url
    }

    private fun checkParam(projectId: String, path: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }
}
