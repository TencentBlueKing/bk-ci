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

package com.tencent.devops.worker.common.api.store

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PropertyUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_SOURCE
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.api.utils.ApiUrlUtils
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.TaskUtil
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

@ApiPriority(priority = 9)
class TencentAtomArchiveResourceApi : AbstractBuildResourceApi(),
    AtomArchiveSDKApi {

    companion object {
        private const val AGENT_PROPERTIES_FILE_NAME = "/.agent.properties"
        private const val RELEASE_STAGE_KEY = "release.stage"
    }

    /**
     * 获取原子信息
     */
    override fun getAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomStatus: Byte?
    ): Result<AtomEnv> {
        var path = "/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        if (atomStatus != null) {
            path = "$path?atomStatus=$atomStatus"
        }
        val request = buildGet(path)
        val responseContent = request(request, "获取插件执行环境信息失败")
        return objectMapper.readValue(responseContent)
    }

    /**
     * 更新原子执行环境信息
     */
    override fun updateAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean> {
        val path = "/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            objectMapper.writeValueAsString(atomEnvRequest)
        )
        val request = buildPut(path, body)
        val responseContent = request(request, "更新插件执行环境信息失败")
        return objectMapper.readValue(responseContent)
    }

    /**
     * 获取原子插件敏感信息
     */
    override fun getAtomSensitiveConf(atomCode: String): Result<List<SensitiveConfResp>?> {
        val path = "/store/api/build/store/sensitiveConf/types/ATOM/codes/$atomCode"
        val request = buildGet(path)
        val responseContent = request(request, "获取插件敏感信息失败")
        return objectMapper.readValue(responseContent)
    }

    /**
     * 获取插件开发语言相关的环境变量
     */
    override fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: String,
        buildHostOs: String
    ): Result<List<AtomDevLanguageEnvVar>?> {
        val path = "/store/api/build/market/atom/dev/language/env/var/languages/$language/types/$buildHostType/oss/$buildHostOs"
        val request = buildGet(path)
        val responseContent = request(request, "获取插件开发语言相关的环境变量信息失败")
        return objectMapper.readValue(responseContent)
    }

    override fun archiveAtom(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ): String {
        uploadAtomPkgFile(atomCode, atomVersion, file, destPath, buildVariables)
        return file.inputStream().use { ShaUtils.sha1InputStream(it) }
    }

    override fun uploadAtomPkgFile(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ) {
        val uploadFilePath = if (destPath.trim().endsWith(file.name)) {
            destPath.trim()
        } else {
            destPath.trim().removePrefix("/") + "/" + file.name
        }
        val userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: ""
        LoggerService.addNormalLine("归档插件文件 >>> ${file.name}")
        // 上传至jfrog(插件迁移需求发布到灰度阶段还需继续把插件文件上传到jfrog)
        if (PropertyUtil.getPropertyValue(RELEASE_STAGE_KEY, AGENT_PROPERTIES_FILE_NAME) != "prod") {
            val url = StringBuilder("/atom/result/$uploadFilePath")
            with(buildVariables) {
                url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
                url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
                url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
                url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(userId)}")
                url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
                url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")
            }

            val request = buildPut(
                path = url.toString(),
                requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file)
            )
            val responseContent = request(request, "归档插件文件失败")
            try {
                val obj = JsonParser().parse(responseContent).asJsonObject
                if (obj.has("code") && obj["code"].asString != "200") throw RuntimeException()
            } catch (e: Exception) {
                LoggerService.addNormalLine(e.message ?: "")
                throw RuntimeException("AtomArchive fail: $responseContent")
            }
        }
        // 上传至bkrepo
        val uploadFileUrl = ApiUrlUtils.generateStoreUploadFileUrl(
            repoName = BkRepoEnum.PLUGIN.repoName,
            projectId = buildVariables.projectId,
            storeType = StoreTypeEnum.ATOM,
            storeCode = atomCode,
            version = atomVersion,
            destPath = uploadFilePath
        )
        val headers = mapOf(AUTH_HEADER_USER_ID to userId)
        val uploadResult = ApiFactory.create(ArchiveSDKApi::class).uploadFile(
            url = uploadFileUrl,
            file = file,
            headers = headers,
            isVmBuildEnv = TaskUtil.isVmBuildEnv(buildVariables.containerType)
        )
        logger.info("uploadAtomPkgFileResult: $uploadResult")
        val uploadFlag = uploadResult.data
        if (uploadFlag == null || !uploadFlag) {
            throw TaskExecuteException(
                errorMsg = "upload file:${file.name} fail",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
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
        val uploadFilePath = if (purePath.endsWith(fileName)) purePath else "$purePath/$fileName"
        LoggerService.addNormalLine("upload file >>> $uploadFilePath")
        // 上传至jfrog(插件迁移需求发布到灰度阶段还需继续把插件文件上传到jfrog)
        if (PropertyUtil.getPropertyValue(RELEASE_STAGE_KEY, AGENT_PROPERTIES_FILE_NAME) != "prod") {
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
                if (obj.has("code") && obj["code"].asString != "200") throw RemoteServiceException("upload file:$fileName fail")
            } catch (ignored: Exception) {
                LoggerService.addNormalLine(ignored.message ?: "")
                throw RemoteServiceException("archive fail: $response")
            }
        }
        // 上传至bkrepo
        val uploadFileUrl = ApiUrlUtils.generateStoreUploadFileUrl(
            repoName = BkRepoEnum.STATIC.repoName,
            projectId = buildVariables.projectId,
            storeType = StoreTypeEnum.ATOM,
            storeCode = atomCode,
            version = atomVersion,
            destPath = "bk-store/bk-plugin-fe/$uploadFilePath"
        )
        val headers = mapOf(AUTH_HEADER_USER_ID to (buildVariables.variables[PIPELINE_START_USER_ID] ?: ""))
        val uploadResult = ApiFactory.create(ArchiveSDKApi::class).uploadFile(
            url = uploadFileUrl,
            file = file,
            headers = headers,
            isVmBuildEnv = TaskUtil.isVmBuildEnv(buildVariables.containerType)
        )
        logger.info("uploadAtomStaticFileUrlResult: $uploadResult")
        val uploadFlag = uploadResult.data
        if (uploadFlag == null || !uploadFlag) {
            throw TaskExecuteException(
                errorMsg = "upload file:${file.name} fail",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
    }

    override fun downloadAtom(
        projectId: String,
        atomFilePath: String,
        atomCreateTime: Long,
        file: File,
        isVmBuildEnv: Boolean
    ) {
        val envType = AgentEnv.getEnv().name.toLowerCase()
        val bkrepoProjectNameKey = "bkrepo.store.project.name.$envType"
        val bkrepoProjectName = PropertyUtil.getPropertyValue(bkrepoProjectNameKey, AGENT_PROPERTIES_FILE_NAME)
        val bkrepoUrl = "${HomeHostUtil.getHost(AgentEnv.getGateway())}/repo/storge/build/atom/$bkrepoProjectName/" +
            "bk-plugin/$atomFilePath"
        val request = buildGet(bkrepoUrl, mapOf(AUTH_HEADER_PROJECT_ID to projectId))
        download(request, file)
    }
}
