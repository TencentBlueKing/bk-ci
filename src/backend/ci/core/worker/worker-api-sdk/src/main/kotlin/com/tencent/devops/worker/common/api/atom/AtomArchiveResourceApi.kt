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

package com.tencent.devops.worker.common.api.atom

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.ARCHIVE_PLUGIN_FILE_FAILED
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.GET_PLUGIN_ENV_INFO_FAILED
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.GET_PLUGIN_SENSITIVE_INFO_FAILED
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.UPDATE_PLUGIN_ENV_INFO_FAILED
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.constant.LOCALE_LANGUAGE
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.constant.StoreMessageCode.ADD_PLUGIN_PLATFORM_INFO_FAILED
import com.tencent.devops.store.constant.StoreMessageCode.GET_PLUGIN_LANGUAGE_ENV_INFO_FAILED
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_SOURCE
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.worker.common.api.archive.ArtifactoryBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_ARCHIVE_PLUGIN_FILE
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder

class AtomArchiveResourceApi : AbstractBuildResourceApi(), AtomArchiveSDKApi {

    private val realm = ArtifactoryBuildResourceApi().getRealm()

    /**
     * 获取插件信息
     */
    override fun getAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomStatus: Byte?,
        osName: String?,
        osArch: String?,
        convertOsFlag: Boolean?
    ): Result<AtomEnv> {
        var path = "/ms/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val queryParamSb = StringBuilder()
        atomStatus?.let { queryParamSb.append("atomStatus=$atomStatus&") }
        osName?.let { queryParamSb.append("osName=$osName&") }
        osArch?.let { queryParamSb.append("osArch=$osArch&") }
        convertOsFlag?.let { queryParamSb.append("convertOsFlag=$convertOsFlag&") }
        if (queryParamSb.isNotBlank()) {
            path = "$path?${queryParamSb.removeSuffix("&")}"
        }
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                GET_PLUGIN_ENV_INFO_FAILED,
                System.getProperty(LOCALE_LANGUAGE)
            )
        )
        return objectMapper.readValue(responseContent)
    }

    /**
     * 更新插件执行环境信息
     */
    override fun updateAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean> {
        val path = "/ms/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(atomEnvRequest)
        )
        val request = buildPut(path, body)
            val responseContent = request(
                request,
                MessageUtil.getMessageByLocale(
                    UPDATE_PLUGIN_ENV_INFO_FAILED,
                    System.getProperty(LOCALE_LANGUAGE)
                )
            )
        return objectMapper.readValue(responseContent)
    }

    /**
     * 获取插件插件敏感信息
     */
    override fun getAtomSensitiveConf(atomCode: String): Result<List<SensitiveConfResp>?> {
        val path = "/ms/store/api/build/store/sensitiveConf/types/ATOM/codes/$atomCode"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                GET_PLUGIN_SENSITIVE_INFO_FAILED,
                System.getProperty(LOCALE_LANGUAGE)
            )
        )
        return objectMapper.readValue(responseContent)
    }

    override fun archiveAtom(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ): String {
        uploadAtomPkgFile(
            atomCode = atomCode,
            atomVersion = atomVersion,
            file = file,
            destPath = destPath,
            buildVariables = buildVariables
        )
        return file.inputStream().use { ShaUtils.sha1InputStream(it) }
    }

    override fun uploadAtomPkgFile(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ) {
        val path = if (destPath.trim().endsWith(file.name)) {
            destPath.trim()
        } else {
            destPath.trim().removePrefix("/") + "/" + file.name
        }

        LoggerService.addNormalLine("${
            MessageUtil.getMessageByLocale(
                BK_ARCHIVE_PLUGIN_FILE,
                System.getProperty(LOCALE_LANGUAGE)
            )
        }>>> ${file.name}")

        val url = StringBuilder("/ms/artifactory/build/atom/result/$path")
        with(buildVariables) {
            url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
            url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
            url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
            url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(variables[PIPELINE_START_USER_ID] ?: "")}")
            url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
            url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")
        }

        val request = buildPut(url.toString(), RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file))
        val responseContent = request(request, "归档插件文件失败")
        try {
            val obj = JsonParser().parse(responseContent).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RemoteServiceException("${obj["code"]}")
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("AtomArchive fail: $responseContent")
        }
    }

    override fun uploadAtomStaticFile(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ) {
        // 过滤掉用../尝试遍历上层目录的操作
        val purePath = purePath(destPath)
        val fileName = file.name
        val path = if (purePath.endsWith(fileName)) purePath else "$purePath/$fileName"
        LoggerService.addNormalLine("upload file >>> $path")
        val fileType = FileTypeEnum.BK_PLUGIN_FE
        val url =
            "/ms/artifactory/api/build/artifactories/file/archive?fileType=$fileType&customFilePath=$purePath"
        val fileBody = RequestBody.create(MultipartFormData, file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()

        val request = buildPost(url, requestBody)
        val response = request(request, "upload file:$fileName fail")
        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") {
                throw RemoteServiceException("upload file:$fileName fail")
            }
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("archive fail: $response")
        }
    }

    override fun downloadAtom(
        projectId: String,
        atomFilePath: String,
        atomCreateTime: Long,
        file: File,
        isVmBuildEnv: Boolean
    ) {
        val filePath = when (realm) {
            REALM_LOCAL -> "$BK_CI_ATOM_DIR/$atomFilePath"
            REALM_BK_REPO -> "/bk-store/plugin/$atomFilePath"
            else -> throw IllegalArgumentException("unknown artifactory realm")
        }
        val path = "/ms/artifactory/api/build/artifactories/file/download?filePath=${
            URLEncoder.encode(
                filePath,
                "UTF-8"
            )
        }"
        val request = buildGet(path)
        download(request, file)
    }

    /**
     * 获取插件开发语言相关的环境变量
     */
    override fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: String,
        buildHostOs: String
    ): Result<List<AtomDevLanguageEnvVar>?> {
        val path = "/store/api/build/market/atom/dev/language/env/var/languages/$language/" +
            "types/$buildHostType/oss/$buildHostOs"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                GET_PLUGIN_LANGUAGE_ENV_INFO_FAILED,
                System.getProperty(LOCALE_LANGUAGE)
            )
        )
        return objectMapper.readValue(responseContent)
    }

    override fun addAtomDockingPlatforms(
        atomCode: String,
        platformCodes: Set<String>
    ): Result<Boolean> {
        val path = "/ms/store/api/build/store/docking/platforms/types/ATOM/codes/$atomCode/add"
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(platformCodes)
        )
        val request = buildPost(path, body)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                ADD_PLUGIN_PLATFORM_INFO_FAILED,
                System.getProperty(LOCALE_LANGUAGE)
            )
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getStorePkgRunEnvInfo(
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): Result<StorePkgRunEnvInfo?> {
        val path = "/ms/store/api/build/store/pkg/envs/types/ATOM/languages/$language/versions/$runtimeVersion/get?" +
            "osName=$osName&osArch=$osArch"
        val request = buildGet(path)
        val responseContent = request(request, "get pkgRunEnvInfo fail")
        return objectMapper.readValue(responseContent)
    }
}
