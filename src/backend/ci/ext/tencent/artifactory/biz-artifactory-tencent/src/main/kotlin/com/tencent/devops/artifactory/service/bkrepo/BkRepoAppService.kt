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

import com.tencent.devops.artifactory.constant.ArtifactoryCode.BK_METADATA_NOT_EXIST_DOWNLOAD_FILE_BY_SHARING
import com.tencent.devops.artifactory.constant.ArtifactoryCode.BK_NO_EXPERIENCE_PERMISSION
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.AppService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_APP_TITLE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_ICON
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_NAME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.MessageFormat
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongMethod")
class BkRepoAppService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoClient: BkRepoClient,
    private val bkRepoService: BkRepoService,
    private val client: Client
) : AppService {
    override fun getExternalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info(
            "getExternalDownloadUrl, userId: $userId, projectId: $projectId, " +
                    "artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed"
        )
        val normalizedPath = getNormalizePath(argPath, artifactoryType, userId, projectId)
        val url = bkRepoService.externalDownloadUrl(
            creatorId = userId,
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            fullPath = normalizedPath,
            ttl = ttl
        )
        return Url(StringUtil.chineseUrlEncode(url))
    }

    override fun getExternalDownloadUrlDirected(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int
    ): Url {
        return getExternalDownloadUrl(userId, projectId, artifactoryType, argPath, ttl, true)
    }

    override fun getExternalPlistDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info(
            "getExternalPlistDownloadUrl, userId: $userId, projectId: $projectId, " +
                    "artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed"
        )
        val normalizedPath = getNormalizePath(argPath, artifactoryType, userId, projectId)
        val url =
            StringUtil.chineseUrlEncode(
                "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/" +
                        "$artifactoryType/filePlist?path=$normalizedPath&x-devops-project-id=$projectId"
            )
        return Url(url)
    }

    private fun getNormalizePath(
        argPath: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        projectId: String
    ): String {
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId, message = MessageFormat.format(
                    MessageUtil.getMessageByLocale(
                        messageCode = ArtifactoryMessageCode.USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN,
                        language = I18nUtil.getLanguage(userId)
                    ),
                    userId,
                    projectId
                ))
            }
            ArtifactoryType.PIPELINE -> {
                val properties = bkRepoClient.listMetadata(
                    userId,
                    projectId,
                    RepoUtils.getRepoByType(artifactoryType),
                    normalizedPath
                )
                if (properties[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
                    throw CustomException(Response.Status.BAD_REQUEST,
                        MessageFormat.format(
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_METADATA_NOT_EXIST_DOWNLOAD_FILE_BY_SHARING,
                                language = I18nUtil.getLanguage(userId)
                            ),"pipelineId"
                        )
                        )
                }
                val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]
                pipelineService.validatePermission(
                    userId,
                    projectId,
                    pipelineId!!,
                    AuthPermission.DOWNLOAD,
                    MessageFormat.format(
                        MessageUtil.getMessageByLocale(
                            messageCode = ArtifactoryMessageCode.USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN,
                            language = I18nUtil.getLanguage(userId)
                        ),
                        userId,
                        projectId,
                        pipelineId
                    )
                )
            }
            // 镜像不支持下载
            ArtifactoryType.IMAGE -> throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(ArtifactoryType.IMAGE.name)
            )
        }
        return normalizedPath
    }

    override fun getPlistFile(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        directed: Boolean,
        experienceHashId: String?,
        organization: String?
    ): String {
        logger.info(
            "getPlistFile, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
                    "argPath: $argPath, directed: $directed, experienceHashId: $experienceHashId"
        )

        if (experienceHashId != null) {
            val check = client.get(ServiceExperienceResource::class).check(userId, experienceHashId, organization)
            if (!check.isOk() || !check.data!!) {
                throw CustomException(Response.Status.BAD_REQUEST, MessageUtil.getMessageByLocale(
                    messageCode = BK_NO_EXPERIENCE_PERMISSION,
                    language = I18nUtil.getLanguage(userId)
                ))
            }
        }

        val userName = if (experienceHashId != null) {
            val experience = client.get(ServiceExperienceResource::class).get(userId, projectId, experienceHashId)
            if (experience.isOk() && experience.data != null) {
                experience.data!!.creator
            } else {
                userId
            }
        } else {
            userId
        }

        val ipaExternalDownloadUrl = getExternalDownloadUrlDirected(userName, projectId, artifactoryType, argPath, ttl)
        val ipaExternalDownloadUrlEncode = StringUtil.chineseUrlEncode(ipaExternalDownloadUrl.url.replace(" ", ""))
        val fileProperties = bkRepoClient.listMetadata(
            userName,
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

    private fun getCdataStr(str: String): String = "<![CDATA[$str]]>"

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoAppService::class.java)
    }
}
