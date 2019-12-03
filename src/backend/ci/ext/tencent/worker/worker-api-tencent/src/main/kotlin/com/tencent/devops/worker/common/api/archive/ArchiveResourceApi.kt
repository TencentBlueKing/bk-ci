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

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
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
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.IosUtils
import net.dongliu.apk.parser.ApkFile
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder
import java.nio.file.FileSystems
import java.nio.file.Paths

@ApiPriority(priority = 9)
class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {
    private val bkrepoMetaDataPrefix = "X-BKREPO-META-"
    private val bkrepoUid = "X-BKREPO-UID"
    private val bkrepoOverride = "X-BKREPO-OVERWRITE"

    private val jfrogResourceApi = JfrogResourceApi()

    fun isRepoGrey(): Boolean {
        val path = "/ms/artifactory/api/build/artifactories/checkRepoGray"
        val request = buildGet(path)
        val resultData: Result<Boolean> = objectMapper.readValue(request(request, "Fail to record the agent shutdown events"))
        if (resultData.isNotOk()) {
            throw RuntimeException("检查仓库灰度失败, message: ${resultData.message}")
        }
        return resultData.data!!
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        val result = mutableListOf<String>()
        if (isRepoGrey()) {
            val repoName = if (fileType == FileTypeEnum.BK_ARCHIVE) {
                "pipeline"
            } else {
                "custom"
            }
            val fileData = jfrogResourceApi.getAllBkRepoFiles(userId, projectId, repoName, pipelineId, buildId)
            LoggerService.addNormalLine("scan file($customFilePath) in repo...")
            val matcher = FileSystems.getDefault().getPathMatcher("glob:" + customFilePath)
            fileData.data.forEach { bkrepoFile ->
                val path = if (repoName == "pipeline") {
                    bkrepoFile.fullPath.removePrefix("/$pipelineId/$buildId/")
                } else {
                    bkrepoFile.fullPath.removePrefix("/")
                }

                if (matcher.matches(Paths.get(path))) {
                    result.add(bkrepoFile.fullPath)
                }
            }
        } else {
            val data = jfrogResourceApi.getAllFiles(buildId, pipelineId, buildId)
            LoggerService.addNormalLine("scan file($customFilePath) in repo...")
            val matcher = FileSystems.getDefault().getPathMatcher("glob:" + customFilePath)
            data.files.forEach { jfrogFile ->
                if (matcher.matches(Paths.get(jfrogFile.uri.removePrefix("/")))) {
                    result.add(jfrogFile.uri)
                }
            }
        }
        return result
    }

    private fun uploadJfrogCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
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

