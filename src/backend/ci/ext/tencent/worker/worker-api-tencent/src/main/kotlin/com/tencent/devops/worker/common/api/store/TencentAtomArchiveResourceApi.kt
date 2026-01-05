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
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PropertyUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.env.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.api.utils.ApiUrlUtils
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_ARCHIVE_PLUG_FILES
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_FAILED_ADD_INFORMATION
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_FAILED_GET_PLUG
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_FAILED_SENSITIVE_INFORMATION
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_FAILED_UPDATE_PLUG
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

@ApiPriority(priority = 9)
class TencentAtomArchiveResourceApi : AbstractBuildResourceApi(),
    AtomArchiveSDKApi {

    companion object {
        private const val AGENT_PROPERTIES_FILE_NAME = "/.agent.properties"
        private const val AUTH_HEADER_BKREPO_MODE = "X-DEVOPS-BKREPO-MODE"
        private const val BKREPO_MODE_DOWNLOAD = "dl"
        private const val DOWNLOAD_READ_TIMEOUT_SEC = 180L
    }

    /**
     * 下载请求参数
     */
    private data class DownloadRequest(
        val host: String,
        val bkrepoProjectName: String,
        val atomFilePath: String,
        val authFlag: Boolean,
        val cacheFlag: Boolean,
        val headers: MutableMap<String, String>,
        val file: File
    )

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
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_GET_PLUG,
                language = AgentEnv.getLocaleLanguage()
            )
        )
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
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_UPDATE_PLUG,
                language = AgentEnv.getLocaleLanguage()
            )
        )
        return objectMapper.readValue(responseContent)
    }

    /**
     * 获取原子插件敏感信息
     */
    override fun getAtomSensitiveConf(atomCode: String): Result<List<SensitiveConfResp>?> {
        val path = "/store/api/build/store/sensitiveConf/types/ATOM/codes/$atomCode"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_SENSITIVE_INFORMATION,
                language = AgentEnv.getLocaleLanguage()
            )
        )
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
        val path =
            "/store/api/build/market/atom/dev/language/env/var/languages/$language/types/$buildHostType/oss/$buildHostOs"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION,
                language = AgentEnv.getLocaleLanguage()
            )
        )
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
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_ADD_INFORMATION,
                language = AgentEnv.getLocaleLanguage()
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
        uploadAtomPkgFile(atomCode, atomVersion, file, destPath, buildVariables)
        return file.inputStream().use { ShaUtils.sha256InputStream(it) }
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
        LoggerService.addNormalLine(
            MessageUtil.getMessageByLocale(
                messageCode = BK_ARCHIVE_PLUG_FILES,
                language = AgentEnv.getLocaleLanguage()
            ) + " >>> ${file.name}"
        )
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

    /**
     * 下载插件文件
     * 实现域名降级机制：优先使用优选域名下载，失败后自动降级到默认域名
     * @param projectId 项目ID，用于权限验证和请求头设置
     * @param atomFilePath 插件文件在BKRepo中的存储路径
     * @param file 目标文件对象，下载内容将写入此文件
     * @param authFlag 是否启用认证标志，控制下载时的认证行为
     * @param queryCacheFlag 是否查询缓存标志，优化下载性能
     * @param containerType 容器类型，用于确定优选下载域名（IDC或DevNet环境）
     */
    override fun downloadAtom(
        projectId: String,
        atomFilePath: String,
        file: File,
        authFlag: Boolean,
        queryCacheFlag: Boolean,
        containerType: String?
    ) {
        // 获取当前环境类型
        val envType = AgentEnv.getEnv().name.lowercase()
        // 根据环境类型获取BKRepo项目名称配置
        val bkrepoProjectName = PropertyUtil.getPropertyValue(
            "bkrepo.store.project.name.$envType", AGENT_PROPERTIES_FILE_NAME
        )
        // 构建请求头，包含项目ID和BKRepo下载模式
        val headers = mutableMapOf(
            AUTH_HEADER_PROJECT_ID to projectId, AUTH_HEADER_BKREPO_MODE to BKREPO_MODE_DOWNLOAD
        )
        // 获取默认回退域名（当前网关域名），作为降级目标
        val fallbackHost = HomeHostUtil.getHost(AgentEnv.getGateway())
        // 根据容器类型获取优选下载域名并解析为主机地址
        val preferredHost = getPreferredDownloadHost(envType, containerType).let {
            HomeHostUtil.getHost(it)
        }
        // 构建下载请求参数对象
        val downloadRequest = DownloadRequest(
            host = preferredHost,
            bkrepoProjectName = bkrepoProjectName,
            atomFilePath = atomFilePath,
            authFlag = authFlag,
            cacheFlag = queryCacheFlag,
            headers = headers,
            file = file
        )
        // 优先使用新域名下载，失败后降级到旧域名
        try {
            // 尝试使用优选域名进行下载，优先保证性能
            doDownload(downloadRequest.copy(host = preferredHost))
            return
        } catch (ignored: Throwable) {
            // 优选域名下载失败，记录警告日志并继续执行降级逻辑
            logger.warn("Download from preferred host[$preferredHost] failed, fallback to default host: ${ignored.message}")
        }
        // 使用默认域名下载（降级机制），确保下载可靠性
        doDownload(downloadRequest.copy(host = fallbackHost))
    }

    /**
     * 获取优先使用的下载域名
     */
    private fun getPreferredDownloadHost(envType: String, containerType: String?): String {
        val propertyKey = if (containerType == NormalContainer.classType) {
            "bkrepo.file.idc.download.host.$envType"
        } else {
            "bkrepo.file.devnet.download.host.$envType"
        }
        return PropertyUtil.getPropertyValue(propertyKey, AGENT_PROPERTIES_FILE_NAME)
    }

    /**
     * 执行下载
     */
    private fun doDownload(request: DownloadRequest) {
        val bkrepoUrl = "${request.host}/repo/storge/build/atom/${request.bkrepoProjectName}/" +
                "bk-plugin/${request.atomFilePath}?authFlag=${request.authFlag}&queryCacheFlag=${request.cacheFlag}"
        try {
            downloadWithHeaders(bkrepoUrl, request.headers, request.file)
        } catch (ignored: RemoteServiceException) {
            // 当状态码为401或403时，移除BKREPO_MODE头后重试
            val httpStatus = ignored.httpStatus
            if (httpStatus == HTTP_401 || httpStatus == HTTP_403) {
                logger.warn("Download failed with $httpStatus, retry without BKREPO_MODE header")
                val retryHeaders = request.headers.toMutableMap().apply {
                    remove(AUTH_HEADER_BKREPO_MODE)
                }
                downloadWithHeaders(bkrepoUrl, retryHeaders, request.file)
            } else {
                throw ignored
            }
        }
    }

    /**
     * 使用指定headers执行下载
     */
    private fun downloadWithHeaders(url: String, headers: Map<String, String>, file: File) {
        val request = buildGet(url, headers)
        download(request = request, destPath = file, readTimeoutInSec = DOWNLOAD_READ_TIMEOUT_SEC)
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

