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

package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwArtifactoryResourceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryResourceV2 {
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryResourceV2Impl::class.java)
    }

    override fun getThirdPartyDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        region: String?,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?
    ): Result<List<String>> {
        var pipelineId: String? = null
        var buildId: String? = null
        var subPath = path
        if (artifactoryType == ArtifactoryType.PIPELINE) {
            val normalizePath = PathUtils.normalizeFullPath(path)
            val pathList = normalizePath.split("/")
            logger.info("getThirdPartyDownloadUrl pathList:$pathList")
            if (pathList.size < 3) {
                throw IllegalArgumentException("invalid path: $path")
            }
            pipelineId = pathList[1]
            buildId = pathList[2]
            subPath = normalizePath.replace("/$pipelineId/$buildId", "")
        }

        logger.info("getThirdPartyDownloadUrl pipelineId:$pipelineId")
        logger.info("getThirdPartyDownloadUrl buildId:$buildId")
        logger.info("getThirdPartyDownloadUrl subPath:$subPath")
        val thirdPartyDownloadUrl = client.get(ServiceArtifactoryDownLoadResource::class).getThirdPartyDownloadUrl(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = artifactoryType,
            path = subPath,
            ttl = ttl,
            crossPipineId = null,
            crossProjectId = null,
            crossBuildNo = null,
            region = region,
            userId = userId
        )
        val urls = thirdPartyDownloadUrl.data?.map {
            val proxyPath = "/bkrepo/api/external"
            if (it.contains(proxyPath)) {
                "https://bkrepo.woa.com/" + it.split(proxyPath)[1]
            } else {
                it
            }
        } ?: emptyList()

        return Result(urls)
    }

    override fun getUserDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        return client.get(ServiceArtifactoryResource::class).downloadUrlForOpenApi(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        )
    }

    override fun search(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        val searchProps = listOf(Property("pipelineId", pipelineId), Property("buildId", buildId))
        return client.get(ServiceArtifactoryResource::class).search(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            searchProps = searchProps
        )
    }
}
