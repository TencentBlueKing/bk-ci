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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.bkrepo.BkRepoCustomDirService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoSearchService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.constant.REPO_CUSTOM
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import javax.ws.rs.BadRequestException
import kotlin.math.ceil

@RestResource
class ServiceArtifactoryResourceImpl @Autowired constructor(
    private val bkRepoService: BkRepoService,
    private val bkRepoSearchService: BkRepoSearchService,
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val bkRepoCustomDirService: BkRepoCustomDirService
) : ServiceArtifactoryResource {
    override fun check(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Boolean> {
        checkParam(projectId)
        return Result(bkRepoService.check(userId, projectId, artifactoryType, path))
    }

    override fun acrossProjectCopy(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        checkParam(projectId)
        return Result(
            bkRepoService.acrossProjectCopy(userId, projectId, artifactoryType, path, targetProjectId, targetPath)
        )
    }

    override fun properties(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParam(projectId)
        return Result(bkRepoService.getProperties(userId, projectId, artifactoryType, path))
    }

    override fun externalUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        creatorId: String?,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val isDirected = directed ?: false
        return Result(
            bkRepoDownloadService.serviceGetExternalDownloadUrl(
                creatorId, userId,
                projectId, artifactoryType, path,
                ttl, isDirected
            )
        )
    }

    override fun appDownloadUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String
    ): Result<Url> {
        return Result(
            bkRepoDownloadService.getExternalUrl(
                userId = userId,
                projectId = projectId,
                artifactoryType = artifactoryType,
                argPath = path
            )
        )
    }

    override fun downloadUrlForOpenApi(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoDownloadService.getDownloadUrl(userId, projectId, artifactoryType, path))
    }

    override fun downloadUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val isDirected = directed ?: false
        return Result(
            bkRepoDownloadService.serviceGetInnerDownloadUrl(
                userId, projectId, artifactoryType, path, ttl,
                isDirected
            )
        )
    }

    override fun show(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        checkParam(projectId)
        return Result(bkRepoService.show(userId, projectId, artifactoryType, path))
    }

    override fun search(
        userId: String?,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 10000
        val result = bkRepoSearchService.serviceSearch(userId, projectId, searchProps, pageNotNull, pageSizeNotNull)
        return Result(FileInfoPage(
            count = result.first,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            records = result.second,
            timestamp = LocalDateTime.now().timestamp()
        ))
    }

    override fun searchCustomFiles(
        userId: String,
        projectId: String,
        condition: CustomFileSearchCondition
    ): Result<List<String>> {
        checkParam(projectId)
        return Result(bkRepoService.listCustomFiles(userId, projectId, condition))
    }

    private fun checkInfoParam(buildId: String, pipelineId: String, fileInfo: FileInfo) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkParameters(userId: String, projectId: String, path: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }

    override fun getReportRootUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<String> {
        val url =
            "${HomeHostUtil.innerApiHost()}/ms/artifactory/api-html/user/reports/" +
                    "$projectId/$pipelineId/$buildId/$taskId"
        return Result(url)
    }

    override fun searchFile(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<Page<FileInfo>> {

        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 500
        val result = bkRepoSearchService.search(userId, projectId, searchProps, pageNotNull, pageSizeNotNull)
        return Result(
            Page(
                count = result.first,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                totalPages = ceil(result.first / pageSizeNotNull.toDouble()).toInt(),
                records = result.second
            )
        )
    }

    override fun listCustomFiles(
        userId: String,
        projectId: String,
        fullPath: String,
        includeFolder: Boolean?,
        deep: Boolean?,
        page: Int?,
        pageSize: Int?,
        modifiedTimeDesc: Boolean?
    ): Result<Page<FileInfo>> {
        val data = bkRepoCustomDirService.listPage(
            userId = userId,
            projectId = projectId,
            repoName = REPO_CUSTOM,
            fullPath = fullPath,
            includeFolder = includeFolder ?: true,
            deep = deep ?: false,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            modifiedTimeDesc = modifiedTimeDesc ?: false
        )
        return Result(data)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceArtifactoryResourceImpl::class.java)
    }
}
