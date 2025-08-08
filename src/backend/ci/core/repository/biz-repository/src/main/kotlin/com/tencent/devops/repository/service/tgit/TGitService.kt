/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.service.tgit

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.stringLimit
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitBranchCommit
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.code.git.api.GitTagCommit
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import java.net.URLEncoder
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.core.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils

@Service
@Suppress("ALL")
class TGitService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gitConfig: GitConfig
) : ITGitService {

    override fun getToken(userId: String, code: String): GitToken {
        logger.info("Start to get the token of user $userId by code $code")
        val startEpoch = System.currentTimeMillis()
        try {
            val tokenUrl =
                "${gitConfig.tGitUrl}/oauth/token?client_id=${gitConfig.tGitClientId}" +
                    "&client_secret=${gitConfig.tGitClientSecret}&code=$code" +
                    "&grant_type=authorization_code&" +
                    "redirect_uri=${urlEncode(gitConfig.tGitWebhookUrl)}"
            logger.info("getToken url>> $tokenUrl")
            val request = Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create("application/x-www-form-urlencoded;charset=utf-8".toMediaTypeOrNull(), ""))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    @BkTimed(extraTags = ["operation", "获取项目中成员信息"], value = "bk_tgit_api_time")
    override fun getUserInfoByToken(token: String, tokenType: TokenTypeEnum): GitUserInfo {
        logger.info("Start to get the user info by token[$token]")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = StringBuilder("${gitConfig.tGitApiUrl}/user")
            setToken(tokenType, url, token)
            logger.info("getToken url>> $url")
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitUserInfo::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    @BkTimed(extraTags = ["operation", "刷新token"], value = "bk_tgit_api_time")
    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        logger.info("Start to refresh the token of user $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.tGitUrl}/oauth/token" +
                "?client_id=${gitConfig.tGitClientId}" +
                "&client_secret=${gitConfig.tGitClientSecret}" +
                "&grant_type=refresh_token" +
                "&refresh_token=${accessToken.refreshToken}" +
                "&redirect_uri=${gitConfig.tGitWebhookUrl}"
            val request = Request.Builder()
                .url(url)
                .post(RequestBody.create("application/x-www-form-urlencoded;charset=utf-8".toMediaTypeOrNull(), ""))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to refresh the token")
        }
    }

    @BkTimed(extraTags = ["operation", "拉分支"], value = "bk_tgit_api_time")
    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitBranch> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get the $userId's $repository branch by accessToken")
        val repoId = urlEncode(repository)
        val url = "${gitConfig.tGitApiUrl}/projects/$repoId/repository/branches" +
            "?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull" +
            if (search != null) {
                "&search=$search"
            } else {
                ""
            }
        val res = mutableListOf<GitBranch>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    errorMessage = "(${response.code})${response.message}"
                )
            }
            val data = response.body?.string() ?: return@use
            val branList = JsonParser().parse(data).asJsonArray
            if (!branList.isJsonNull) {
                branList.forEach {
                    val branch = it.asJsonObject
                    val commit = branch["commit"].asJsonObject
                    if (!branch.isJsonNull && !commit.isJsonNull) {
                        res.add(
                            GitBranch(
                                name = if (branch["name"].isJsonNull) "" else branch["name"].asString,
                                commit = GitBranchCommit(
                                    id = if (commit["id"].isJsonNull) "" else commit["id"].asString,
                                    message = if (commit["message"].isJsonNull) "" else commit["message"].asString,
                                    authoredDate =
                                    if (commit["authored_date"].isJsonNull) "" else commit["authored_date"].asString,
                                    authorEmail =
                                    if (commit["author_email"].isJsonNull) "" else commit["author_email"].asString,
                                    authorName =
                                    if (commit["author_name"].isJsonNull) "" else commit["author_name"].asString,
                                    title =
                                    if (commit["title"].isJsonNull) "" else commit["title"].asString
                                )
                            )
                        )
                    }
                }
            }
        }
        return res
    }

    @BkTimed(extraTags = ["operation", "拉标签"], value = "bk_tgit_api_time")
    override fun getTag(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?
    ): List<GitTag> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get the $userId's $repository tag by page: $pageNotNull pageSize: $pageSizeNotNull")
        val repoId = urlEncode(repository)
        val url = "${gitConfig.tGitApiUrl}/projects/$repoId/repository/tags" +
            "?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
        val res = mutableListOf<GitTag>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body?.string() ?: return@use
            val tagList = JsonParser().parse(data).asJsonArray
            if (!tagList.isJsonNull) {
                tagList.forEach {
                    val tag = it.asJsonObject
                    val commit = tag["commit"].asJsonObject
                    if (!tag.isJsonNull && !commit.isJsonNull) {
                        res.add(
                            GitTag(
                                name = if (tag["name"].isJsonNull) {
                                    ""
                                } else tag["name"].asString,
                                message = if (tag["message"].isJsonNull) {
                                    ""
                                } else tag["message"].asString,
                                commit = GitTagCommit(
                                    id = if (commit["id"].isJsonNull) {
                                        ""
                                    } else commit["id"].asString,
                                    message = if (commit["message"].isJsonNull) {
                                        ""
                                    } else commit["message"].asString,
                                    authoredDate = if (commit["authored_date"].isJsonNull) {
                                        ""
                                    } else commit["authored_date"].asString,
                                    authorName = if (commit["author_name"].isJsonNull) {
                                        ""
                                    } else commit["author_name"].asString,
                                    authorEmail = if (commit["author_email"].isJsonNull) {
                                        ""
                                    } else commit["author_email"].asString
                                )
                            )
                        )
                    }
                }
            }
        }
        return res
    }

    @BkTimed(extraTags = ["operation", "GIT_FILE_CONTENT"], value = "bk_tgit_api_time")
    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        val startEpoch = System.currentTimeMillis()
        try {
            var url = "${gitConfig.tGitApiUrl}/projects/${urlEncode(repoName)}/repository/blobs/" +
                "${urlEncode(ref)}?filepath=${urlEncode(filePath)}"

            logger.info("[$repoName|$filePath|$authType|$ref] Start to get the git file content from $url")
            val request = if (authType == RepoAuthType.OAUTH) {
                url += "&access_token=$token"
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            } else {
                Request.Builder()
                    .url(url)
                    .get()
                    .header("PRIVATE-TOKEN", token)
                    .build()
            }
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "fail to get git file content with: ${it.code}): ${it.message}"
                    )
                }
                return it.stringLimit(
                    readLimit = MAX_FILE_SIZE
                )
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    override fun downloadGitFile(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String,
        response: HttpServletResponse
    ) {
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.gitApiUrl}/projects/${urlEncode(repoName)}/repository/" +
                    "blobs/${urlEncode(ref)}?" +
                    "filepath=${urlEncode(filePath)}&access_token=$token"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "fail to get git file with: ${it.code}): ${it.message}"
                    )
                }
                FileCopyUtils.copy(it.body!!.byteStream(), response.outputStream)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file")
        }
    }

    @BkTimed(extraTags = ["operation", "get_file_tree"], value = "bk_tgit_api_time")
    override fun getFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): List<GitFileInfo> {
        logger.info("[$gitProjectId|$path|$ref] Start to get the git file tree")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = StringBuilder(
                "${gitConfig.tGitApiUrl}/projects/" +
                    "${urlEncode(gitProjectId)}/repository/tree"
            )
            setToken(tokenType, url, token)
            with(url) {
                append(
                    "&path=${urlEncode(path)}"
                )
                append(
                    if (!ref.isNullOrBlank()) {
                        "&ref_name=${urlEncode(ref)}"
                    } else {
                        ""
                    }
                )
                append("&recursive=$recursive&access_token=$token")
            }
            logger.info("request url: $url")
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            return OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "(${it.code})${it.message}"
                    )
                }
                val data = it.body!!.string()
                JsonUtil.getObjectMapper().readValue(data) as List<GitFileInfo>
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file tree")
        }
    }

    @BkTimed(extraTags = ["operation", "git_project_list"], value = "bk_tgit_api_time")
    override fun getProjectList(
        accessToken: String,
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
        val url = (
            "${gitConfig.tGitApiUrl}/projects?access_token=$accessToken" +
            "&page=$pageNotNull&per_page=$pageSizeNotNull"
        )
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
        var result = res.toList()
        logger.info("getProjectList: $url")
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    errorMessage = "(${response.code})${response.message}"
                )
            }
            val data = response.body?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                result = JsonUtil.to(data, object : TypeReference<List<GitCodeProjectInfo>>() {})
            }
        }

        return result
    }

    private fun setToken(tokenType: TokenTypeEnum, url: StringBuilder, token: String) {
        if (TokenTypeEnum.OAUTH == tokenType) {
            url.append("?access_token=$token")
        } else {
            url.append("?private_token=$token")
        }
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

    override fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int,
        url: String
    ): List<ChangeFileInfo> {
        val host = GitUtils.getGitApiUrl(apiUrl = gitConfig.tGitApiUrl, repoUrl = url)
        val apiUrl = StringBuilder("$host/projects/${urlEncode(gitProjectId)}/" +
                    "repository/compare/changed_files/list")
        setToken(tokenType, apiUrl, token)
        val requestUrl = apiUrl.toString().addParams(
            mapOf(
                "from" to from,
                "to" to to,
                "straight" to straight,
                "page" to page,
                "pageSize" to pageSize
            )
        )
        val res = mutableListOf<ChangeFileInfo>()
        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()
        var result = res.toList()
        logger.info("getChangeFileList: $requestUrl")
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    errorMessage = "(${response.code})${response.message}"
                )
            }
            val data = response.body?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                result = JsonUtil.to(data, object : TypeReference<List<ChangeFileInfo>>() {})
            }
        }
        return result
    }

    private fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object {
        private val logger = LoggerFactory.getLogger(TGitService::class.java)
        private const val PAGE_SIZE = 100
        private const val SLEEP_MILLS_FOR_RETRY_500: Long = 500
        private const val MAX_FILE_SIZE = 1 * 1024 * 1024
    }
}
