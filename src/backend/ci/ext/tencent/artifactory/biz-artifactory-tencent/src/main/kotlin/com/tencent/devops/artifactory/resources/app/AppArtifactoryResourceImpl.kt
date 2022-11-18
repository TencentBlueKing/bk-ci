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

package com.tencent.devops.artifactory.resources.app

import com.tencent.devops.artifactory.api.app.AppArtifactoryResource
import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileDetailForApp
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoAppService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoSearchService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.VersionUtil
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_ICON
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.BadRequestException

@RestResource
@SuppressWarnings("MagicNumber", "TooManyFunctions", "ThrowsCount")
class AppArtifactoryResourceImpl @Autowired constructor(
    private val bkRepoService: BkRepoService,
    private val bkRepoSearchService: BkRepoSearchService,
    private val bkRepoAppService: BkRepoAppService,
    private val pipelineService: PipelineService,
    private val client: Client
) : AppArtifactoryResource {

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

    override fun getBuildFileList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        appVersion: String?,
        platform: Int?
    ): Result<List<AppFileInfo>> {
        checkParameters(userId, projectId)
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        var data = bkRepoService.getBuildFileList(userId, projectId, pipelineId, buildId)
        val isNewVersion = VersionUtil.compare(appVersion, "2.0.0") >= 0
        if (isNewVersion) {
            data.forEach {
                it.modifiedTime = it.modifiedTime * 1000
            }

            // 可安装类型制定
            val (topType, secondType) = if (platform == PlatformEnum.ANDROID.id) {
                Pair("apk", "ipa")
            } else {
                Pair("ipa", "apk")
            }

            // 按字母排序
            val comparator = Comparator<AppFileInfo> { a1, a2 -> StringUtils.compareIgnoreCase(a1.name, a2.name) }
            val topSet = sortedSetOf(comparator)
            val secondSet = sortedSetOf(comparator)
            val otherSet = sortedSetOf(comparator)

            data.forEach {
                when {
                    it.name.endsWith(topType) -> {
                        topSet.add(it)
                    }
                    it.name.endsWith(secondType) -> {
                        secondSet.add(it)
                    }
                    else -> {
                        otherSet.add(it)
                    }
                }
            }

            data = mutableListOf<AppFileInfo>().let {
                it.addAll(topSet)
                it.addAll(secondSet)
                it.addAll(otherSet)
                it
            }
        }

        return Result(data)
    }

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
        return Result(FileInfoPage(0L, pageNotNull, pageSizeNotNull, result.second, result.first))
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

    override fun detail(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetailForApp> {
        checkParameters(userId, projectId, path)
        val fileDetail = try {
            bkRepoService.show(userId, projectId, artifactoryType, path)
        } catch (e: Exception) {
            logger.info("no permission , user:$userId , path:$path , artifactoryType:$artifactoryType")
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = CommonMessageCode.PERMISSION_DENIED_FOR_APP,
                defaultMessage = "请联系流水线负责人授予下载构件权限。"
            )
        }
        val pipelineId = fileDetail.meta["pipelineId"] ?: StringUtils.EMPTY
        val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName
        val pipelineInfo = if (pipelineId != StringUtils.EMPTY) {
            client.get(ServicePipelineResource::class).getPipelineInfo(projectId, pipelineId, null).data
        } else {
            null
        }

        if (!pipelineService.hasPermission(userId, projectId, pipelineId, AuthPermission.VIEW)) {
            logger.info("no permission , user:$userId , project:$projectId , pipeline:$pipelineId")
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = CommonMessageCode.PERMISSION_DENIED_FOR_APP,
                defaultMessage = "访问构件请联系流水线负责人：\n${pipelineInfo?.creator ?: ""} 授予流水线权限。"
            )
        }

        val backUpIcon = lazy { client.get(ServiceProjectResource::class).get(projectId).data!!.logoAddr!! }

        return Result(
            FileDetailForApp(
                name = fileDetail.name,
                platform = if (fileDetail.name.endsWith(".apk")) PlatformEnum.ANDROID.name else PlatformEnum.IOS.name,
                size = fileDetail.size,
                createdTime = fileDetail.createdTime,
                projectName = projectName,
                pipelineName = pipelineInfo?.pipelineName ?: StringUtils.EMPTY,
                creator = fileDetail.meta[ARCHIVE_PROPS_USER_ID] ?: StringUtils.EMPTY,
                bundleIdentifier = fileDetail.meta[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER] ?: StringUtils.EMPTY,
                logoUrl = UrlUtil.toOuterPhotoAddr(fileDetail.meta[ARCHIVE_PROPS_APP_ICON] ?: backUpIcon.value),
                path = fileDetail.path,
                fullName = fileDetail.fullName,
                fullPath = fileDetail.fullPath,
                artifactoryType = artifactoryType,
                modifiedTime = fileDetail.modifiedTime,
                md5 = fileDetail.checksums.md5,
                buildNum = NumberUtils.toInt(fileDetail.meta[ARCHIVE_PROPS_BUILD_NO], 0),
                nodeMetadata = fileDetail.nodeMetadata
            )
        )
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

    override fun externalUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)

        val result = if (path.endsWith(".ipa")) {
            bkRepoAppService.getExternalPlistDownloadUrl(userId, projectId, artifactoryType, path, 24 * 3600, false)
        } else {
            bkRepoAppService.getExternalDownloadUrl(userId, projectId, artifactoryType, path, 24 * 3600, true)
        }
        return Result(result)
    }

    override fun getFilePlist(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        experienceHashId: String?,
        organization: String?,
        ttl: Int?
    ): String {
        checkParameters(userId, projectId, path)
        if (!path.endsWith(".ipa")) {
            throw BadRequestException("Path must end with ipa")
        }
        return bkRepoAppService.getPlistFile(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path,
            ttl = ttl ?: (24 * 3600),
            directed = false,
            experienceHashId = experienceHashId,
            organization = organization
        )
    }

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        return Result(
            bkRepoAppService.getExternalDownloadUrl(
                userId, projectId, artifactoryType, path, 24 * 3600,
                true
            )
        )
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

    companion object {
        private val logger = LoggerFactory.getLogger(AppArtifactoryResourceImpl::class.java)
    }
}
