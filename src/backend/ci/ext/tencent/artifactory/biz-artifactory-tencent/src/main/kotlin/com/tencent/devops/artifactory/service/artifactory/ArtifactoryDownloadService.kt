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

package com.tencent.devops.artifactory.service.artifactory

import com.tencent.devops.artifactory.client.JFrogAQLService
import com.tencent.devops.artifactory.client.JFrogApiService
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoDownloadService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.pojo.FileShareInfo
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.util.EmailUtil
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RegionUtil
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException
import javax.ws.rs.core.Response

@Service
class ArtifactoryDownloadService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val client: Client,
    private val artifactoryService: ArtifactoryService,
    private val shortUrlApi: ShortUrlApi,
    private val jFrogService: JFrogService,
    private val jFrogApiService: JFrogApiService,
    private val jFrogAQLService: JFrogAQLService,
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val shortUrlService: ShortUrlService
) : RepoDownloadService {
    override fun serviceGetExternalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info("serviceGetExternalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, normalizedPath)
        val url = jFrogApiService.externalDownloadUrl(realPath, userId, ttl, directed)
        return Url(StringUtil.chineseUrlEncode(url))
    }

    override fun serviceGetInnerDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean): Url {
        logger.info("serviceGetInnerDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val url = jFrogApiService.internalDownloadUrl(realPath, ttl, userId)
        return Url(url)
    }

    override fun getDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, channelCode: ChannelCode?): Url {
        logger.info("getDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, channelCode: $channelCode")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId, message = "用户（$userId) 没有项目（$projectId）下载权限)")
            }
            ArtifactoryType.PIPELINE -> {
                val properties = jFrogPropertiesApi.getProperties(realPath)
                if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
                }
                val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
                pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.DOWNLOAD, "用户($userId)在项目($projectId)下没有流水线${pipelineId}下载构建权限")
            }
        }
        val url = RegionUtil.replaceRegionServer(jFrogApiService.downloadUrl(realPath), RegionUtil.IDC)
        return Url(url, url)
    }

    override fun getExternalUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url {
        logger.info("getExternalUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw RuntimeException("元数据(pipelineId)不存在")
        }
        if (!properties.containsKey(ARCHIVE_PROPS_BUILD_ID)) {
            throw RuntimeException("元数据(buildId)不存在")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        val buildId = properties[ARCHIVE_PROPS_BUILD_ID]!!.first()
        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId, message = "用户（$userId) 没有项目（$projectId）下载权限)")
            }
            ArtifactoryType.PIPELINE -> {
                pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.DOWNLOAD, "用户($userId)在项目($projectId)下没有流水线${pipelineId}下载构建权限")
            }
        }

        val url = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
        return Url(shortUrlService.createShortUrl(url, 300))
    }

    override fun shareUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, downloadUsers: String) {
        logger.info("shareUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, downloadUsers: $downloadUsers")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                val pipelineId = pipelineService.getPipelineId(path)
                pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.SHARE, "用户($userId)在项目($projectId)下没有流水线${pipelineId}分享权限")
            }
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId)
            }
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val downloadUrl = jFrogApiService.internalDownloadUrl(realPath, ttl, downloadUsers)
        val jFrogDetail = jFrogService.file(realPath)
        val fileName = JFrogUtil.getFileName(path)
        val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName

        val days = ttl / (3600 * 24)
        val title = EmailUtil.getShareEmailTitle(userId, fileName, 1)
        val body = EmailUtil.getShareEmailBody(projectName, title, userId, days, listOf(FileShareInfo(fileName, jFrogDetail.checksums?.md5
            ?: "", projectName, downloadUrl)))
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
        crossPipelineId: String?,
        crossBuildNo: String?,
        region: String?,
        userId: String?
    ): List<String> {
        logger.info("getThirdPartyDownloadUrl, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId" +
            ", artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, crossProjectId: $crossProjectId" +
            ", crossPipelineId: $crossPipelineId, crossBuildNo: $crossBuildNo, region：$region, userId: $userId")
        var targetProjectId = projectId
        var targetPipelineId = pipelineId
        var targetBuildId = buildId
        if (!crossProjectId.isNullOrBlank()) {
            targetProjectId = crossProjectId!!
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                targetPipelineId = crossPipelineId ?: throw BadRequestException("Invalid Parameter pipelineId")
                val targetBuild = client.get(ServiceBuildResource::class).getSingleHistoryBuild(
                    targetProjectId,
                    targetPipelineId,
                    crossBuildNo ?: throw BadRequestException("Invalid Parameter buildNo"),
                    ChannelCode.BS
                ).data
                targetBuildId = (targetBuild ?: throw BadRequestException("构建不存在($crossBuildNo)")).id
            }
        }

        var accessUserId = when {
            !userId.isNullOrBlank() -> {
                userId!!
            }
            !crossProjectId.isNullOrBlank() -> {
                client.get(ServicePipelineResource::class).getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
            }
            else -> {
                null
            }
        }
        logger.info("accessUserId: $accessUserId, targetProjectId: $targetProjectId, targetPipelineId: $targetPipelineId, targetBuildId: $targetBuildId")

        // 校验用户权限
        if (accessUserId != null) {
            if (artifactoryType == ArtifactoryType.CUSTOM_DIR && !pipelineService.hasPermission(accessUserId, targetProjectId)) {
                throw BadRequestException("用户（$accessUserId) 没有项目（$targetProjectId）下载权限)")
            }
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                pipelineService.validatePermission(accessUserId, targetProjectId, targetPipelineId, AuthPermission.DOWNLOAD, "用户($accessUserId)在项目($targetProjectId)下没有流水线($targetPipelineId)下载构件权限")
            }
        }

        val pathArray = regex.split(argPath)
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val jFrogFileInfoList = mutableListOf<JFrogAQLFileInfo>()
        pathArray.forEach { path ->
            val normalizedPath = JFrogUtil.normalize(path)
            val realPath = if (path.startsWith("/")) normalizedPath else "/$normalizedPath"

            val pathPrefix = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/" + JFrogUtil.getPipelinePathPrefix(targetProjectId).removePrefix(repoPathPrefix) + "$targetPipelineId/$targetBuildId/" + JFrogUtil.getParentFolder(realPath).removePrefix("/")
            } else {
                "/" + JFrogUtil.getCustomDirPathPrefix(targetProjectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(realPath).removePrefix("/")
            }
            val fileName = JFrogUtil.getFileName(path)

            val jFrogAQLFileInfoList = jFrogAQLService.searchFileByRegex(repoPathPrefix, setOf(pathPrefix), setOf(fileName))
            logger.info("match file list[$path]: $jFrogFileInfoList")
            jFrogFileInfoList.addAll(jFrogAQLFileInfoList)
        }

        val fileInfoList = artifactoryService.transferJFrogAQLFileInfo(targetProjectId, jFrogFileInfoList, emptyList(), false)
        val filePathList = fileInfoList.map { JFrogUtil.getRealPath(targetProjectId, artifactoryType, it.fullPath) }
        return jFrogApiService.batchThirdPartyDownloadUrl(filePathList, ttl ?: 24 * 3600).map {
            RegionUtil.replaceRegionServer(it.value, region)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryDownloadService::class.java)
        private val regex = Pattern.compile(",|;")
    }
}