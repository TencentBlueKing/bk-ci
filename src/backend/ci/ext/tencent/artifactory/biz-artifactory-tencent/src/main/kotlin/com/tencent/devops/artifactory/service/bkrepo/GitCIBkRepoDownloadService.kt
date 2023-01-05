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

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class GitCIBkRepoDownloadService @Autowired constructor(
    private val pipelineService: PipelineService,
    bkRepoService: BkRepoService,
    client: Client,
    bkRepoClient: BkRepoClient,
    commonConfig: CommonConfig,
    shortUrlService: ShortUrlService
) : BkRepoDownloadService(
    pipelineService = pipelineService,
    bkRepoService = bkRepoService,
    client = client,
    bkRepoClient = bkRepoClient,
    commonConfig = commonConfig,
    shortUrlService = shortUrlService
) {

    @Value("\${gitci.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    override fun getDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        channelCode: ChannelCode?,
        fullUrl: Boolean
    ): Url {
        logger.info("getDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "argPath: $argPath")
        pipelineService.validatePermission(userId, projectId)
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val repo = RepoUtils.getRepoByType(artifactoryType)
        val url = "$v2GitUrl/bkrepo/api/user/generic/$projectId/$repo$normalizedPath?download=true"
        return Url(url, url)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBkRepoDownloadService::class.java)
    }
}
