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

package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.ArtifactoryCreateInfo
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArtifactoryInfoService
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.artifactory.ArtifactorySearchService
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoSearchService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.BadRequestException

@RestResource
class ServiceArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val bkRepoService: BkRepoService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val bkRepoSearchService: BkRepoSearchService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val artifactoryInfoService: ArtifactoryInfoService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : ServiceArtifactoryResource {
    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<Boolean> {
        checkParam(projectId)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.check(projectId, artifactoryType, path))
        } else {
            Result(artifactoryService.check(projectId, artifactoryType, path))
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

    override fun properties(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<List<Property>> {
        checkParam(projectId)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.getProperties(projectId, artifactoryType, path))
        } else {
            Result(artifactoryService.getProperties(projectId, artifactoryType, path))
        }
    }

    override fun externalUrl(
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
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoDownloadService.serviceGetExternalDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
        } else {
            Result(artifactoryDownloadService.serviceGetExternalDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
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
        checkParam(projectId)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val isDirected = directed ?: false
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoDownloadService.serviceGetInnerDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
        } else {
            Result(artifactoryDownloadService.serviceGetInnerDownloadUrl(userId, projectId, artifactoryType, path, ttl, isDirected))
        }
    }

    override fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<FileDetail> {
        checkParam(projectId)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.show(projectId, artifactoryType, path))
        } else {
            Result(artifactoryService.show(projectId, artifactoryType, path))
        }
    }

    override fun search(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)

        return if (repoGray.isGray(projectId, redisOperation)) {
            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: 10000
            val result = bkRepoSearchService.serviceSearch(projectId, searchProps, pageNotNull, pageSizeNotNull)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        } else {
            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: -1
            val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
            val result = artifactorySearchService.serviceSearch(projectId, searchProps, offset, pageSizeNotNull)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        }
    }

    override fun searchFile(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)

        return if (repoGray.isGray(projectId, redisOperation)) {
            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: 10000
            val result = bkRepoSearchService.serviceSearchFileByRegex(projectId, pipelineId, buildId, regexPath, customized)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        } else {
            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: -1
            val result = artifactorySearchService.serviceSearchFileByRegex(projectId, pipelineId, buildId, regexPath, customized)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        }
    }

    override fun searchFileAndPropertyByAnd(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        return if (repoGray.isGray(projectId, redisOperation)) {
            val result = bkRepoSearchService.serviceSearchFileAndProperty(projectId, searchProps)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        } else {
            val result = artifactorySearchService.serviceSearchFileAndProperty(projectId, searchProps)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        }
    }

    override fun searchFileAndPropertyByOr(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        logger.info("searchFileAndPropertyByOr, projectId: $projectId, page: $page, pageSize: $pageSize, searchProps: $searchProps")
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        return if (repoGray.isGray(projectId, redisOperation)) {
            val result = bkRepoSearchService.serviceSearchFileAndPropertyByOr(projectId, searchProps)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        } else {
            val result = artifactorySearchService.serviceSearchFileAndPropertyByOr(projectId, searchProps)
            Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
        }
    }

    override fun createDockerUser(projectId: String): Result<DockerUser> {
        checkParam(projectId)
        val result = artifactoryService.createDockerUser(projectId)
        return Result(DockerUser(result.user, result.password))
    }

    override fun setProperties(
        projectId: String,
        imageName: String,
        tag: String,
        properties: Map<String, String>
    ): Result<Boolean> {
        if (imageName.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        if (tag.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        artifactoryService.setDockerProperties(projectId, imageName, tag, properties)
        return Result(true)
    }

    override fun searchCustomFiles(projectId: String, condition: CustomFileSearchCondition): Result<List<String>> {
        checkParam(projectId)

        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(bkRepoService.listCustomFiles(projectId, condition))
        } else {
            Result(artifactoryService.listCustomFiles(projectId, condition))
        }
    }

    override fun getJforgInfoByteewTime(
        startTime: Long,
        endTime: Long,
        page: Int,
        pageSize: Int
    ): Result<List<FileInfo>> {
        return Result(artifactorySearchService.getJforgInfoByteewTime(page, pageSize, startTime, endTime))
    }

    override fun createArtifactoryInfo(
        buildId: String,
        pipelineId: String,
        projectId: String,
        buildNum: Int,
        fileInfo: FileInfo,
        dataFrom: Int
    ): Result<Long> {
        checkInfoParam(buildId, pipelineId, fileInfo)
        val id = artifactoryInfoService.createInfo(
            buildId = buildId,
            pipelineId = pipelineId,
            projectId = projectId,
            buildNum = buildNum,
            fileInfo = fileInfo,
            dataFrom = dataFrom
        )
        return Result(id)
    }

    override fun batchCreateArtifactoryInfo(infoList: List<ArtifactoryCreateInfo>): Result<Int> {
        return Result(artifactoryInfoService.batchCreateArtifactoryInfo(infoList))
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

    override fun getReportRootUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<String> {
        val url =
            "${HomeHostUtil.innerApiHost()}/ms/artifactory/api-html/user/reports/$projectId/$pipelineId/$buildId/$taskId"
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
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val bkProps = mutableListOf<Property>()
        searchProps.props.forEach { (k, v) ->
            bkProps.add(Property(k, v))
        }
        val result = artifactorySearchService.serviceSearch(
            projectId = projectId,
            searchProps = bkProps,
            offset = offset,
            limit = pageSizeNotNull
        )
        return Result(
            Page(
                count = result.first,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                totalPages = (result.first / pageSizeNotNull).toInt(),
                records = result.second
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceArtifactoryResourceImpl::class.java)
    }
}