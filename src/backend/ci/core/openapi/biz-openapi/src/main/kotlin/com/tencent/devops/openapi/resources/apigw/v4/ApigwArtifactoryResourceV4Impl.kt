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

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceLogFileResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwArtifactoryResourceV4
import com.tencent.devops.openapi.service.IndexService
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("UNUSED")
class ApigwArtifactoryResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val indexService: IndexService
) : ApigwArtifactoryResourceV4 {

    override fun getUserDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        logger.info("OPENAPI_ARTIFACTORY_V4|$userId|get user download url|$projectId|$artifactoryType|$path")
        return client.get(ServiceArtifactoryResource::class).downloadUrlForOpenApi(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        )
    }

    override fun getAppDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        logger.info(("OPENAPI_ARTIFACTORY_V4|$userId|get app download url|$projectId|$artifactoryType|$path"))
        return client.get(ServiceArtifactoryResource::class).appDownloadUrl(
            projectId = projectId,
            userId = userId,
            artifactoryType = artifactoryType,
            path = path
        )
    }

    override fun search(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<FileInfo>> {
        logger.info("OPENAPI_ARTIFACTORY_V4|$userId|search|$projectId|$pipelineId|$buildId|$page|$pageSize")
        val map = mutableMapOf<String, String>()
        map["pipelineId"] = checkPipelineId(projectId, pipelineId, buildId)
        map["buildId"] = buildId
        val searchProps = SearchProps(
            fileNames = null,
            props = map
        )
        return client.get(ServiceArtifactoryResource::class).searchFile(
            userId = userId,
            projectId = projectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            searchProps = searchProps
        )
    }

    override fun getPluginLogUrl(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        elementId: String,
        executeCount: String
    ): Result<Url> {
        logger.info(
            "OPENAPI_ARTIFACTORY_V4|$userId|get plugin log url|$projectId|$pipelineId|$buildId|$elementId" +
                "|$executeCount"
        )
        return client.get(ServiceLogFileResource::class).getPluginLogUrl(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            elementId = elementId,
            executeCount = executeCount
        )
    }

    override fun listCustomFiles(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        fullPath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?,
        modifiedTimeDesc: Boolean?
    ): Result<Page<FileInfo>> {
        logger.info(
            "OPENAPI_ARTIFACTORY_V4|$userId|list custom files|$projectId|$fullPath|$includeFolder|$deep" +
                "|$page|$pageSize"
        )
        return client.get(ServiceArtifactoryResource::class).listCustomFiles(
            userId = userId,
            projectId = projectId,
            fullPath = fullPath,
            includeFolder = includeFolder,
            deep = deep,
            page = page,
            pageSize = pageSize,
            modifiedTimeDesc = modifiedTimeDesc
        )
    }

    private fun checkPipelineId(projectId: String, pipelineId: String?, buildId: String): String {
        val pipelineIdFormDB = indexService.getHandle(buildId) {
            kotlin.runCatching {
                client.get(ServiceBuildResource::class).getPipelineIdFromBuildId(projectId, buildId).data
            }.getOrElse {
                throw ParamBlankException(
                    it.message ?: "Invalid buildId, please check if projectId & buildId are related"
                )
            } ?: throw ParamBlankException("Invalid buildId")
        }
        if (pipelineId != null && pipelineId != pipelineIdFormDB) {
            throw ParamBlankException("PipelineId is invalid ")
        }
        return pipelineIdFormDB
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryResourceV4Impl::class.java)
    }
}
