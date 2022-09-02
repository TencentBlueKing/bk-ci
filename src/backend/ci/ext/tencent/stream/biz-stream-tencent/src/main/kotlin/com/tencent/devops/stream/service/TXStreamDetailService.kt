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

package com.tencent.devops.stream.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Primary
@Service
class TXStreamDetailService @Autowired constructor(
    dslContext: DSLContext,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    gitRequestEventDao: GitRequestEventDao,
    streamBasicSettingService: StreamBasicSettingService,
    pipelineResourceDao: GitPipelineResourceDao,
    streamGitProjectInfoCache: StreamGitProjectInfoCache,
    private val client: Client,
    private val txStreamBasicSettingService: TXStreamBasicSettingService,
    private val streamGitConfig: StreamGitConfig
) : StreamDetailService(
    client = client,
    dslContext = dslContext,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    gitRequestEventDao = gitRequestEventDao,
    streamBasicSettingService = streamBasicSettingService,
    pipelineResourceDao = pipelineResourceDao,
    streamGitProjectInfoCache = streamGitProjectInfoCache,
    streamGitConfig = streamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TXStreamDetailService::class.java)
    }

    @Value("\${gateway.reportPrefix}")
    private lateinit var reportPrefix: String

    override fun downloadUrl(
        userId: String,
        gitUserId: String,
        gitProjectId: Long,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        val conf = txStreamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        try {
            val url = client.get(ServiceArtifactoryDownLoadResource::class).downloadIndexUrl(
                projectId = conf.projectCode!!,
                artifactoryType = artifactoryType,
                userId = userId,
                path = path,
                ttl = 10,
                directed = true
            ).data!!
            return Url(getUrl(url.url)!!, getUrl(url.url2))
        } catch (e: Exception) {
            logger.warn("TXStreamDetailService|downloadUrl|error=${e.message}")
            throw CustomException(Response.Status.BAD_REQUEST, "Artifactory download url failed. ${e.message}")
        }
    }

    private fun getUrl(url: String?): String? {
        if (url == null) {
            return null
        }
        // 没有被替换掉域名的url
        return if (!url.startsWith("/")) {
            url
        } else {
            reportPrefix + url
        }
    }
}
