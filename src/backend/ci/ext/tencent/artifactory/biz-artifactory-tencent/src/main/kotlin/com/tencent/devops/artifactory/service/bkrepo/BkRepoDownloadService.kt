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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.BUILD_NOT_EXIST
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.METADATA_NOT_EXIST
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoDownloadService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.pojo.FileShareInfo
import com.tencent.devops.artifactory.util.EmailUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RegionUtil
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.constant.CommonMessageCode.FILE_NOT_EXIST
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_APP_TITLE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_ICON
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_NAME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.audit.ActionAuditContent.BUILD_ID_TEMPLATE
import com.tencent.devops.common.audit.ActionAuditContent.PIPELINE_DOWNLOAD_CONTENT
import com.tencent.devops.common.auth.api.ActionId.PIPELINE_DOWNLOAD
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId.PIPELINE
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response

@Suppress("LongParameterList", "ComplexMethod", "LongMethod", "MagicNumber")
open class BkRepoDownloadService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoService: BkRepoService,
    private val client: Client,
    private val bkRepoClient: BkRepoClient,
    private val commonConfig: CommonConfig,
    private val shortUrlService: ShortUrlService
) : RepoDownloadService {
    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun outerDownloadUrlByToken(
        creatorId: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int
    ): Url {
        logger.info(
            "outerBkrepoDownloadUrl, creatorId: $creatorId, userId:$userId, projectId: $projectId, " +
                "artifactoryType: $artifactoryType, path: $path, ttl: $ttl"
        )
        val normalizedPath = getNormalizePath(path, artifactoryType, creatorId ?: userId, projectId)
        val url = bkRepoService.externalDownloadUrl(
            creatorId = creatorId ?: userId,
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            fullPath = normalizedPath,
            ttl = ttl
        )
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = normalizedPath
        )
        return Url(StringUtil.chineseUrlEncode(url))
    }

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun outerPlistContent(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        experienceHashId: String?,
        organization: String?
    ): String {
        logger.info(
            "getPlistFile, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
                "argPath: $argPath, experienceHashId: $experienceHashId"
        )

        if (experienceHashId != null) {
            val check = client.get(ServiceExperienceResource::class).check(userId, experienceHashId, organization)
            if (!check.isOk() || !check.data!!) {
                throw CustomException(
                    Response.Status.BAD_REQUEST, MessageUtil.getMessageByLocale(
                    messageCode = ArtifactoryMessageCode.NO_EXPERIENCE_PERMISSION,
                    language = I18nUtil.getLanguage(userId)
                )
                )
            }
        }

        val creatorId = if (experienceHashId != null) {
            val experience = client.get(ServiceExperienceResource::class).get(userId, projectId, experienceHashId)
            if (experience.isOk() && experience.data != null) {
                experience.data!!.creator
            } else {
                userId
            }
        } else {
            userId
        }

        // 获取IP下载链接
        val ipaExternalDownloadUrl = outerDownloadUrlByToken(
            creatorId = creatorId,
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = argPath,
            ttl = ttl
        )
        val ipaExternalDownloadUrlEncode = StringUtil.chineseUrlEncode(ipaExternalDownloadUrl.url)

        // 获取IPA属性
        val fileProperties = bkRepoClient.listMetadata(
            creatorId,
            projectId,
            RepoUtils.getRepoByType(artifactoryType),
            argPath
        )
        val bundleIdentifier = fileProperties[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER] ?: ""
        val appTitle = fileProperties[ARCHIVE_PROPS_APP_NAME] ?: fileProperties[ARCHIVE_PROPS_APP_APP_TITLE] ?: ""
        val appVersion = fileProperties[ARCHIVE_PROPS_APP_VERSION] ?: ""
        val appIcon = fileProperties[ARCHIVE_PROPS_APP_ICON]?.let { UrlUtil.toOuterPhotoAddr(it) } ?: ""
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>items</key>
                <array>
                    <dict>
                        <key>assets</key>
                        <array>
                            <dict>
                                <key>kind</key>
                                <string>software-package</string>
                                <key>url</key>
                                <string>${getCdataStr(ipaExternalDownloadUrlEncode)}</string>
                            </dict>
                            <dict>
                                <key>kind</key>
                                <string>display-image</string>
                                <key>needs-shine</key>
                                <false/>
                                <key>url</key>
                                <string>${getCdataStr(appIcon)}</string>
                            </dict>
                        </array>
                        <key>metadata</key>
                        <dict>
                            <key>bundle-identifier</key>
                            <string>${getCdataStr(bundleIdentifier)}</string>
                            <key>bundle-version</key>
                            <string>${getCdataStr(appVersion)}</string>
                            <key>title</key>
                            <string>${getCdataStr(appTitle)}</string>
                            <key>kind</key>
                            <string>software</string>
                        </dict>
                    </dict>
                </array>
            </dict>
            </plist>
        """.trimIndent()
    }

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun outerPlistUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int
    ): Url {
        logger.info(
            "getExternalPlistDownloadUrl, userId: $userId, projectId: $projectId, " +
                "artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl"
        )
        val normalizedPath = getNormalizePath(argPath, artifactoryType, userId, projectId)
        val url =
            StringUtil.chineseUrlEncode(
                "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/" +
                    "$artifactoryType/filePlist?path=$normalizedPath&x-devops-project-id=$projectId"
            )
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = normalizedPath
        )
        return Url(url)
    }

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun innerDownloadUrlByToken(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int
    ): Url {
        logger.info(
            "innerBkrepoDownloadUrl, userId: $userId, projectId: $projectId, " +
                "artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl"
        )
        val normalizedPath = getNormalizePath(argPath, artifactoryType, userId, projectId)
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = normalizedPath
        )
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

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun innerDownloadUrlByUser(
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
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = normalizedPath
        )
        return Url(url, url)
    }

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun outerHtmlUrl4Download(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String
    ): Url {
        logger.info("getExternalUrl|userId=$userId|projectId=$projectId|type=$artifactoryType|argPath=$argPath")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        val fileInfo =
            bkRepoClient.getFileDetail(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
                ?: throw NotFoundException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = FILE_NOT_EXIST,
                        params = arrayOf(argPath)
                    )
                )
        val properties = fileInfo.metadata
        properties[ARCHIVE_PROPS_PIPELINE_ID] ?: throw BadRequestException(
            I18nUtil.getCodeLanMessage(
                messageCode = METADATA_NOT_EXIST,
                params = arrayOf("pipelineId")
            )
        )
        properties[ARCHIVE_PROPS_BUILD_ID] ?: throw BadRequestException(
            I18nUtil.getCodeLanMessage(
                messageCode = METADATA_NOT_EXIST,
                params = arrayOf("buildId")
            )
        )
        val shortUrl = shortUrlService.createShortUrl(
            url = PathUtils.buildDetailLink(
                projectId = projectId,
                artifactoryType = artifactoryType.name,
                path = fileInfo.fullPath
            ),
            ttl = 24 * 3600 * 30
        )
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = normalizedPath
        )
        return Url(shortUrl)
    }

    @ActionAuditRecord(
        actionId = PIPELINE_DOWNLOAD,
        instance = AuditInstanceRecord(
            resourceType = PIPELINE
        ),
        content = PIPELINE_DOWNLOAD_CONTENT
    )
    override fun sendNotifyWithInnerUrl(
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
        val path = getNormalizePath(argPath, artifactoryType, userId, projectId)
        val downloadUrl = bkRepoService.internalDownloadUrl(userId, projectId, artifactoryType, path, ttl)
        val fileDetail = bkRepoClient.getFileDetail(
            userId,
            projectId,
            RepoUtils.getRepoByType(artifactoryType),
            path
        ) ?: throw BadRequestException(
            I18nUtil.getCodeLanMessage(
                messageCode = FILE_NOT_EXIST,
                params = arrayOf(path)
            )
        )
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
        // 审计
        audit(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        )
    }

    override fun innerCrossDownloadUrl(
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
                targetBuildId = (targetBuild ?: throw BadRequestException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BUILD_NOT_EXIST,
                        params = arrayOf(crossBuildNo)
                    )
                )).id
            }
        }

        val accessUserId: String
        val projectDownloadErrorMsg: String?
        val pipelineDownloadErrorMsg: String?
        if (!userId.isNullOrBlank()) {
            accessUserId = userId
            projectDownloadErrorMsg = I18nUtil.getCodeLanMessage(
                messageCode = ArtifactoryMessageCode.USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                params = arrayOf(accessUserId, targetProjectId)
            )
            pipelineDownloadErrorMsg = I18nUtil.getCodeLanMessage(
                messageCode = ArtifactoryMessageCode.USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN,
                params = arrayOf(accessUserId, targetProjectId, targetPipelineId)
            )
        } else {
            accessUserId = client.get(ServicePipelineResource::class)
                .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
            projectDownloadErrorMsg = I18nUtil.getCodeLanMessage(
                messageCode = ArtifactoryMessageCode.LAST_MODIFY_USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                params = arrayOf(accessUserId, targetProjectId)
            )
            pipelineDownloadErrorMsg = I18nUtil.getCodeLanMessage(
                messageCode = ArtifactoryMessageCode.LAST_MODIFY_USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN,
                params = arrayOf(accessUserId, targetProjectId, targetPipelineId)
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
                timeoutInSeconds = ((ttl ?: (24 * 3600))).toLong()
            )
            if (region == "OPENAPI") {
                resultList.add("${bkRepoClient.getRkRepoIdcHost()}/repository$shareUri&download=true")
            } else {
                resultList.add(
                    "${RegionUtil.getRegionUrl(region)}/bkrepo/api/external/repository$shareUri&download=true"
                )
            }
        }
        return resultList
    }

    private fun getCdataStr(str: String): String = "<![CDATA[$str]]>"

    private fun getNormalizePath(
        argPath: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        projectId: String
    ): String {
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        checkArtifactoryType(artifactoryType, userId, projectId, normalizedPath)
        return normalizedPath
    }

    private fun checkArtifactoryType(
        artifactoryType: ArtifactoryType,
        userId: String,
        projectId: String,
        normalizedPath: String
    ) {
        try {
            // 能够获取文件属性证明有下载文件的权限
            bkRepoClient.listMetadata(
                userId,
                projectId,
                RepoUtils.getRepoByType(artifactoryType),
                normalizedPath
            )
        } catch (e: Exception) {
            logger.error("checkArtifactoryType failed", e)
            throw CustomException(
                Response.Status.BAD_REQUEST,
                MessageUtil.getMessageByLocale(
                    messageCode = ArtifactoryMessageCode.METADATA_NOT_EXIST_DOWNLOAD_FILE_BY_SHARING,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf("pipelineId")
                )
            )
        }
    }

    private fun audit(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ) {
        val repoName = RepoUtils.getRepoByType(artifactoryType)
        try {
            val pipelineId = bkRepoClient.listMetadata(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                path = path
            )["pipelineId"]
            val buildId = bkRepoClient.listMetadata(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                path = path
            )["buildId"]
            // 审计
            ActionAuditContext.current()
                .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
                .scopeId = projectId
            if (!pipelineId.isNullOrEmpty()) {
                val pipelineName = client.get(ServicePipelineResource::class)
                    .getPipelineInfo(projectId, pipelineId, null).data?.pipelineName ?: ""
                ActionAuditContext.current()
                    .setInstanceName(pipelineName)
                    .setInstanceId(pipelineId)
            }
            if (!buildId.isNullOrEmpty()) {
                ActionAuditContext.current()
                    .addExtendData("buildId", buildId)
                    .addAttribute(BUILD_ID_TEMPLATE, buildId)
            }
        } catch (ignore: Exception) {
            logger.warn("audit download artifacts fail!$projectId|$repoName|$path")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoDownloadService::class.java)
    }
}
