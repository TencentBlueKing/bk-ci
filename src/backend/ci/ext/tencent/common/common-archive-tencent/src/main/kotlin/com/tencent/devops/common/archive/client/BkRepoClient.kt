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
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_UID
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.generic.pojo.FileInfo
import com.tencent.bkrepo.repository.pojo.metadata.UserMetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeRenameRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.pojo.BkRepoData
import com.tencent.devops.common.archive.pojo.BkRepoFile
import com.tencent.devops.common.archive.pojo.QueryData
import com.tencent.devops.common.archive.pojo.QueryNodeInfo
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Paths
import javax.ws.rs.NotFoundException

class BkRepoClient constructor(
    private val objectMapper: ObjectMapper,
    private val commonConfig: CommonConfig
) {
    private fun getGatewaytUrl(): String {
        return HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
    }

    fun getFileSize(userId: String, projectId: String, repoName: String, path: String): NodeSizeInfo {
        logger.info("getFileSize, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/size/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
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
        logger.info("setMetadata, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, metadata: $metadata")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val requestData = UserMetadataSaveRequest(
            metadata = metadata
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
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
        logger.info("listMetadata, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/metadata/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
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
            .header(AUTH_HEADER_UID, userId)
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

    fun uploadFile(userId: String, projectId: String, repoName: String, path: String, inputStream: InputStream) {
        logger.info("uploadFile, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
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
            .header(AUTH_HEADER_UID, userId)
            .put(requestBody).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("upload file  failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("upload file failed")
            }
        }
    }

    fun uploadLocalFile(userId: String, projectId: String, repoName: String, path: String, file: File) {
        logger.info("uploadLocalFile, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, localFile: ${file.canonicalPath}")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/generic/$projectId/$repoName/${path.removePrefix("/")}"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .header(BK_REPO_OVERRIDE, "true")
            .put(RequestBody.create(MediaType.parse("application/octet-stream"), file))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("upload file failed, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("upload file failed")
            }
        }
    }

    fun delete(userId: String, projectId: String, repoName: String, path: String) {
        logger.info("delete, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("delete file failed, responseContent: ${response.body()!!.string()}")
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
            destFullPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("move file failed, responseContent: ${response.body()!!.string()}")
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
            destFullPath = toPath,
            destPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("copy file failed, responseContent: ${response.body()!!.string()}")
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
            .header(AUTH_HEADER_UID, userId)
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
            .header(AUTH_HEADER_UID, userId)
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
            .header(AUTH_HEADER_UID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.warn("file not found, repoName: $repoName, path: $path")
                    return null
                }
                logger.error("get file info failed, responseContent: $responseContent")
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
            .header(AUTH_HEADER_UID, userId)
            // .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.bytes()
            val mediaType = response.body()!!.contentType()!!
            if (!response.isSuccessful) {
                logger.error("get file content failed, responseContent: $responseContent")
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
        logger.info("matchBkRepoFile, userId: $userId, srcPath: $srcPath, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, isCustom: $isCustom")
        val result = mutableListOf<BkRepoFile>()
        val bkRepoData = getAllBkRepoFiles(userId, projectId, pipelineId, buildId, isCustom)
        val matcher = FileSystems.getDefault().getPathMatcher("glob:${srcPath.removePrefix("/")}")
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
        val repoName = if (param.custom) "custom" else "pipeline"
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
            "${getGatewaytUrl()}/bkrepo/api/service/generic/list/$projectId/custom?includeFolder=false&deep=true"
        } else {
            "${getGatewaytUrl()}/bkrepo/api/service/generic/list/$projectId/pipeline/$pipelineId/$buildId?includeFolder=false&deep=true"
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
                throw RuntimeException("获取文件失败")
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, BkRepoData::class.java)
            } catch (e: Exception) {
                logger.error("get bkrepo files fail: $responseBody")
                throw RuntimeException("获取文件失败")
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
            "fullPath: $fullPath, downloadUsers: $downloadUsers, downloadIps: $downloadIps, timeoutInSeconds: $timeoutInSeconds")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/share/$projectId/$repoName/${fullPath.removePrefix("/")}"
        val requestData = ShareRecordCreateRequest(
            authorizedUserList = downloadUsers,
            authorizedIpList = downloadIps,
            expireSeconds = timeoutInSeconds
        )
        val requestBody = objectMapper.writeValueAsString(requestData)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("create share uri failed, requestBody: $requestBody, responseContent: $responseContent")
                throw RuntimeException("create share uri failed")
            }

            val responseData = objectMapper.readValue<Response<ShareRecordInfo>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("create share uri failed: ${responseData.message}")
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
        logger.info("queryByRepoAndMetadata, userId: $userId, projectId: $projectId, repoNames: $repoNames, fileNames: $fileNames, metadata: $metadata, page: $page, pageSize: $pageSize")

        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        var ruleList = mutableListOf<Rule>(projectRule, repoRule)
        if (fileNames.isNotEmpty()) {
            val fileNameRule = Rule.NestedRule(fileNames.map { Rule.QueryRule("name", it, OperationType.MATCH) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(fileNameRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map { Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ) }.toMutableList())
            ruleList.add(metadataRule)
        }
        var rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, rule, page, pageSize)
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
        logger.info("queryByPathEqOrNameMatchOrMetadataEqAnd, userId: $userId, projectId: $projectId, repoNames: $repoNames, filePaths: $filePaths, fileNames: $fileNames, metadata: $metadata, page: $page, pageSize: $pageSize")

        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        var ruleList = mutableListOf<Rule>(projectRule, repoRule)
        if (filePaths.isNotEmpty()) {
            val filePathRule = Rule.NestedRule(filePaths.map { Rule.QueryRule("path", it, OperationType.EQ) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(filePathRule)
        }
        if (fileNames.isNotEmpty()) {
            val fileNameRule = Rule.NestedRule(fileNames.map { Rule.QueryRule("name", it, OperationType.MATCH) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(fileNameRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map { Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ) }.toMutableList(), , Rule.NestedRule.RelationType.AND)
            ruleList.add(metadataRule)
        }
        var rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, rule, page, pageSize)
    }

    fun queryByPattern(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        fullPathPatterns: List<String>,
        metadata: Map<String, String>
    ): List<QueryNodeInfo> {
        logger.info("queryByPattern, userId: $userId, projectId: $projectId, repoNames: $repoNames, fullPathPatterns: $fullPathPatterns, metadata: $metadata")

        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        var ruleList = mutableListOf<Rule>(projectRule, repoRule)
        if (fullPathPatterns.isNotEmpty()) {
            val fullPathRule = Rule.NestedRule(fullPathPatterns.map { Rule.QueryRule("fullPath", it, OperationType.MATCH) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(fullPathRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map { Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ) }.toMutableList())
            ruleList.add(metadataRule)
        }
        var rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, rule, 0, 10000)
    }

    private fun query(userId: String, rule: Rule, page: Int, pageSize: Int): List<QueryNodeInfo> {
        logger.info("query, userId: $userId, rule: $rule, page: $page, pageSize: $pageSize")
        val url = "${getGatewaytUrl()}/bkrepo/api/service/repository/api/node/query"
        val queryModel = QueryModel(
            page = PageLimit(0, 10000),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf(),
            rule = rule
        )

        val requestBody = objectMapper.writeValueAsString(queryModel)
        logger.info("requestBody: $requestBody")
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_UID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("query failed, responseContent: $responseContent")
                throw RuntimeException("query failed")
            }

            val responseData = objectMapper.readValue<Response<QueryData>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("query failed: ${responseData.message}")
            }

            return responseData.data!!.records
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private const val METADATA_PREFIX = "X-BKREPO-META-"
        // private const val BK_REPO_UID = "X-BKREPO-UID"
        private const val BK_REPO_OVERRIDE = "X-BKREPO-OVERWRITE"
    }
}