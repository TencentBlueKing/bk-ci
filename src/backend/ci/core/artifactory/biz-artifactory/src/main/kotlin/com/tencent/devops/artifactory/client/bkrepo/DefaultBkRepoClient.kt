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

package com.tencent.devops.artifactory.client.bkrepo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.generic.pojo.FileInfo
import com.tencent.bkrepo.repository.pojo.metadata.UserMetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeRenameRequest
import com.tencent.bkrepo.repository.pojo.project.UserProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.UserRepoCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.devops.artifactory.pojo.bkrepo.ArtifactorySearchParam
import com.tencent.devops.artifactory.pojo.bkrepo.BkRepoFile
import com.tencent.devops.artifactory.pojo.bkrepo.QueryData
import com.tencent.devops.artifactory.pojo.bkrepo.QueryNodeInfo
import com.tencent.devops.artifactory.util.BkRepoUtils
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.file.FileSystems
import java.nio.file.Paths
import javax.ws.rs.NotFoundException

@Component
@Suppress("UNUSED", "LongParameterList", "LargeClass", "TooManyFunctions", "MagicNumber", "ThrowsCount")
class DefaultBkRepoClient constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${artifactory.realm:}")
    private var artifactoryRealm: String = ""

    @Value("\${artifactory.bkrepo.baseUrl:}")
    private var bkRepoBaseUrl: String = ""

    @Value("\${artifactory.bkrepo.authorization:}")
    private var bkRepoAuthorization: String = ""

    private fun getBkRepoUrl(): String {
        return bkRepoBaseUrl.removeSuffix("/")
    }

    fun useBkRepo(): Boolean {
        return BKREPO_REALM == artifactoryRealm
    }

    fun createBkRepoResource(userId: String, projectId: String): Boolean {
        if (BKREPO_REALM != artifactoryRealm) {
            logger.info("realm not bkrepo, skip create bkrepo resource")
            return false
        }

        return try {
            createProject(userId, projectId)
            createGenericRepo(userId, projectId, REPO_PIPELINE)
            createGenericRepo(userId, projectId, REPO_CUSTOM)
            createGenericRepo(userId, projectId, REPO_REPORT)
            true
        } catch (ignore: Exception) {
            logger.error("BKSystemErrorMonitor|BK-REPO|create repo resource error", ignore)
            false
        }
    }

    fun createProject(userId: String, projectId: String) {
        logger.info("createProject, userId: $userId, projectId: $projectId")
        val requestData = UserProjectCreateRequest(
            name = projectId,
            displayName = projectId,
            description = projectId
        )
        val request = Request.Builder()
            .url("${getBkRepoUrl()}/repository/api/project/create")
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            val responseData = objectMapper.readValue<Response<Any>>(responseContent)
            if (response.code() == 400 && responseData.code == 25102) {
                logger.warn("project[$projectId] already exists")
            } else if (!response.isSuccessful) {
                logger.warn("BKREPO_createProject_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}, responseContent: $responseContent")
                throw RemoteServiceException("create repo project failed: $responseContent", response.code())
            }
        }
    }

    fun createGenericRepo(userId: String, projectId: String, repoName: String) {
        logger.info("createRepo, userId: $userId, projectId: $projectId, repoName: $repoName")
        val requestData = UserRepoCreateRequest(
            category = RepositoryCategory.LOCAL,
            name = repoName,
            projectId = projectId,
            type = RepositoryType.GENERIC,
            public = false,
            description = "storage for devops ci $repoName"
        )
        val request = Request.Builder()
            .url("${getBkRepoUrl()}/repository/api/repo/create")
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            val responseData = objectMapper.readValue<Response<Any>>(responseContent)
            if (response.code() == 400 && responseData.code == 251004) {
                logger.warn("repo $projectId|$repoName already exists")
            } else if (!response.isSuccessful) {
                logger.warn("BKREPO_createGenericRepo_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}, responseContent: $responseContent")
                throw RemoteServiceException("create generic repo failed: $responseContent", response.code())
            }
        }
    }

    fun getFileSize(userId: String, projectId: String, repoName: String, path: String): NodeSizeInfo {
        logger.info("getFileSize, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/size/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/node/size/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("get file size failed, request: ${request.url()}, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("get file size failed: $path not found")
                }
                throw RemoteServiceException("get file size failed: $responseContent", response.code())
            }

            val responseData = objectMapper.readValue<Response<NodeSizeInfo>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("get file size failed: ${responseData.message}", response.code())
            }

            return responseData.data!!
        }
    }

    fun setMetadata(userId: String, projectId: String, repoName: String, path: String, metadata: Map<String, String>) {
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/metadata/$projectId/$repoName/$path"
        val requestData = UserMetadataSaveRequest(
            metadata = metadata
        )
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_setMetadata_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("set file metadata failed: $responseContent", response.code())
            }
        }
    }

    fun listMetadata(userId: String, projectId: String, repoName: String, path: String): Map<String, String> {
        logger.info("listMetadata, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/metadata/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("list file metadata failed, request: ${request.url()}, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("list file metadata failed: $path not found")
                }
                throw RemoteServiceException("list file metadata failed: $responseContent", response.code())
            }

            val responseData = objectMapper.readValue<Response<Map<String, String>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("list file metadata failed: ${responseData.message}", response.code())
            }

            return responseData.data!!
        }
    }

    @Deprecated(message = "api已废弃", replaceWith = ReplaceWith("listFilePage"))
    fun listFile(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        includeFolders: Boolean = false,
        deep: Boolean = false
    ): List<FileInfo> {
        val url = "${getBkRepoUrl()}/generic/list/$projectId/$repoName/$path?deep=$deep&includeFolder=$includeFolders"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("list file failed, request: ${request.url()}, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("list file failed: $path not found")
                }
                throw RemoteServiceException("get file info failed: $responseContent", response.code())
            }

            val responseData = objectMapper.readValue<Response<List<FileInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("get file info failed: ${responseData.message}", response.code())
            }

            return responseData.data!!
        }
    }

    fun listFilePage(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        includeFolders: Boolean = false,
        deep: Boolean = false,
        page: Int,
        pageSize: Int
    ): Page<NodeInfo> {
        val url = "${getBkRepoUrl()}/repository/api/node/page/$projectId/$repoName/$path" +
            "?deep=$deep&includeFolder=$includeFolders&includeMetadata=true&pageNumber=$page&pageSize=$pageSize"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("list file failed, request: ${request.url()}, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("list file failed: $path not found")
                }
                throw RemoteServiceException("get file info failed: $responseContent", response.code())
            }

            val responseData = objectMapper.readValue<Response<Page<NodeInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("get file info failed: ${responseData.message}", response.code())
            }

            return responseData.data!!
        }
    }

    fun uploadLocalFile(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        file: File,
        metadata: Map<String, String> = mapOf()
    ) {
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/generic/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/generic/$projectId/$repoName/$path"
        val header = mutableMapOf<String, String>()
        header[BK_REPO_UID] = userId
        header[AUTH_HEADER_DEVOPS_PROJECT_ID] = projectId
        header[BK_REPO_OVERRIDE] = "true"
        header["Authorization"] = bkRepoAuthorization
        metadata.forEach {
            header["$METADATA_PREFIX${it.key}"] = urlEncode(it.value)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .headers(Headers.of(header))
            .put(RequestBody.create(MediaType.parse("application/octet-stream"), file))
        val request = requestBuilder.build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_uploadLocalFile_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("upload file failed: $responseContent", httpStatus = response.code())
            }
        }

        try {
            if (repoName == BkRepoUtils.REPO_NAME_PIPELINE) {
                val pipelineId = DefaultPathUtils.resolvePipelineId(path)
                val buildId = DefaultPathUtils.resolveBuildId(path)
                if (!metadata["pipelineName"].isNullOrBlank()) {
                    setMetadata(userId = userId,
                        projectId = projectId,
                        repoName = repoName,
                        path = "/$pipelineId",
                        metadata = mapOf(METADATA_DISPLAY_NAME to metadata.getValue("pipelineName")))
                }
                if (!metadata["buildNum"].isNullOrBlank()) {
                    setMetadata(userId = userId,
                        projectId = projectId,
                        repoName = repoName,
                        path = "/$pipelineId/$buildId",
                        metadata = mapOf(METADATA_DISPLAY_NAME to metadata.getValue("buildNum")))
                }
            }
        } catch (ignore: Exception) {
            logger.warn("set pipeline displayName failed", ignore)
        }
    }

    private fun urlEncode(str: String?): String {
        return if (str.isNullOrBlank()) {
            ""
        } else {
            URLEncoder.encode(str, "UTF-8")
        }
    }

    fun delete(userId: String, projectId: String, repoName: String, path: String) {
        logger.info("delete, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_delete_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("delete file failed: $responseContent", response.code())
            }
        }
    }

    fun move(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/move"
        val url = "${getBkRepoUrl()}/repository/api/node/move"
        val requestData = UserNodeMoveCopyRequest(
            srcProjectId = projectId,
            srcRepoName = repoName,
            srcFullPath = fromPath,
            destProjectId = projectId,
            destRepoName = repoName,
            destFullPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_move_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("move file failed: $responseContent", response.code())
            }
        }
    }

    fun copy(
        userId: String,
        fromProject: String,
        fromRepo: String,
        fromPath: String,
        toProject: String,
        toRepo: String,
        toPath: String
    ) {
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/copy"
        val url = "${getBkRepoUrl()}/repository/api/node/copy"
        val requestData = UserNodeMoveCopyRequest(
            srcProjectId = fromProject,
            srcRepoName = fromRepo,
            srcFullPath = fromPath,
            destProjectId = toProject,
            destRepoName = toRepo,
            destFullPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, fromProject)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_copy_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("copy file failed: $responseContent", response.code())
            }
        }
    }

    fun rename(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        val url = "${getBkRepoUrl()}/repository/api/node/rename"
        val requestData = UserNodeRenameRequest(projectId, repoName, fromPath, toPath)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("BKREPO_rename_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}")
                throw RemoteServiceException("rename failed: $responseContent", httpStatus = response.code())
            }
        }
    }

    fun mkdir(userId: String, projectId: String, repoName: String, path: String) {
        logger.info("mkdir, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(RequestBody.create(null, ""))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("mkdir failed, request: ${request.url()}, responseContent: $responseContent")
                throw RemoteServiceException("mkdir failed: $responseContent", httpStatus = response.code())
            }
        }
    }

    fun getFileDetail(userId: String, projectId: String, repoName: String, path: String): NodeDetail? {
        logger.info("getFileInfo, projectId:$projectId, repoName: $repoName, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.warn("file not found, repoName: $repoName, path: $path")
                    return null
                }
                logger.warn("get file info failed, request: ${request.url()}, responseContent: $responseContent")
                throw RemoteServiceException("get file info failed: $responseContent", httpStatus = response.code())
            }

            val responseData = objectMapper.readValue<Response<NodeDetail>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("get file info failed: ${responseData.message}", response.code())
            }
            return responseData.data!!
        }
    }

    fun getFileContent(userId: String, projectId: String, repoName: String, path: String): Pair<ByteArray, MediaType> {
        logger.info("getFileContent, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/generic/$projectId/$repoName/$path"
        val url = "${getBkRepoUrl()}/generic/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.bytes()
            val mediaType = response.body()!!.contentType()!!
            if (!response.isSuccessful) {
                logger.warn("get file content failed, request: ${request.url()}, responseContent: $responseContent")
                throw RemoteServiceException("get file content failed: $responseContent", httpStatus = response.code())
            }
            return Pair(responseContent, mediaType)
        }
    }

    fun matchBkRepoFile(
        userId: String,
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<BkRepoFile> {
        val repoName: String
        val filePath: String
        val fileName: String
        if (isCustom) {
            val normalizedPath = "/${srcPath.removePrefix("./").removePrefix("/")}"
            repoName = "custom"
            filePath = DefaultPathUtils.getParentFolder(normalizedPath)
            fileName = DefaultPathUtils.getFileName(normalizedPath)
        } else {
            repoName = "pipeline"
            filePath = "/$pipelineId/$buildId/"
            fileName = DefaultPathUtils.getFileName(srcPath)
        }

        return queryByPathEqOrNameMatchOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            filePaths = listOf(filePath),
            fileNames = listOf(fileName),
            metadata = mapOf(),
            page = 0,
            pageSize = 10000
        ).map {
            BkRepoFile(
                fullPath = it.fullPath,
                displayPath = it.fullPath,
                size = it.size,
                folder = it.folder
            )
        }
    }

    fun getFileDownloadUrl(param: ArtifactorySearchParam): List<String> {
        logger.info("getFileDownloadUrl, param: $param")
        val repoName = if (param.custom) "custom" else "pipeline"
        val files = matchBkRepoFile(
            userId = "",
            srcPath = param.regexPath,
            projectId = param.projectId,
            pipelineId = param.pipelineId,
            buildId = param.buildId,
            isCustom = param.custom
        )
        logger.info("match files: $files")
        return files.map {
            "${getBkRepoUrl()}/generic/${param.projectId}/$repoName${it.fullPath}"
        }
    }

    fun downloadFile(userId: String, projectId: String, repoName: String, fullPath: String, destFile: File) {
        downloadFile(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            fullPath = fullPath,
            outputStream = destFile.outputStream()
        )
    }

    fun downloadFile(
        userId: String,
        projectId: String,
        repoName: String,
        fullPath: String,
        outputStream: OutputStream
    ) {
        val url = "${getBkRepoUrl()}/generic/$projectId/$repoName/${fullPath.removePrefix("/")}"
        val request = Request.Builder().url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (response.code() == 404) {
                logger.warn("file($url) not found")
                throw NotFoundException("File is not exist!")
            }
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn("download file($url) failed, code ${response.code()}, content: $responseContent")
                throw RemoteServiceException("download file failed", response.code(), responseContent)
            }
            FileCopyUtils.copy(response.body()!!.byteStream(), outputStream)
        }
    }

    fun listFileByPattern(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        repoName: String,
        pathPattern: String
    ): List<FileInfo> {
        return if (pathPattern.endsWith("/")) {
            val path = if (repoName == "pipeline") {
                "$pipelineId/$buildId/${pathPattern.removeSuffix("/")}"
            } else {
                pathPattern.removeSuffix("/")
            }
            listFile(userId, projectId, repoName, path, includeFolders = false, deep = false)
        } else {
            val f = File(pathPattern)
            val path = if (f.parent.isNullOrBlank()) {
                if (repoName == "pipeline") {
                    "$pipelineId/$buildId"
                } else {
                    ""
                }
            } else {
                if (repoName == "pipeline") {
                    "$pipelineId/$buildId/${f.parent}"
                } else {
                    f.parent
                }
            }
            val regex = f.name
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$regex")
            listFile(userId, projectId, repoName, path, includeFolders = false, deep = false).filter {
                matcher.matches(Paths.get(it.name))
            }
        }
    }

    fun downloadFileByPattern(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        repoName: String,
        pathPattern: String,
        destPath: String
    ): List<File> {
        logger.info("downloadFileByPattern, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, " +
            "buildId: $buildId, repoName: $repoName, pathPattern: $pathPattern, destPath: $destPath")
        val fileList = listFileByPattern(
            userId,
            projectId,
            pipelineId,
            buildId,
            repoName,
            pathPattern
        )
        logger.info("match files: ${fileList.map { it.fullPath }}")

        val destFiles = mutableListOf<File>()
        fileList.forEach {
            val destFile = File(destPath, it.name)
            downloadFile(userId, projectId, repoName, it.fullPath, destFile)
            destFiles.add(destFile)
            logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
        }
        return destFiles
    }

    fun createShareUri(
        userId: String,
        projectId: String,
        repoName: String,
        fullPath: String,
        downloadUsers: List<String>,
        downloadIps: List<String>,
        timeoutInSeconds: Long
    ): String {
        logger.info("createShareUri, userId: $userId, projectId: $projectId, repoName: $repoName, " +
            "fullPath: $fullPath, downloadUsers: $downloadUsers, downloadIps: $downloadIps," +
            " timeoutInSeconds: $timeoutInSeconds")
        val url = "${getBkRepoUrl()}/repository/api/share/$projectId/$repoName/${fullPath.removePrefix("/")}"
        val requestData = ShareRecordCreateRequest(
            authorizedUserList = downloadUsers,
            authorizedIpList = downloadIps,
            expireSeconds = timeoutInSeconds
        )
        val requestBody = objectMapper.writeValueAsString(requestData)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()?.string()
            if (!response.isSuccessful) {
                logger.warn("createShareUri_fail|http request failed, request: ${request.url()}, " +
                    "response.code: ${response.code()}, responseContent: $responseContent")
                throw RemoteServiceException("create share uri failed: $responseContent", response.code())
            }

            val responseData = objectMapper.readValue<Response<ShareRecordInfo>>(responseContent!!)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("create share uri failed: ${responseData.message}", response.code())
            }

            return responseData.data!!.shareUrl
        }
    }

    fun queryByNameAndMetadata(
        userId: String,
        projectId: String, // eq
        repoNames: List<String>, // eq or
        fileNames: List<String>, // match or
        metadata: Map<String, String>, // eq and
        page: Int,
        pageSize: Int
    ): List<QueryNodeInfo> {
        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        val ruleList = mutableListOf<Rule>(projectRule, repoRule, Rule.QueryRule("folder", false, OperationType.EQ))
        if (fileNames.isNotEmpty()) {
            val fileNameRule = Rule.NestedRule(fileNames.map {
                Rule.QueryRule("name", it, OperationType.MATCH)
            }.toMutableList(),
                Rule.NestedRule.RelationType.OR)
            ruleList.add(fileNameRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map {
                Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ)
            }.toMutableList())
            ruleList.add(metadataRule)
        }
        val rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, projectId, rule, page, pageSize)
    }

    fun queryByPathEqOrNameMatchOrMetadataEqAnd(
        userId: String,
        projectId: String, // eq
        repoNames: List<String>, // eq or
        filePaths: List<String>, // eq or
        fileNames: List<String>, // match or
        metadata: Map<String, String>, // eq and
        page: Int,
        pageSize: Int
    ): List<QueryNodeInfo> {
        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        val ruleList = mutableListOf<Rule>(projectRule, repoRule, Rule.QueryRule("folder", false, OperationType.EQ))
        if (filePaths.isNotEmpty()) {
            val filePathRule = Rule.NestedRule(filePaths.map {
                Rule.QueryRule("path", it, OperationType.EQ)
            }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(filePathRule)
        }
        if (fileNames.isNotEmpty()) {
            val fileNameRule = Rule.NestedRule(fileNames.map { fileName ->
                Rule.QueryRule("name", fileName, if (fileName.contains('*')) OperationType.MATCH else OperationType.EQ)
            }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(fileNameRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map {
                Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ)
            }.toMutableList(), Rule.NestedRule.RelationType.AND)
            ruleList.add(metadataRule)
        }
        val rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, projectId, rule, page, pageSize)
    }

    private fun getPathNameAndRule(path: String, name: String): Rule.NestedRule {
        val pathRule = Rule.QueryRule("path", path, OperationType.EQ)
        val nameRule = Rule.QueryRule("name", name, if (name.contains('*')) OperationType.MATCH else OperationType.EQ)
        return Rule.NestedRule(mutableListOf(pathRule, nameRule), Rule.NestedRule.RelationType.AND)
    }

    fun queryByPathNamePairOrMetadataEqAnd(
        userId: String,
        projectId: String, // eq
        repoNames: List<String>, // eq or
        pathNamePairs: List<Pair<String, String>>, // (path eq and name match) or
        metadata: Map<String, String> = mapOf(), // eq and
        page: Int,
        pageSize: Int
    ): List<QueryNodeInfo> {
        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        val ruleList = mutableListOf<Rule>(projectRule, repoRule, Rule.QueryRule("folder", false, OperationType.EQ))

        if (pathNamePairs.size == 1) {
            ruleList.add(getPathNameAndRule(pathNamePairs[0].first, pathNamePairs[0].second))
        } else if (pathNamePairs.size > 1) {
            val pathNameRules = mutableListOf<Rule>()
            pathNamePairs.forEach {
                pathNameRules.add(getPathNameAndRule(it.first, it.second))
            }
            ruleList.add(Rule.NestedRule(pathNameRules, Rule.NestedRule.RelationType.OR))
        }

        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map {
                Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ)
            }.toMutableList(), Rule.NestedRule.RelationType.AND)
            ruleList.add(metadataRule)
        }
        val rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, projectId, rule, page, pageSize)
    }

    private fun query(userId: String, projectId: String, rule: Rule, page: Int, pageSize: Int): List<QueryNodeInfo> {
        logger.info("query, userId: $userId, rule: $rule, page: $page, pageSize: $pageSize")
        // val url = "${getBkRepoUrl()}/bkrepo/api/service/repository/api/node/query"
        val url = "${getBkRepoUrl()}/repository/api/node/query"
        val queryModel = QueryModel(
            page = PageLimit(page, pageSize),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf(),
            rule = rule
        )

        val requestBody = objectMapper.writeValueAsString(queryModel)
        logger.info("requestBody: $requestBody")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bkRepoAuthorization)
            .header(BK_REPO_UID, userId)
            .header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("query failed, request: ${request.url()}, responseContent: $responseContent")
                throw RemoteServiceException("query failed: $responseContent", httpStatus = response.code())
            }

            val responseData = objectMapper.readValue<Response<QueryData>>(responseContent)
            if (responseData.isNotOk()) {
                throw RemoteServiceException("query failed: ${responseData.message}", httpStatus = response.code())
            }

            return responseData.data!!.records
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private const val BK_REPO_UID = "X-BKREPO-UID"
        private const val METADATA_PREFIX = "X-BKREPO-META-"
        private const val BK_REPO_OVERRIDE = "X-BKREPO-OVERWRITE"

        private const val METADATA_DISPLAY_NAME = "displayName"

        private const val BKREPO_REALM = "bkrepo"

        const val REPO_PIPELINE = "pipeline"
        const val REPO_CUSTOM = "custom"
        const val REPO_REPORT = "report"
    }
}
