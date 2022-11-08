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

package com.tencent.devops.artifactory.resources.user

import com.tencent.devops.artifactory.api.user.UserArtifactoryResource
import com.tencent.devops.artifactory.pojo.CopyToCustomReq
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoSearchService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.web.RestResource
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import javax.ws.rs.BadRequestException

@RestResource
class UserArtifactoryResourceImpl @Autowired constructor(
    val bkRepoService: BkRepoService,
    val bkRepoSearchService: BkRepoSearchService,
    val bkRepoDownloadService: BkRepoDownloadService
) : UserArtifactoryResource {
    override fun checkDevnetGateway(userId: String): Result<Boolean> {
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<FileInfo>> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoService.list(userId, projectId, artifactoryType, path))
    }

    override fun getOwnFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = bkRepoService.getOwnFileList(userId, projectId, limit.offset, limit.limit)
        return Result(FileInfoPage(0L, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    @Timed
    override fun search(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 10000
        val result = bkRepoSearchService.search(userId, projectId, searchProps, pageNotNull, pageSizeNotNull)
        return Result(FileInfoPage(
            count = result.first,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            records = result.second,
            timestamp = LocalDateTime.now().timestamp()
        ))
    }

    override fun searchFileAndProperty(
        userId: String,
        projectId: String,
        searchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val result = bkRepoSearchService.searchFileAndProperty(userId, projectId, searchProps)
        return Result(FileInfoPage(result.second.size.toLong(), 0, 0, result.second, result.first))
    }

    override fun show(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoService.show(userId, projectId, artifactoryType, path))
    }

    override fun properties(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoService.getProperties(userId, projectId, artifactoryType, path))
    }

    override fun folderSize(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FolderSize> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoService.folderSize(userId, projectId, artifactoryType, path))
    }

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoDownloadService.getDownloadUrl(userId, projectId, artifactoryType, path, fullUrl = false))
    }

    override fun ioaUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoDownloadService.getDownloadUrl(userId, projectId, artifactoryType, path, fullUrl = false))
    }

    override fun shareUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        downloadUsers: String
    ): Result<Boolean> {
        checkParameters(userId, projectId, path)
        if (ttl < 0) {
            throw InvalidParamException("Invalid ttl")
        }
        if (downloadUsers.isBlank()) {
            throw InvalidParamException("Invalid downloadUsers")
        }
        bkRepoDownloadService.shareUrl(userId, projectId, artifactoryType, path, ttl, downloadUsers)
        return Result(true)
    }

    override fun externalUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        return Result(bkRepoDownloadService.getExternalUrl(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path)
        )
    }

    override fun getFilePipelineInfo(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FilePipelineInfo> {
        checkParameters(userId, projectId, path)
        return Result(bkRepoService.getFilePipelineInfo(userId, projectId, artifactoryType, path))
    }

    override fun copyToCustom(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        copyToCustomReq: CopyToCustomReq
    ): Result<Boolean> {
        checkParameters(userId, projectId)
        bkRepoService.copyToCustom(userId, projectId, pipelineId, buildId, copyToCustomReq)
        return Result(true)
    }

    private fun checkParameters(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
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
}
