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
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
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
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder

@ApiPriority(priority = 9)
class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {
    private val bkrepoMetaDataPrefix = "X-BKREPO-META-"
    private val bkrepoUid = "X-BKREPO-UID"
    private val bkrepoOverride = "X-BKREPO-OVERWRITE"

    private val jfrogResourceApi = JfrogResourceApi()
    private val bkrepoResourceApi = BkRepoResourceApi()

    fun isRepoGrey(): Boolean {
        return true
    }

    private fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    private fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        var repoName: String
        var filePath: String
        var fileName: String
        if (fileType == FileTypeEnum.BK_CUSTOM) {
            repoName = "custom"
            val normalizedPath = "/${customFilePath!!.removePrefix("./").removePrefix("/")}"
            filePath = getParentFolder(normalizedPath)
            fileName = getFileName(normalizedPath)
        } else {
            repoName = "pipeline"
            filePath = "/$pipelineId/$buildId/"
            fileName = getFileName(customFilePath!!)
        }

        return bkrepoResourceApi.queryByPathEqOrNameMatchOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            filePaths = listOf(filePath),
            fileNames = listOf(fileName),
            metadata = mapOf(),
            page = 0,
            pageSize = 10000
        ).map { it.fullPath }
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
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "archive fail: $response"
            )
        }
    }

    private fun uploadBkRepoCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
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
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "archive fail: $response"
            )
        }
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        uploadBkRepoCustomize(file, destPath, buildVariables)
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
        uploadBkRepoPipeline(file, buildVariables)
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
        downloadBkRepoFile(userId, projectId, "custom", uri, destPath)
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
        downloadBkRepoFile(userId, projectId, "pipeline", uri, destPath)
    }

    /*
     * 此处绑定了jfrog的plugin实现接口，用于给用户颁发临时密钥用于docker push
     */
    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        val path = "/dockerbuild/credential"
        val request = buildGet(path)
        val responseContent = request(request, "获取凭证信息失败")
        return jacksonObjectMapper().readValue(responseContent)
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

    override fun uploadFile(
        url: String,
        destPath: String,
        file: File,
        headers: Map<String, String>?
    ): Result<Boolean> {
        LoggerService.addNormalLine("upload file url >>> $url")
        val fileBody = RequestBody.create(MultipartFormData, file)
        val fileName = file.name
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()
        val request = buildPost(url, requestBody, headers ?: emptyMap())
        val responseContent = request(request, "upload file:$fileName fail")
        return objectMapper.readValue(responseContent)
    }
}