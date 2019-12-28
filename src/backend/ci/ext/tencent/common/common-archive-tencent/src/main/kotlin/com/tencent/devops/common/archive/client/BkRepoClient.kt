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

package com.tencent.devops.common.archive.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_USER_ID
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.generic.pojo.FileInfo
import com.tencent.bkrepo.generic.pojo.operate.FileSearchRequest
import com.tencent.bkrepo.repository.pojo.metadata.UserMetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeRenameRequest
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.pojo.BkRepoData
import com.tencent.devops.common.archive.pojo.BkRepoFile
import com.tencent.devops.common.archive.pojo.CreateShareUriRequest
import com.tencent.devops.common.archive.pojo.CreateShareUriResponse
import com.tencent.devops.common.service.config.CommonConfig
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Paths
import javax.ws.rs.NotFoundException

class BkRepoClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val commonConfig: CommonConfig
) {

    private fun getGatewaytUrl(): String {
        return if (commonConfig.devopsHostGateway!!.startsWith("http://")) {
            commonConfig.devopsHostGateway!!
        } else {
            "http://${commonConfig.devopsHostGateway!!}"
        }
    }

    fun getFileSize(userId: String, projectId: String, repoName: String, path: String): NodeSizeInfo {
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/size/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get file size failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("get file size failed")
            }

            val responseData = objectMapper.readValue<Response<NodeSizeInfo>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file size failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun setMetadata(userId: String, projectId: String, repoName: String, path: String, metadata: Map<String, String>) {
        logger.info("setMetadata, projectId: $projectId, repoName: $repoName, path: $path, metadata: $metadata")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val requestData = UserMetadataSaveRequest(
            metadata = metadata
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("set file metadata failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("set file metadata failed")
            }
        }
    }

    fun listMetadata(userId: String, projectId: String, repoName: String, path: String): Map<String, String> {
        logger.info("list metadata of, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("list file metadata failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("list file metadata failed")
            }

            val responseData = objectMapper.readValue<Response<Map<String, String>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("list file metadata failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun listFile(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        includeFolders: Boolean = false,
        deep: Boolean = false
    ): List<FileInfo> {
        logger.info("listFile, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, includeFolders: $includeFolders, deep: $deep")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/list/$projectId/$repoName/$path?deep=$deep&includeFolder=$includeFolders"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("list file failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<List<FileInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file info failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun searchFile(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        filePaterns: List<String>,
        metadatas: Map<String, String>,
        page: Int,
        pageSize: Int
    ): Page<NodeInfo> {
        logger.info("searchFile, userId: $userId, repoNames: $repoNames, filePaterns: $filePaterns, metadatas: $metadatas, page: $page, pageSize: $pageSize")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/search"
        val requestData = FileSearchRequest(
            projectId = projectId,
            repoNameList = repoNames,
            pathPattern = listOf(),
            metadataCondition = metadatas,
            page = page,
            size = pageSize
        )
        val requestBody = objectMapper.writeValueAsString(requestData)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("search file failed, requestBody: $requestBody, responseContent: $responseContent")
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<Page<NodeInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("search file failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun uploadFile(userId: String, projectId: String, repoName: String, path: String, inputStream: InputStream) {
        logger.info("uploadFile, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/$projectId/$repoName/$path"
        val requestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink?) {
                val source = Okio.source(inputStream)
                sink!!.writeAll(source)
            }

            override fun contentType(): MediaType? {
                return MediaType.parse("application/octet-stream")
            }
        }
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .put(requestBody).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("upload file  failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("upload file failed")
            }
        }
    }

    fun uploadLocalFile(userId: String, projectId: String, repoName: String, path: String, file: File) {
        logger.info("uploadLocalFile, projectId: $projectId, repoName: $repoName, path: $path, localFile: ${file.canonicalPath}")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/$projectId/$repoName/${path.removePrefix("/")}"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .header(BK_REPO_OVERRIDE, "true")
            .put(RequestBody.create(MediaType.parse("application/octet-stream"), file))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("upload file failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("upload file failed")
            }
        }
    }

    fun delete(userId: String, projectId: String, repo: String, path: String) {
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repo/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("delete file failed, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("delete file info failed")
            }
        }
    }

    fun move(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        // todo 校验path参数
        logger.info("move, userId: $userId, projectId: $projectId, repoName: $repoName, fromPath: $fromPath, toPath: $toPath")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/move"
        val requestData = UserNodeMoveRequest(
            srcProjectId = projectId,
            srcRepoName = repoName,
            srcFullPath = fromPath,
            destProjectId = projectId,
            destRepoName = repoName,
            destPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("move file failed, fromPath: $fromPath, toPath: $toPath, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("move file failed")
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
        // todo 校验path参数
        logger.info("copy, userId: $userId, fromProject: $fromProject, fromRepo: $fromRepo, fromPath: $fromPath, toProject: $toProject, toRepo: $toRepo, toPath: $toPath")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/copy"
        val requestData = UserNodeCopyRequest(
            srcProjectId = fromProject,
            srcRepoName = fromRepo,
            srcFullPath = fromPath,
            destProjectId = toProject,
            destRepoName = toRepo,
            destPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("copy file failed, fromPath: $fromPath, toPath: $toPath, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("copy file failed")
            }
        }
    }

    fun rename(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        // todo 校验path参数
        logger.info("rename, userId: $userId, projectId: $projectId, repoName: $repoName, fromPath: $fromPath, toPath: $toPath")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/rename"
        val requestData = UserNodeRenameRequest(projectId, repoName, fromPath, toPath)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("rename failed, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("rename failed")
            }
        }
    }

    fun mkdir(userId: String, projectId: String, repoName: String, path: String) {
        logger.info("mkdir, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(RequestBody.create(null, ""))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("mkdir failed, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("mkdir failed")
            }
        }
    }

    fun getFileDetail(userId: String, projectId: String, repoName: String, path: String): NodeDetail? {
        logger.info("getFileInfo, projectId:$projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.warn("file not found, repoName: $repoName, path: $path")
                    return null
                }
                logger.error("get file info failed, repoName: $repoName, path: $path, responseContent: $responseContent")
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<NodeDetail>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file info failed: ${responseData.message}")
            }
            return responseData.data!!
        }
    }

    fun getFileContent(userId: String, projectId: String, repoName: String, path: String): Pair<ByteArray, MediaType> {
        logger.info("getFileContent, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header(AUTH_HEADER_USER_ID, userId)
            // .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.bytes()
            val mediaType = response.body()!!.contentType()!!
            if (!response.isSuccessful) {
                logger.error("get file content failed, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
                throw RuntimeException("get file content failed")
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
        val result = mutableListOf<BkRepoFile>()
        val bkRepoData = getAllBkRepoFiles(userId, projectId, pipelineId, buildId, isCustom)
        val matcher = FileSystems.getDefault().getPathMatcher("glob:$srcPath")
        val pipelinePathPrefix = "/$pipelineId/$buildId/"
        bkRepoData.data?.forEach { bkrepoFile ->
            val repoPath = if (isCustom) {
                bkrepoFile.fullPath.removePrefix("/")
            } else {
                bkrepoFile.fullPath.removePrefix(pipelinePathPrefix)
            }
            if (matcher.matches(Paths.get(repoPath))) {
                bkrepoFile.displayPath = repoPath
                result.add(bkrepoFile)
            }
        }
        return result
    }

    fun getFileDownloadUrl(param: ArtifactorySearchParam): List<String> {
        logger.info("getFileDownloadUrl, param: $param")
        val repoName = if(param.custom) "custom" else "pipeline"
        val files = matchBkRepoFile(
            "",
            param.regexPath,
            param.projectId,
            param.pipelineId,
            param.buildId,
            param.custom
        )
        logger.info("match files: $files")
        return files.map {
            "${getGatewaytUrl()}/bkrepo/api/service/generic/${param.projectId}/$repoName${it.fullPath}"
        }
    }

    private fun getAllBkRepoFiles(userId: String, projectId: String, pipelineId: String, buildId: String, isCustom: Boolean): BkRepoData {
        logger.info("getAllBkrepoFiles, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, isCustom: $isCustom")
        var url = if (isCustom) {
            "${getGatewaytUrl()}/bkrepo/api/service/generic/list/$projectId/custom?includeFolder=true&deep=true"
        } else {
            "${getGatewaytUrl()}/bkrepo/api/service/generic/list/$projectId/pipeline/$pipelineId/$buildId?includeFolder=true&deep=true"
        }
        val request = Request.Builder()
            .url(url)
            .header("X-BKREPO-UID", userId)
            .get()
            .build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get bkrepo files fail: $responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, BkRepoData::class.java)
            } catch (e: Exception) {
                logger.error("get bkrepo files fail: $responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
        }
    }

    fun downloadFile(userId: String, projectId: String, repoName: String, fullPath: String, destFile: File) {
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/$projectId/$repoName/${fullPath.removePrefix("/")}"
        OkhttpUtils.downloadFile(url, destFile, mapOf("X-BKREPO-UID" to userId))
    }

    fun listFileByRegex(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        regex: String
    ): List<FileInfo> {
        logger.info("listFileByRegex, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, regex: $regex")
        val matcher = FileSystems.getDefault().getPathMatcher("glob:$regex")
        return listFile(userId, projectId, repoName, path, includeFolders = false, deep = true).filter {
            matcher.matches(Paths.get(it.fullPath.removePrefix("$path")))
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
        logger.info("listFileByPattern, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, repoName: $repoName, pathPattern: $pathPattern")
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
        user: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        repoName: String,
        pathPattern: String,
        destPath: String
    ): List<File> {
        val fileList = listFileByPattern(
            user,
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
            downloadFile(user, projectId, repoName, it.fullPath, destFile)
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
    ): String{
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/share/$projectId/$repoName/${fullPath.removePrefix("/")}"
        val requestData = CreateShareUriRequest(
            authorizedUserList = downloadUsers,
            authorizedIpList = downloadIps,
            expireSeconds = timeoutInSeconds
        )
        val requestBody = objectMapper.writeValueAsString(requestData)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("create share uri failed, requestBody: $requestBody, responseContent: $responseContent")
                throw RuntimeException("create share uri failed")
            }

            val responseData = objectMapper.readValue<Response<CreateShareUriResponse>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("create share uri failed: ${responseData.message}")
            }

            return responseData.data!!.shareUrl
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private const val METADATA_PREFIX = "X-BKREPO-META-"
        // private const val BK_REPO_UID = "X-BKREPO-UID"
        private const val BK_REPO_OVERRIDE = "X-BKREPO-OVERWRITE"
    }
}