    fun uploadBkRepoCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        val bkrepoPath = destPath.removeSuffix("/") + "/" + file.name
        val url = StringBuilder("/bkrepo/api/build/generic/${buildVariables.projectId}/custom/$bkrepoPath")
        val header = mutableMapOf<String, String>()
        with(buildVariables) {
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_PROJECT_ID] = projectId
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_PIPELINE_ID] = pipelineId
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_BUILD_ID] = buildId
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_USER_ID] = variables[PIPELINE_START_USER_ID] ?: ""
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_BUILD_NO] = variables[PIPELINE_BUILD_NUM] ?: ""
            header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_SOURCE] = "pipeline"
            header[bkrepoUid] = variables[PIPELINE_START_USER_ID] ?: ""
            header[bkrepoOverride] = "true"
        }
        setBkRepoProps(file, header)

        logger.info("header: $header")

        val request = buildPut(url.toString(), RequestBody.create(MediaType.parse("application/octet-stream"), file), header)
        val response = request(request, "上传自定义文件失败")
        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "0") throw RuntimeException()
        } catch (e: Exception) {
            logger.error(e.message ?: "")
            throw RuntimeException("archive fail: $response")
        }
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        if (isRepoGrey()) {
            LoggerService.addNormalLine("user bkrepo: true")
            uploadBkRepoCustomize(file, destPath, buildVariables)
        } else {
            LoggerService.addNormalLine("user bkrepo: false")
            uploadJfrogCustomize(file, destPath, buildVariables)
        }
    }

    private fun uploadJfrogPipeline(file: File, buildVariables: BuildVariables) {
        LoggerService.addNormalLine("upload file >>> ${file.name}")

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

    fun uploadBkRepoPipeline(file: File, buildVariables: BuildVariables) {
        logger.info("upload file >>> ${file.name}")
        val url = "/bkrepo/api/build/generic/${buildVariables.projectId}/pipeline/${buildVariables.pipelineId}/${buildVariables.buildId}/${file.name}"

        val header = mutableMapOf<String, String>()
        with(buildVariables) {
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PROJECT_ID] = projectId
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID] = pipelineId
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID] = buildId
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_USER_ID] = variables[PIPELINE_START_USER_ID]
                ?: ""
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO] = variables[PIPELINE_BUILD_NUM]
                ?: ""
            header[bkrepoMetaDataPrefix + com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_SOURCE] = "pipeline"
            header[bkrepoUid] = variables[PIPELINE_START_USER_ID] ?: ""
            header[bkrepoOverride] = "true"
        }
        setBkRepoProps(file, header)

        val request = buildPut(url, RequestBody.create(MediaType.parse("application/octet-stream"), file), header)
        val response = request(request, "上传流水线文件失败")

        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "0") throw RuntimeException()
        } catch (e: Exception) {
            logger.error(e.message ?: "")
        }
    }

    override fun uploadPipeline(file: File, buildVariables: BuildVariables) {
        if (isRepoGrey()) {
            LoggerService.addNormalLine("user bkrepo: true")
            uploadBkRepoPipeline(file, buildVariables)
        } else {
            LoggerService.addNormalLine("user bkrepo: false")
            uploadJfrogPipeline(file, buildVariables)
        }
    }

    private fun downloadJfrogCustomizeFile(
        userId: String,
        projectId: String,
        uri: String,
        destPath: File
    ) {
        val url = "/jfrog/storage/build/custom$uri"
        val request = buildGet(url)
        download(request, destPath)
    }

    private fun downloadBkRepoFile(user: String, projectId: String, repoName: String, fullpath: String, destPath: File) {
        val url = "/bkrepo/api/build/generic/$projectId/$repoName$fullpath"
        var header = HashMap<String, String>()
        header.set("X-BKREPO-UID", user)
        val request = buildGet(url, header)
        download(request, destPath)
    }

    override fun downloadCustomizeFile(
        userId: String,
        projectId: String,
        uri: String,
        destPath: File
    ) {
        if (isRepoGrey()) {
            LoggerService.addNormalLine("user bkrepo: true")
            downloadBkRepoFile(userId, projectId, "custom", uri, destPath)
        } else {
            LoggerService.addNormalLine("user bkrepo: false")
            downloadJfrogCustomizeFile(userId, projectId, uri, destPath)
        }
    }

    private fun downloadJfrogPipelineFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        uri: String,
        destPath: File
    ) {
        val url = "/jfrog/storage/build/archive/$pipelineId/$buildId$uri"
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun downloadPipelineFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        uri: String,
        destPath: File
    ) {
        if (isRepoGrey()) {
            LoggerService.addNormalLine("user bkrepo: true")
            downloadBkRepoFile(userId, projectId, "pipeline", uri, destPath)
        } else {
            LoggerService.addNormalLine("user bkrepo: false")
            downloadJfrogPipelineFile(userId, projectId, pipelineId, buildId, uri, destPath)
        }
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

    private fun setBkRepoProps(file: File, header: MutableMap<String, String>) {
        try {
            if (file.name.endsWith(".ipa")) {
                val map = IosUtils.getIpaInfoMap(file)
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_VERSION] = tryEncode(map["bundleVersion"])
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER] = tryEncode(map["bundleIdentifier"])
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_APP_TITLE] = tryEncode(map["appTitle"])
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_IMAGE] = tryEncode(map["image"])
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_FULL_IMAGE] = tryEncode(map["fullImage"])
            }
            if (file.name.endsWith(".apk")) {
                val apkFile = ApkFile(file)
                val meta = apkFile.apkMeta
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_VERSION] = tryEncode(meta.versionName)
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_APP_TITLE] = tryEncode(meta.name)
                header[bkrepoMetaDataPrefix + ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER] = tryEncode(meta.packageName)
            }
        } catch (e: Exception) {
            logger.error("Fail to get the props of file - (${file.absolutePath})", e)
        }
    }

    private fun tryEncode(str: String?): String {
        return if (str.isNullOrBlank()) {
            ""
        } else {
            URLEncoder.encode(str, "UTF-8")
        }
    }
}