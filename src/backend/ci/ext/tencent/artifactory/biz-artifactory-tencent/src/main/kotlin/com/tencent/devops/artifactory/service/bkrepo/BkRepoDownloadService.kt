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

import com.tencent.devops.artifactory.pojo.DownloadUrl
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.RepoDownloadService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class BkRepoDownloadService @Autowired constructor(
    private val bkRepoService: BkRepoService
) : RepoDownloadService {
    private val regex = Pattern.compile(",|;")

    @Value("\${bkrepo.devnetGatewayUrl:#{null}}")
    private val DEVNET_GATEWAY_URL: String? = null

    @Value("\${bkrepo.idcGatewayUrl:#{null}}")
    private val IDC_GATEWAY_URL: String? = null

    override fun getDownloadUrl(token: String): DownloadUrl {
        // 不支持
        throw OperationException("not support")
    }

    override fun serviceGetExternalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info("serviceGetExternalDownloadUrl, userId: $userId, projectId: $projectId, " +
            "artifactoryType: $artifactoryType, path: $path, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val url = bkRepoService.externalDownloadUrl(
            userId,
            projectId,
            artifactoryType,
            normalizedPath,
            ttl,
            directed
        )
        return Url(url)
    }

    override fun serviceGetInnerDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info("serviceGetInnerDownloadUrl, userId: $userId, projectId: $projectId, " +
            "artifactoryType: $artifactoryType, path: $path, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val url = bkRepoService.internalDownloadUrl(userId, projectId, artifactoryType, normalizedPath, ttl, directed)
        return Url(url)
    }

    override fun getDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        logger.info("getDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path")
        // 校验用户流水线权限？
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val repo = RepoUtils.getRepoByType(artifactoryType)
        return Url(
            "$DEVNET_GATEWAY_URL/bkrepo/api/user/generic/$projectId/$repo$normalizedPath",
            "$IDC_GATEWAY_URL/bkrepo/api/user/generic/$projectId/$repo$normalizedPath"
        )
    }

    // 可能已废弃，待检查
    override fun getIoaUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        logger.info("getIoaUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)

        // todo
        throw OperationException("not implemented")
    }

    override fun getExternalUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        logger.info("getExternalUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)

        // todo
        throw OperationException("not implemented")
    }

    override fun shareUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        downloadUsers: String
    ) {
        logger.info("shareUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path, ttl: $ttl, downloadUsers: $downloadUsers")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)

        // todo
        throw OperationException("not implemented")
    }

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?
    ): List<String> {
        logger.info("getThirdPartyDownloadUrl, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, " +
            "artifactoryType: $artifactoryType, path: $path, ttl: $ttl")

        // todo
        throw OperationException("not implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}