/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_APP_TITLE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_FULL_IMAGE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_IMAGE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.IosUtils
import net.dongliu.apk.parser.ApkFile
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {

    override fun getFileDownloadUrls(
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        val purePath = if (customFilePath != null) {
            purePath(customFilePath).toString()
        } else {
            customFilePath
        }

        val artifactoryType = when (fileType) {
            FileTypeEnum.BK_ARCHIVE -> ArtifactoryType.PIPELINE
            FileTypeEnum.BK_CUSTOM -> ArtifactoryType.CUSTOM_DIR
            else -> ArtifactoryType.CUSTOM_DIR
        }
        val url =
            "/ms/artifactory/api/build/artifactories/pipeline/$pipelineId/build/$buildId/file/download/urls/get?fileType=$fileType&customFilePath=$purePath"
        val request = buildGet(url)
        val response = request(request, "获取下载链接请求出错")
        val result = try {
            objectMapper.readValue<Result<GetFileDownloadUrlsResponse?>>(response)
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("archive fail: $response")
        }
        if (result.isNotOk()) {
            throw RemoteServiceException(result.message ?: "获取下载链接请求出错，${result.status.toLong()}")
        }

        return result.data?.fileUrlList ?: emptyList()
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        val jfrogPath = destPath.removeSuffix("/") + "/" + file.name

        LoggerService.addNormalLine("upload file >>> $jfrogPath")

        val url = StringBuilder("/custom/result/$jfrogPath")
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
        val url = StringBuilder("/archive/result/${file.name}")
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
        val url = if (uri.startsWith("http://") || uri.startsWith("https://")) {
            uri
        } else {
            "/ms/artifactory/api/build/artifactories/file/archive/download?fileType=${FileTypeEnum.BK_CUSTOM}&customFilePath=$uri"
        }
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun downloadPipelineFile(pipelineId: String, buildId: String, uri: String, destPath: File) {
        val url = if (uri.startsWith("http://") || uri.startsWith("https://")) {
            uri
        } else {
            "/ms/artifactory/api/build/artifactories/file/archive/download?fileType=${FileTypeEnum.BK_ARCHIVE}&customFilePath=$uri"
        }
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        return hashMapOf()
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
            LoggerService.addYellowLine("Fail to get the props of file - (${file.absolutePath})")
            logger.error("Fail to get the props of file - (${file.absolutePath})", e)
        }
    }
}