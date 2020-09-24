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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.AppService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
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
        logger.info("getExternalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId, message = "用户（$userId) 没有项目（$projectId）下载权限)")
            }
            ArtifactoryType.PIPELINE -> {
                val properties = bkRepoClient.listMetadata(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
                if (properties[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
                }
                val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]
                pipelineService.validatePermission(userId, projectId, pipelineId!!, AuthPermission.DOWNLOAD, "用户($userId)在项目($projectId)下没有流水线${pipelineId}下载构建权限")
            }
        }
        val url = bkRepoService.externalDownloadUrl(
            userId,
            projectId,
            artifactoryType,
            normalizedPath,
            ttl
        )
        return Url(StringUtil.chineseUrlEncode(url))
    }

    override fun getExternalDownloadUrlDirected(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int): Url {
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
        logger.info("getExternalPlistDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(argPath)
        when (artifactoryType) {
            ArtifactoryType.CUSTOM_DIR -> {
                pipelineService.validatePermission(userId, projectId, message = "用户（$userId) 没有项目（$projectId）下载权限)")
            }
            ArtifactoryType.PIPELINE -> {
                val properties = bkRepoClient.listMetadata(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
                if (properties[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
                }
                val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]
                pipelineService.validatePermission(userId, projectId, pipelineId!!, AuthPermission.DOWNLOAD, "用户($userId)在项目($projectId)下没有流水线${pipelineId}下载构建权限")
            }
        }
        val url = StringUtil.chineseUrlEncode("${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?path=$normalizedPath")
        return Url(url)
    }

    override fun getPlistFile(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean, experienceHashId: String?): String {
        logger.info("getPlistFile, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, directed: $directed, experienceHashId: $experienceHashId")
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
            userId,
            projectId,
            RepoUtils.getRepoByType(artifactoryType),
            argPath
        )
        var bundleIdentifier = ""
        var appTitle = ""
        var appVersion = ""
        fileProperties.forEach {
            when (it.key) {
                "bundleIdentifier" -> bundleIdentifier = it.value
                "appTitle" -> appTitle = it.value
                "appVersion" -> appVersion = it.value
                else -> null
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n" +
            "<dict>\n" +
            "    <key>items</key>\n" +
            "    <array>\n" +
            "        <dict>\n" +
            "            <key>assets</key>\n" +
            "            <array>\n" +
            "                <dict>\n" +
            "                    <key>kind</key>\n" +
            "                    <string>software-package</string>\n" +
            "                    <key>url</key>\n" +
            "                    <string>$ipaExternalDownloadUrlEncode</string>\n" +
            "                </dict>\n" +
            "            </array>\n" +
            "            <key>metadata</key>\n" +
            "            <dict>\n" +
            "                <key>bundle-identifier</key>\n" +
            "                <string>$bundleIdentifier</string>\n" +
            "                <key>bundle-version</key>\n" +
            "                <string>$appVersion</string>\n" +
            "                <key>title</key>\n" +
            "                <string>$appTitle</string>\n" +
            "                <key>kind</key>\n" +
            "                <string>software</string>\n" +
            "            </dict>\n" +
            "        </dict>\n" +
            "    </array>\n" +
            "</dict>\n" +
            "</plist>"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoAppService::class.java)
    }
}