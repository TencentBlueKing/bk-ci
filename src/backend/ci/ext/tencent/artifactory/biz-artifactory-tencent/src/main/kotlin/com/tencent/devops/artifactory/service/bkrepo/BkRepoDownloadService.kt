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

import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoDownloadService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.pojo.FileShareInfo
import com.tencent.devops.artifactory.util.EmailUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

@Suppress("LongParameterList", "ComplexMethod", "LongMethod", "MagicNumber")
open class BkRepoDownloadService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoService: BkRepoService,
    private val client: Client,
    private val bkRepoClient: BkRepoClient,
    private val commonConfig: CommonConfig,
    private val shortUrlService: ShortUrlService
) : RepoDownloadService {
    override fun serviceGetExternalDownloadUrl(
        creatorId: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info(
            "serviceGetExternalDownloadUrl, creatorId: $creatorId, userId:$userId, projectId: $projectId, " +
                    "artifactoryType: $artifactoryType, path: $path, ttl: $ttl, directed: $directed"
        )
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val url = bkRepoService.externalDownloadUrl(
            creatorId = creatorId ?: userId,
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            fullPath = normalizedPath,
            ttl = ttl
        )
        return Url(StringUtil.chineseUrlEncode(url))
    }

    override fun serviceGetInnerDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info(
            "serviceGetInnerDownloadUrl, userId: $userId, projectId: $projectId, " +
                    "artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed"
        )
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val url = bkRepoService.internalDownloadUrl(userId, projectId, artifactoryType, normalizedPath, ttl)
        return Url(url)
    }

    // 创建临时分享的下载链接，目前仅为bkRepo有，所以并未抽象
    fun serviceGetInternalTemporaryAccessDownloadUrls(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPathSet: Set<String>,
        ttl: Int,
        permits: Int?
    ): List<Url> {
        logger.info(
            "serviceGetInnerDownloadUrl, userId: $userId, projectId: $projectId," +
                    " artifactoryType: $artifactoryType, argPathSet: $argPathSet, ttl: $ttl, permits: $permits"
        )
        val normalizedPaths = mutableSetOf<String>()
        argPathSet.forEach { path ->
            normalizedPaths.add(PathUtils.checkAndNormalizeAbsPath(path))
        }
        val urls = bkRepoService.internalTemporaryAccessDownloadUrls(
            userId,
            projectId,
            artifactoryType,
            normalizedPaths,
            ttl,
            permits
        )
        return urls.map { Url(it) }
    }

    override fun getDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        channelCode: ChannelCode?,
        fullUrl: Boolean
    ): Url {
        logger.info("getDownloadUrl|userId=$userId|projectId=$projectId|type=$artifactoryType|argPath=$argPath")
        pipelineService.validatePermission(userId, projectId)
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val repo = RepoUtils.getRepoByType(artifactoryType)
        val urlBuilder = StringBuilder()
        if (fullUrl) {
            urlBuilder.append(HomeHostUtil.getHost(commonConfig.devopsIdcGateway!!))
        }
        val url = urlBuilder.append("/bkrepo/api/user/generic/$projectId/$repo$normalizedPath?download=true").toString()
        return Url(url, url)
    }

    override fun getExternalUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String
    ): Url {
        logger.info("getExternalUrl|userId=$userId|projectId=$projectId|type=$artifactoryType|argPath=$argPath")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val fileInfo =
            bkRepoClient.getFileDetail(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
                ?: throw NotFoundException("文件($argPath)不存在")
        val properties = fileInfo.metadata
        properties[ARCHIVE_PROPS_PIPELINE_ID] ?: throw BadRequestException("元数据(pipelineId)不存在")
        properties[ARCHIVE_PROPS_BUILD_ID] ?: throw BadRequestException("元数据(buildId)不存在")
        val shortUrl = shortUrlService.createShortUrl(
            url = PathUtils.buildDetailLink(
                projectId = projectId,
                artifactoryType = artifactoryType.name,
                path = fileInfo.fullPath
            ),
            ttl = 24 * 3600 * 30
        )
        return Url(shortUrl)
    }

    override fun shareUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        downloadUsers: String
    ) {
        logger.info(
            "shareUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
                    "argPath: $argPath, ttl: $ttl, downloadUsers: $downloadUsers"
        )
        val path = PathUtils.checkAndNormalizeAbsPath(argPath)

        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(
                    userId,
                    projectId,
                    message = MessageCodeUtil.getCodeMessage(
                        ArtifactoryMessageCode.USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                        arrayOf(userId, projectId)
                    )
                )
            }

            ArtifactoryType.PIPELINE -> {
                val pipelineId = pipelineService.getPipelineId(path)
                pipelineService.validatePermission(
                    userId,
                    projectId,
                    pipelineId,
                    AuthPermission.SHARE,
                    MessageCodeUtil.getCodeMessage(
                        ArtifactoryMessageCode.USER_PIPELINE_SHARE_PERMISSION_FORBIDDEN,
                        arrayOf(userId, projectId, pipelineId)
                    )
                )
            }
            // 镜像不支持下载
            ArtifactoryType.IMAGE -> throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(ArtifactoryType.IMAGE.name)
            )
        }
        val downloadUrl = bkRepoService.internalDownloadUrl(userId, projectId, artifactoryType, path, ttl)
        val fileDetail = bkRepoClient.getFileDetail(
            userId,
            projectId,
            RepoUtils.getRepoByType(artifactoryType),
            path
        ) ?: throw BadRequestException("文件（$path) 不存在")
        val fileName = fileDetail.name
        val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName

        val days = ttl / (3600 * 24)
        val title = EmailUtil.getShareEmailTitle(userId, fileName, 1)
        val body = EmailUtil.getShareEmailBody(
            projectName,
            title,
            userId,
            days,
            listOf(FileShareInfo(fileName, fileDetail.md5 ?: "", projectName, downloadUrl))
        )
        val receivers = downloadUsers.split(",").toSet()
        receivers.forEach {
            if (it.startsWith("g_")) throw BadRequestException("Invalid download users")
        }

        val emailNotifyMessage = EmailUtil.makeEmailNotifyMessage(title, body, receivers)
        client.get(ServiceNotifyResource::class).sendEmailNotify(emailNotifyMessage)
    }

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?,
        region: String?,
        userId: String?
    ): List<String> {
        logger.info(
            "getThirdPartyDownloadUrl, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId" +
                    ", artifactoryType: $artifactoryType, argPath: $argPath, crossProjectId: $crossProjectId, " +
                    "ttl: $ttl, crossPipineId: $crossPipineId, crossBuildNo: $crossBuildNo, region：$region, " +
                    "userId: $userId"
        )
        var targetProjectId = projectId
        var targetPipelineId = pipelineId
        var targetBuildId = buildId
        if (!crossProjectId.isNullOrBlank()) {
            targetProjectId = crossProjectId
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                targetPipelineId = crossPipineId ?: throw BadRequestException("Invalid Parameter pipelineId")
                val targetBuild = client.get(ServiceBuildResource::class).getSingleHistoryBuild(
                    targetProjectId,
                    targetPipelineId,
                    crossBuildNo ?: throw BadRequestException("Invalid Parameter buildNo"),
                    ChannelCode.BS
                ).data
                targetBuildId = (targetBuild ?: throw BadRequestException("构建不存在($crossBuildNo)")).id
            }
        }

        val accessUserId: String
        val projectDownloadErrorMsg: String?
        val pipelineDownloadErrorMsg: String?
        if (!userId.isNullOrBlank()) {
            accessUserId = userId
            projectDownloadErrorMsg = MessageCodeUtil.getCodeMessage(
                ArtifactoryMessageCode.USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                arrayOf(accessUserId, targetProjectId)
            )
            pipelineDownloadErrorMsg = MessageCodeUtil.getCodeMessage(
                ArtifactoryMessageCode.USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN,
                arrayOf(accessUserId, targetProjectId, targetPipelineId)
            )
        } else {
            accessUserId = client.get(ServicePipelineResource::class)
                .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
            projectDownloadErrorMsg = MessageCodeUtil.getCodeMessage(
                ArtifactoryMessageCode.LAST_MODIFY_USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                arrayOf(accessUserId, targetProjectId)
            )
            pipelineDownloadErrorMsg = MessageCodeUtil.getCodeMessage(
                ArtifactoryMessageCode.LAST_MODIFY_USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN,
                arrayOf(accessUserId, targetProjectId, targetPipelineId)
            )
        }
        logger.info(
            "accessUserId: $accessUserId, targetProjectId: $targetProjectId, " +
                    "targetPipelineId: $targetPipelineId, targetBuildId: $targetBuildId"
        )

        // 校验用户权限, auth权限优化实施后可以去掉
        if (artifactoryType == ArtifactoryType.CUSTOM_DIR && !pipelineService.hasPermission(
                accessUserId,
                targetProjectId
            )
        ) {
            throw PermissionForbiddenException(projectDownloadErrorMsg)
        }
        if (artifactoryType == ArtifactoryType.PIPELINE) {
            pipelineService.validatePermission(
                userId = accessUserId,
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                permission = AuthPermission.DOWNLOAD,
                message = pipelineDownloadErrorMsg
            )
        }

        val regex = Pattern.compile(",|;")
        val pathArray = regex.split(argPath)
        val fileList = mutableListOf<FileDetail>()
        pathArray.forEach { path ->
            val absPath = "/${PathUtils.normalize(path).removePrefix("/")}"
            val filePath = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/$targetPipelineId/$targetBuildId/${
                    PathUtils.getParentFolder(absPath).removePrefix("/")
                }" // /$projectId/$pipelineId/$buildId/path/
            } else {
                "/${PathUtils.getParentFolder(absPath).removePrefix("/")}" // /path/
            }
            val fileName = PathUtils.getFileName(path) // *.txt

            bkRepoClient.queryByPathEqOrNameMatchOrMetadataEqAnd(
                userId = accessUserId,
                projectId = targetProjectId,
                repoNames = listOf(RepoUtils.getRepoByType(artifactoryType)),
                filePaths = listOf(filePath),
                fileNames = listOf(fileName),
                metadata = mapOf(),
                page = 0,
                pageSize = 10000
            ).records.forEach {
                fileList.add(RepoUtils.toFileDetail(it))
            }
        }

        val resultList = mutableListOf<String>()
        fileList.forEach {
            val repoName = RepoUtils.getRepoByType(artifactoryType)
            val shareUri = bkRepoClient.createShareUri(
                creatorId = accessUserId,
                projectId = targetProjectId,
                repoName = repoName,
                fullPath = it.fullPath,
                downloadUsers = listOf(),
                downloadIps = listOf(),
                timeoutInSeconds = (ttl ?: 24 * 3600).toLong()
            )
            if (region == "OPENAPI") {
                resultList.add("${bkRepoClient.getRkRepoIdcHost()}/repository$shareUri&download=true")
            } else {
                resultList.add(
                    "${commonConfig.devopsOuterHostGateWay}/bkrepo/api/external/repository$shareUri&download=true"
                )
            }
        }
        return resultList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoDownloadService::class.java)
    }
}
