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

package com.tencent.devops.scm.services

import com.fasterxml.jackson.core.type.TypeReference
import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.scm.code.git.api.GitOauthApi
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.MrCommentBody
import com.tencent.devops.scm.utils.GitCodeUtils
import com.tencent.devops.scm.utils.QualityUtils
import com.tencent.devops.scm.utils.RetryUtils
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import javax.ws.rs.core.Response

@Suppress("All")
@Service
class GitCiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCiService::class.java)
    }

    @Value("\${gitCI.clientId}")
    private lateinit var gitCIClientId: String

    @Value("\${gitCI.clientSecret}")
    private lateinit var gitCIClientSecret: String

    @Value("\${gitCI.url}")
    private lateinit var gitCIUrl: String

    @Value("\${gitCI.oauthUrl}")
    private lateinit var gitCIOauthUrl: String

    fun getGitCIMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?
    ): List<GitMember> {
        val url = "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "UTF8")}/members" +
            "?access_token=$token" +
            if (search != null) {
                "&query=$search"
            } else {
                ""
            } +
            "&page=$page" + "&per_page=$pageSize"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        RetryUtils.doRetryHttp(request).use { response ->
            val data = response.body!!.string()
            if (!response.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                    message = "(${response.code})${response.message}"
                )
            }
            return JsonUtil.to(data, object : TypeReference<List<GitMember>>() {})
        }
    }

    fun getBranch(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): List<String> {
        val url = "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "utf-8")}" +
            "/repository/branches?access_token=$token&page=$page&per_page=$pageSize" +
            if (search != null) {
                "&search=$search"
            } else {
                ""
            } +
            if (orderBy != null) {
                "&order_by=${orderBy.value}"
            } else {
                ""
            } +
            if (sort != null) {
                "&sort=${sort.value}"
            } else {
                ""
            }
        val res = mutableListOf<String>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        RetryUtils.doRetryHttp(request).use { response ->
            val data = response.body?.string() ?: return@use
            val branList = JsonParser.parseString(data).asJsonArray
            if (!branList.isJsonNull) {
                branList.forEach {
                    val branch = it.asJsonObject
                    if (branch.isJsonNull) {
                        return@forEach
                    }
                    res.add(if (branch["name"].isJsonNull) "" else branch["name"].asString)
                }
            }
        }
        return res
    }

    fun getGitCIFileContent(
        gitProjectId: String,
        filePath: String,
        token: String,
        ref: String,
        useAccessToken: Boolean
    ): String {
        logger.info("[$gitProjectId|$filePath|$ref] Start to get the git file content")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "utf-8")}/repository/blobs/" +
                "${URLEncoder.encode(ref, "UTF-8")}?filepath=${URLEncoder.encode(filePath, "UTF-8")}" +
                if (useAccessToken) {
                    "&access_token=$token"
                } else {
                    "&private_token=$token"
                }
            logger.info("request url: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            return RetryUtils.retryFun("getGitCIFileContent") {
                RetryUtils.doRetryLongHttp(request).use { response ->
                    if (!response.isSuccessful) {
                        throw CustomException(
                            status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                            message = "(${response.code})${response.message}"
                        )
                    }
                    response.body!!.string()
                }
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    fun getGitCIProjectInfo(
        gitProjectId: String,
        token: String,
        useAccessToken: Boolean = true
    ): Result<GitCIProjectInfo?> {
        val (url, request) = getProjectInfoRequest(gitProjectId, useAccessToken, token)
        return RetryUtils.retryFun("getGitCIProjectInfo") {
            RetryUtils.doRetryHttp(request).use { response ->
                logger.info("[url=$url]|getGitCIProjectInfo($gitProjectId) with response=$response")
                if (!response.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                        message = "(${response.code})${response.message}"
                    )
                }
                val data = response.body!!.string()
                Result(JsonUtil.to(data, GitCIProjectInfo::class.java))
            }
        }
    }

    fun getGitCodeProjectInfo(
        gitProjectId: String,
        token: String,
        useAccessToken: Boolean = true
    ): Result<GitCodeProjectInfo?> {
        logger.info("[gitProjectId=$gitProjectId]|getGitCodeProjectInfo")
        val (url, request) = getProjectInfoRequest(gitProjectId, useAccessToken, token)
        RetryUtils.doRetryHttp(request).use {
            val response = it.body!!.string()
            logger.info("[url=$url]|getGitCIProjectInfo with response=$response")
            if (!it.isSuccessful) return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
            return Result(JsonUtil.to(response, GitCodeProjectInfo::class.java))
        }
    }

    private fun getProjectInfoRequest(
        gitProjectId: String,
        useAccessToken: Boolean,
        token: String
    ): Pair<StringBuilder, Request> {
        val encodeId = URLEncoder.encode(gitProjectId, "utf-8") // 如果id为NAMESPACE_PATH则需要encode
        val str = "$gitCIUrl/api/v3/projects/$encodeId?" + if (useAccessToken) {
            "access_token=$token"
        } else {
            "private_token=$token"
        }
        val url = StringBuilder(str)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        return Pair(url, request)
    }

    fun getMergeRequestChangeInfo(gitProjectId: Long, token: String?, mrId: Long): Result<GitMrChangeInfo?> {
        logger.info("[gitProjectId=$gitProjectId]|getGitCodeProjectInfo")
        val url = "$gitCIUrl/api/v3/projects/$gitProjectId/merge_request/$mrId/changes?" +
            "access_token=$token"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        RetryUtils.doRetryHttp(request).use { response ->
            logger.info("[url=$url]|getMergeRequestChangeInfo with response=$response")
            if (!response.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                    message = "(${response.code})${response.message}"
                )
            }
            val data = response.body!!.string()
            return Result(JsonUtil.to(data, GitMrChangeInfo::class.java))
        }
    }

    fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeProjectInfo> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val url = "$gitCIUrl/api/v3/projects?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
            .addParams(
                mapOf(
                    "search" to search,
                    "order_by" to orderBy?.value,
                    "sort" to sort?.value,
                    "owned" to owned,
                    "min_access_level" to minAccessLevel?.level
                )
            )
        val res = mutableListOf<GitCodeProjectInfo>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        logger.info("getProjectList: $url")
        RetryUtils.doRetryHttp(request).use { response ->
            val data = response.body?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                return JsonUtil.to(data, object : TypeReference<List<GitCodeProjectInfo>>() {})
            }
        }
        return res
    }

    fun getGitCIAllMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        query: String?
    ): List<GitMember> {
        val newPage = if (page == 0) 1 else page
        val newPageSize = if (pageSize > 1000) 1000 else pageSize
        val url = "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "UTF8")}/members/all" +
            "?access_token=$token" +
            if (query != null) {
                "&query=$query"
            } else {
                ""
            } +
            "&page=$newPage" + "&per_page=$newPageSize"
        logger.info("getGitCIAllMembers request url: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        RetryUtils.doRetryHttp(request).use { response ->
            val data = response.body!!.string()
            if (!response.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                    message = "(${response.code})${response.message}"
                )
            }
            return JsonUtil.to(data, object : TypeReference<List<GitMember>>() {})
        }
    }

    fun getFileInfo(
        gitProjectId: String,
        token: String?,
        ref: String?,
        filePath: String?,
        useAccessToken: Boolean
    ): Result<GitCodeFileInfo> {
        val startEpoch = System.currentTimeMillis()
        try {
            val encodeId = URLEncoder.encode(gitProjectId, "utf-8")
            val url = "$gitCIUrl/api/v3/projects/$encodeId/repository/files" +
                if (useAccessToken) {
                    "?access_token=$token"
                } else {
                    "?private_token=$token"
                } +
                if (ref != null) {
                    "&ref=${URLEncoder.encode(ref, "UTF-8")}"
                } else {
                    ""
                } +
                if (filePath != null) {
                    "&file_path=${URLEncoder.encode(filePath, "UTF-8")}"
                } else {
                    ""
                }
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            RetryUtils.doRetryHttp(request).use { response ->
                logger.info("[url=$url]|getFileInfo with response=$response")
                if (!response.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                        message = "(${response.code})${response.message}"
                    )
                }
                val data = response.body!!.string()
                val result = try {
                    JsonUtil.to(data, GitCodeFileInfo::class.java)
                } catch (e: Throwable) {
                    logger.info("[url=$url]|getFileInfo to data error: ${e.message}")
                    throw CustomException(
                        status = Response.Status.BAD_REQUEST,
                        message = "File format error"
                    )
                }
                return Result(result)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    fun getChangeFileList(
        token: String,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean? = false,
        page: Int,
        pageSize: Int,
        useAccessToken: Boolean
    ): List<ChangeFileInfo> {
        val newPage = if (page == 0) 1 else page
        val newPageSize = if (pageSize > 10000) 10000 else pageSize
        val url = "${getUrlPrefix(gitProjectId)}/repository/compare/changed_files/list" +
            if (useAccessToken) {
                "?access_token=$token"
            } else {
                "?private_token=$token"
            }
                .addParams(
                    mapOf(
                        "from" to from,
                        "to" to to,
                        "straight" to straight,
                        "page" to newPage,
                        "per_page" to newPageSize
                    )
                )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        RetryUtils.doRetryLongHttp(request).use { response ->
            logger.info("[url=$url]|getChangeFileList with response=$response")
            if (!response.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                    message = "(${response.code})${response.message}"
                )
            }
            val data = response.body?.string() ?: return emptyList()
            return JsonUtil.to(data, object : TypeReference<List<ChangeFileInfo>>() {})
        }
    }

    fun addMrComment(
        token: String,
        gitProjectId: String,
        mrId: Long,
        mrBody: MrCommentBody
    ) {
        logger.info("$gitProjectId|$mrId|addMrComment")
        try {
            GitOauthApi().addMRComment(
                host = "$gitCIUrl/api/v3",
                token = token,
                projectName = gitProjectId,
                requestId = mrId,
                message = QualityUtils.getQualityReport(mrBody.reportData.first, mrBody.reportData.second)
            )
        } catch (e: Exception) {
            logger.warn("$gitProjectId add mr $mrId comment error: ${e.message}")
            val code = if (e is GitApiException) {
                e.code
            } else {
                Response.Status.BAD_REQUEST.statusCode
            }
            throw CustomException(
                status = Response.Status.fromStatusCode(code),
                message = "($code)${e.message}"
            )
        }
    }

    fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeGroup> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val url = "$gitCIUrl/api/v3/groups?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
            .addParams(
                mapOf(
                    "owned" to owned,
                    "min_access_level" to minAccessLevel?.level
                )
            )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        RetryUtils.doRetryHttp(request).use { response ->
            logger.info("[url=$url]|getProjectGroupList with response=$response")
            if (!response.isSuccessful) {
                throw GitCodeUtils.handleErrorMessage(response)
            }
            val data = response.body?.string()?.ifBlank { null } ?: return emptyList()
            return JsonUtil.to(data, object : TypeReference<List<GitCodeGroup>>() {})
        }
    }

    private fun getUrlPrefix(gitProjectId: String): String {
        return "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "UTF8")}"
    }

    private fun String.addParams(args: Map<String, Any?>): String {
        val sb = StringBuilder(this)
        args.forEach { (name, value) ->
            if (value != null) {
                sb.append("&$name=$value")
            }
        }
        return sb.toString()
    }
}
