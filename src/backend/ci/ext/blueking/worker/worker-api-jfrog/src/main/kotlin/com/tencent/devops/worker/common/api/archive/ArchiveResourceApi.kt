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

package com.tencent.devops.worker.common.api.archive

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.api.pojo.JFrogFileInfoList
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_APP_TITLE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_FULL_IMAGE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_IMAGE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_SOURCE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.IosUtils
import net.dongliu.apk.parser.ApkFile
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

@ApiPriority(priority = 1)
class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        var responseContent = ""
        val result = mutableListOf<String>()
        var jFrogFileInfoList: JFrogFileInfoList? = null
        try {
            var listFilesUrl = "/artifactory/archive/api/build"
            when (fileType) {
                FileTypeEnum.BK_ARCHIVE -> listFilesUrl = "$listFilesUrl/$pipelineId/$buildId?list&deep=1&listFolders=1"
                FileTypeEnum.BK_CUSTOM -> listFilesUrl = "/artifactory/custom/api/build/?list&deep=1&listFolders=1"
            }
            val request = buildGet(listFilesUrl)
            responseContent = request(request, "获取仓库文件失败")
            jFrogFileInfoList = JsonUtil.getObjectMapper().readValue(responseContent, JFrogFileInfoList::class.java)
        } catch (e: Exception) {
            LoggerService.addNormalLine("get archive files fail :\n$responseContent")
        }
        LoggerService.addNormalLine("scan file($customFilePath) in repo...")
        val matcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + customFilePath)
        jFrogFileInfoList!!.files.forEach { jfrogFile ->
            if (matcher.matches(Paths.get(jfrogFile.uri.removePrefix("/")))) {
                result.add(jfrogFile.uri)
            }
        }
        return result
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables) {

        val jfrogPath = destPath.removeSuffix("/") + "/" + file.name

        LoggerService.addNormalLine("upload file >>> $jfrogPath")

        val url = StringBuilder("/artifactory/custom/upload/build/$jfrogPath")
        with(buildVariables) {
            url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
            url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
            url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
            url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(variables[PIPELINE_START_USER_ID] ?: "")}")
            url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
            url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")
            setProps(file, url)
        }

        val request = buildPut(url.toString(), RequestBody.create(MediaType.parse("application/octet-stream"), file))
        val response = request(request, "上传自定义文件失败")
        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RuntimeException()
        } catch (e: Exception) {
            LoggerService.addNormalLine(e.message ?: "")
            throw RuntimeException("archive fail: $response")
        }
    }

    override fun uploadPipeline(file: File, buildVariables: BuildVariables) {
        LoggerService.addNormalLine("upload file >>> ${file.name}")
        // do upload
        val url = StringBuilder("/artifactory/archive/upload/build/${file.name}")
        buildVariables.buildEnvs
        with(buildVariables) {
            url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
            url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
            url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
            url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(variables[PIPELINE_START_USER_ID] ?: "")}")
            url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
            url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")

            setProps(file, url)
        }

        val request = buildPut(url.toString(), RequestBody.create(MediaType.parse("application/octet-stream"), file))

        val response = request(request, "上传流水线文件失败")

        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RuntimeException()
        } catch (e: Exception) {
            LoggerService.addNormalLine(e.message ?: "")
        }
    }

    override fun downloadCustomizeFile(uri: String, destPath: File) {
        val url = "/artifactory/custom/download/build/$uri"
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun downloadPipelineFile(pipelineId: String, buildId: String, uri: String, destPath: File) {
        val url = "/artifactory/archive/download/build/$pipelineId/$buildId$uri"
        val request = buildGet(url)
        download(request, destPath)
    }

    private fun setProps(file: File, url: StringBuilder) {
        try {
            if (file.name.endsWith(".ipa")) {
                val map = IosUtils.getIpaInfoMap(file)
                url.append(";$ARCHIVE_PROPS_APP_VERSION=${map["bundleVersion"] ?: ""}")
                url.append(";$ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER=${map["bundleIdentifier"] ?: ""}")
                url.append(";$ARCHIVE_PROPS_APP_APP_TITLE=${map["appTitle"] ?: ""}")
                url.append(";$ARCHIVE_PROPS_APP_IMAGE=${map["image"] ?: ""}")
                url.append(";$ARCHIVE_PROPS_APP_FULL_IMAGE=${map["fullImage"] ?: ""}")
            }
            if (file.name.endsWith(".apk")) {
                val apkFile = ApkFile(file)
                val meta = apkFile.apkMeta
                url.append(";$ARCHIVE_PROPS_APP_VERSION=${meta.versionName}")
                url.append(";$ARCHIVE_PROPS_APP_APP_TITLE=${meta.name}")
                url.append(";$ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER=${meta.packageName}")
            }
        } catch (e: Exception) {
            logger.error("get archive file properties fail", e)
            AlertUtils.doAlert(
                AlertLevel.HIGH,
                "get archive file properties fail",
                "url: $url, exception: ${e.message}"
            )
        }
    }

    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        val path = "/ms/artifactory/api/build/artifactories/$projectId/createDockerUser"
        val request = buildGet(path)
        val responseContent = request(request, "获取凭证信息失败")
        return jacksonObjectMapper().readValue(responseContent)
    }
}