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
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PropertyUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.api.utils.ApiUrlUtils
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.File

@ApiPriority(priority = 9)
class TencentAtomArchiveResourceApi : AbstractBuildResourceApi(),
    AtomArchiveSDKApi {

    companion object {
        private const val AGENT_PROPERTIES_FILE_NAME = "/.agent.properties"
    }

    /**
     * 获取原子信息
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
        var path = "/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val queryParamSb = StringBuilder()
        atomStatus?.let { queryParamSb.append("atomStatus=$atomStatus&") }
        osName?.let { queryParamSb.append("osName=$osName&") }
        osArch?.let { queryParamSb.append("osArch=$osArch&") }
        convertOsFlag?.let { queryParamSb.append("convertOsFlag=$convertOsFlag&") }
        if (queryParamSb.isNotBlank()) {
            path = "$path?${queryParamSb.removeSuffix("&")}"
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
            "application/json; charset=utf-8".toMediaTypeOrNull(),
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

    override fun addAtomDockingPlatforms(
        atomCode: String,
        platformCodes: Set<String>
    ): Result<Boolean> {
        val path = "/store/api/build/store/docking/platforms/types/ATOM/codes/$atomCode/add"
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(platformCodes)
        )
        val request = buildPost(path, body)
        val responseContent = request(request, "添加插件对接平台信息失败")
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
            headers = headers
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
            headers = headers
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

    override fun getStorePkgRunEnvInfo(
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): Result<StorePkgRunEnvInfo?> {
        val path = "/store/api/build/store/pkg/envs/types/ATOM/languages/$language/versions/$runtimeVersion/get?" +
            "osName=$osName&osArch=$osArch"
        val request = buildGet(path)
        val responseContent = request(request, "get pkgRunEnvInfo fail")
        return objectMapper.readValue(responseContent)
    }
}
