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

package com.tencent.devops.repository.service.scm

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.stringLimit
import com.tencent.devops.common.api.util.script.CommonScriptUtils
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.gitlab.GitlabFileInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.utils.scm.GitCodeUtils
import com.tencent.devops.scm.code.git.CodeGitOauthCredentialSetter
import com.tencent.devops.scm.code.git.CodeGitUsernameCredentialSetter
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitBranchCommit
import com.tencent.devops.scm.code.git.api.GitOauthApi
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.code.git.api.GitTagCommit
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.enums.GitProjectsOrderBy
import com.tencent.devops.scm.enums.GitSortAscOrDesc
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Base64
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils

@Service
@Suppress("ALL")
class GitService @Autowired constructor(
    private val gitConfig: GitConfig,
    private val objectMapper: ObjectMapper
) : IGitService {

    companion object {
        private val logger = LoggerFactory.getLogger(GitService::class.java)
        private const val MAX_FILE_SIZE = 1 * 1024 * 1024
    }

    @Value("\${scm.git.public.account}")
    private lateinit var gitPublicAccount: String

    @Value("\${scm.git.public.email}")
    private lateinit var gitPublicEmail: String

    @Value("\${scm.git.public.secret}")
    private lateinit var gitPublicSecret: String

    private val redirectUrl = gitConfig.redirectUrl

    private val gitCIUrl = gitConfig.gitUrl

    private val executorService = Executors.newFixedThreadPool(2)

    @BkTimed(extraTags = ["operation", "获取项目"], value = "bk_tgit_api_time")
    override fun getProject(accessToken: String, userId: String): List<Project> {

        logger.info("Start to get the projects by user $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            var page = 1

            val result = mutableListOf<Project>()
            while (true) {
                val projectUrl = "${gitConfig.gitApiUrl}/projects?access_token=$accessToken&page=$page&per_page=100"
                page++
                val request = Request.Builder()
                    .url(projectUrl)
                    .get()
                    .build()

                OkhttpUtils.doHttp(request).use { response ->
                    val data = response.body!!.string()
                    val repoList = JsonParser().parse(data).asJsonArray
                    repoList.forEach {
                        val obj = it.asJsonObject
                        val lastActivityTime = obj["last_activity_at"].asString.removeSuffix("+0000")
                        result.add(
                            Project(
                                id = obj["id"].asString,
                                name = obj["name"].asString,
                                nameWithNameSpace = obj["name_with_namespace"].asString,
                                sshUrl = obj["ssh_url_to_repo"].asString,
                                httpUrl = obj["https_url_to_repo"].asString,
                                lastActivity = DateTimeUtil.convertLocalDateTimeToTimestamp(
                                    LocalDateTime.parse(
                                        lastActivityTime
                                    )
                                ) * 1000L
                            )
                        )
                    }
                    if (repoList.size() < 100) {
                        logger.info("Finish get the project by user with size ${result.size}")
                        return result.sortedBy { 0 - it.lastActivity }
                    } // 倒序排序
                }
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the project")
        }
    }

    @BkTimed(extraTags = ["operation", "获取项目"], value = "bk_tgit_api_time")
    override fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitProjectsOrderBy?,
        sort: GitSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<Project> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val url = "${gitConfig.gitApiUrl}/projects" +
            "?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
                .addParams(
                    mapOf(
                        "search" to search,
                        "order_by" to orderBy?.value,
                        "sort" to sort?.value,
                        "owned" to owned,
                        "min_access_level" to minAccessLevel?.level
                    )
                )
        val res = mutableListOf<Project>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                repoList.forEach {
                    val project = it.asJsonObject
                    val lastActivityTime = project["last_activity_at"].asString.removeSuffix("+0000")
                    res.add(
                        Project(
                            id = project["id"].asString,
                            name = project["name"].asString,
                            nameWithNameSpace = project["name_with_namespace"].asString,
                            sshUrl = project["ssh_url_to_repo"].asString,
                            httpUrl = project["https_url_to_repo"].asString,
                            lastActivity = DateTimeUtil.convertLocalDateTimeToTimestamp(
                                LocalDateTime.parse(lastActivityTime)
                            ) * 1000L
                        )
                    )
                }
            }
        }
        return res
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
        val repoId = URLEncoder.encode(repository, "utf-8")
        val url = "${gitConfig.gitApiUrl}/projects/$repoId/repository/branches" +
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
                                    message = if (commit["message"].isJsonNull) {
                                        ""
                                    } else commit["message"].asString,
                                    authoredDate = if (commit["authored_date"].isJsonNull) {
                                        ""
                                    } else commit["authored_date"].asString,
                                    authorEmail = if (commit["author_email"].isJsonNull) {
                                        ""
                                    } else commit["author_email"].asString,
                                    authorName = if (commit["author_name"].isJsonNull) {
                                        ""
                                    } else commit["author_name"].asString,
                                    title = if (commit["title"].isJsonNull) {
                                        ""
                                    } else commit["title"].asString
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
        val repoId = URLEncoder.encode(repository, "utf-8")
        val url = "${gitConfig.gitApiUrl}/projects/$repoId/repository/tags" +
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

    @BkTimed(extraTags = ["operation", "刷新token"], value = "bk_tgit_api_time")
    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        logger.info("Start to refresh the token of user $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.gitUrl}/oauth/token" +
                "?client_id=${gitConfig.clientId}" +
                "&client_secret=${gitConfig.clientSecret}" +
                "&grant_type=refresh_token" +
                "&refresh_token=${accessToken.refreshToken}" +
                "&redirect_uri=${gitConfig.gitHookUrl}"
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

    @BkTimed(extraTags = ["operation", "AUTHORIZE"], value = "bk_tgit_api_time")
    override fun getAuthUrl(authParamJsonStr: String): String {
        return "${gitConfig.gitUrl}/oauth/authorize?client_id=${gitConfig.clientId}" +
            "&redirect_uri=${gitConfig.callbackUrl}&response_type=code&state=$authParamJsonStr"
    }

    @BkTimed(extraTags = ["operation", "TOKEN"], value = "bk_tgit_api_time")
    override fun getToken(userId: String, code: String): GitToken {
        logger.info("Start to get the token of user $userId by code $code")
        val startEpoch = System.currentTimeMillis()
        try {
            val tokenUrl =
                "${gitConfig.gitUrl}/oauth/token?client_id=${gitConfig.clientId}" +
                    "&client_secret=${gitConfig.clientSecret}&code=$code" +
                    "&grant_type=authorization_code&redirect_uri=${gitConfig.redirectUrl}"
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
            val url = StringBuilder("${gitConfig.gitUrl}/user")
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

    override fun getUserInfoById(userId: String, token: String, tokenType: TokenTypeEnum): GitUserInfo {
        return getGitUserInfo(userId, token, tokenType).data!!
    }

    @BkTimed(extraTags = ["operation", "RedirectUrl"], value = "bk_tgit_api_time")
    override fun getRedirectUrl(authParamJsonStr: String): String {
        logger.info("getRedirectUrl authParamJsonStr is: $authParamJsonStr")
        val authParamDecodeJsonStr = URLDecoder.decode(authParamJsonStr, "UTF-8")
        val authParams = JsonUtil.toMap(authParamDecodeJsonStr)
        val type = authParams["redirectUrlType"] as? String
        val specRedirectUrl = authParams["redirectUrl"] as? String
        return when (RedirectUrlTypeEnum.getRedirectUrlType(type ?: "")) {
            RedirectUrlTypeEnum.SPEC -> specRedirectUrl!!
            RedirectUrlTypeEnum.DEFAULT -> redirectUrl
            else -> {
                val projectId = authParams["projectId"] as String
                val repoId = authParams["repoId"] as String
                val repoHashId = "-" + HashUtil.encodeOtherLongId(repoId.toLong())
                "$redirectUrl/$projectId#popupGit$repoHashId"
            }
        }
    }

    @BkTimed(extraTags = ["operation", "GIT_FILE_CONTENT"], value = "bk_tgit_api_time")
    override fun getGitFileContent(
        repoUrl: String?,
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        val apiUrl = if (repoUrl.isNullOrBlank()) {
            gitConfig.gitApiUrl
        } else {
            GitUtils.getGitApiUrl(gitConfig.gitApiUrl, repoUrl)
        }
        logger.info("[$repoName|$filePath|$authType|$ref] Start to get the git file content from $apiUrl")
        val startEpoch = System.currentTimeMillis()
        try {
            val url =
                "$apiUrl/projects/${URLEncoder.encode(repoName, "UTF-8")}/repository/blobs/" +
                    "${URLEncoder.encode(ref, "UTF-8")}?filepath=${URLEncoder.encode(filePath, "UTF-8")}"
            var realUrl = url
            val request = if (authType == RepoAuthType.OAUTH) {
                realUrl += "&access_token=$token"
                Request.Builder()
                    .url(realUrl)
                    .get()
                    .build()
            } else {
                Request.Builder()
                    .url(realUrl)
                    .get()
                    .header("PRIVATE-TOKEN", token)
                    .build()
            }
            OkhttpUtils.doHttp(request).use {
                val data = it.stringLimit(
                    readLimit = MAX_FILE_SIZE,
                    errorMsg = I18nUtil.getCodeLanMessage(RepositoryMessageCode.BK_REQUEST_FILE_SIZE_LIMIT)
                )
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "fail to get git file content with: $url($data)"
                    )
                }
                return data
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    @BkTimed(extraTags = ["operation", "git_lab_file_content"], value = "bk_tgit_api_time")
    override fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): String {
        val apiUrl = GitUtils.getGitApiUrl(gitConfig.gitlabApiUrl, repoUrl)
        logger.info("[$repoName|$filePath|$ref|$accessToken] Start to get the gitlab file content from $apiUrl")
        val startEpoch = System.currentTimeMillis()
        try {
            val headers = mapOf("PRIVATE-TOKEN" to accessToken)
            // 查询文件内容
            val encodeFilePath = URLEncoder.encode(filePath, "utf-8")
            val encodeRef = URLEncoder.encode(ref, "utf-8")
            val encodeProjectName = URLEncoder.encode(repoName, "utf-8")
            val projectFileUrl =
                "$apiUrl/projects/$encodeProjectName/repository/files/$encodeFilePath?ref=$encodeRef"
            logger.info(projectFileUrl)
            OkhttpUtils.doGet(projectFileUrl, headers).use { response ->
                val body = response.stringLimit(
                    readLimit = MAX_FILE_SIZE,
                    errorMsg = I18nUtil.getCodeLanMessage(RepositoryMessageCode.BK_REQUEST_FILE_SIZE_LIMIT)
                )
                logger.info("get gitlab content response body: $body")
                val fileInfo = objectMapper.readValue(body, GitlabFileInfo::class.java)
                return String(Base64.getDecoder().decode(fileInfo.content))
            }
        } catch (e: Exception) {
            logger.warn(
                "Fail to get the gitlab content of repo($repoName) in path($filePath)/ref($ref): ${e.message}",
                e
            )
            return ""
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the gitlab file content")
        }
    }

    @BkTimed(extraTags = ["operation", "git_create_repository"], value = "bk_tgit_api_time")
    override fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum?
    ): Result<GitRepositoryResp?> {
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects")
        setToken(tokenType, url, token)
        val params = mutableMapOf<String, Any?>()
        params["name"] = repositoryName
        if (null != visibilityLevel) {
            params["namespace_id"] = namespaceId
        }
        if (null != visibilityLevel) {
            params["visibility_level"] = visibilityLevel.level
            if (visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC) {
                params["fork_enabled"] = true // 如果项目设置为开源就打开fork设置开关
            }
        }
        val request = Request.Builder()
            .url(url.toString())
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(params)))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            val dataMap = JsonUtil.toMap(data)
            val atomRepositoryUrl = dataMap["http_url_to_repo"]
            if (StringUtils.isEmpty(atomRepositoryUrl)) {
                val validateResult: Result<String?> =
                    I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_CREATE_GIT_CODE_REPOSITORY_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                logger.info("createOAuthCodeRepository validateResult>> $validateResult")

                return Result(validateResult.status, "${validateResult.message}（git error:$data）")
            }
            val nameSpaceName = dataMap["name_with_namespace"] as String
            // 把需要创建项目代码库的用户加入为对应项目的owner用户
            executorService.submit<Unit> {
                // 添加插件的开发成员
                addGitProjectMember(listOf(userId), nameSpaceName, GitAccessLevelEnum.MASTER, token, tokenType)
                if (!sampleProjectPath.isNullOrBlank()) {
                    // 把样例工程代码添加到用户的仓库
                    initRepositoryInfo(
                        userId = userId,
                        sampleProjectPath = sampleProjectPath,
                        token = token,
                        tokenType = tokenType,
                        repositoryName = repositoryName,
                        atomRepositoryUrl = atomRepositoryUrl as String,
                        frontendType = frontendType
                    )
                }
            }
            return Result(GitRepositoryResp(nameSpaceName, atomRepositoryUrl as String))
        }
    }

    @BkTimed(extraTags = ["operation", "init_repository_info"], value = "bk_tgit_api_time")
    fun initRepositoryInfo(
        userId: String,
        sampleProjectPath: String,
        token: String,
        tokenType: TokenTypeEnum,
        repositoryName: String,
        atomRepositoryUrl: String,
        frontendType: FrontendTypeEnum?
    ): Result<Boolean> {
        val atomTmpWorkspace = Files.createTempDirectory(repositoryName).toFile()
        logger.info("initRepositoryInfo atomTmpWorkspace is:${atomTmpWorkspace.absolutePath}")
        try {
            // 1、clone插件示例工程代码到插件工作空间下
            val credentialSetter = if (tokenType == TokenTypeEnum.OAUTH) {
                CodeGitOauthCredentialSetter(token)
            } else {
                CodeGitUsernameCredentialSetter(gitPublicAccount, gitPublicSecret)
            }
            CommonScriptUtils.execute(
                script = "git clone ${credentialSetter.getCredentialUrl(sampleProjectPath)}",
                dir = atomTmpWorkspace
            )
            // 2、删除下载下来示例工程的git信息
            val atomFileDir = atomTmpWorkspace.listFiles()?.firstOrNull()
            logger.info("initRepositoryInfo atomFileDir is:${atomFileDir?.absolutePath}")
            val atomGitFileDir = File(atomFileDir, ".git")
            if (atomGitFileDir.exists()) {
                FileSystemUtils.deleteRecursively(atomGitFileDir)
            }
            // 如果用户选的是自定义UI方式开发插件，则需要初始化UI开发脚手架
            if (FrontendTypeEnum.SPECIAL == frontendType) {
                val atomFrontendFileDir = File(atomFileDir, BK_FRONTEND_DIR_NAME)
                if (!atomFrontendFileDir.exists()) {
                    atomFrontendFileDir.mkdirs()
                }
                CommonScriptUtils.execute(
                    script = "git clone ${credentialSetter.getCredentialUrl(gitConfig.frontendSampleProjectUrl)}",
                    dir = atomFrontendFileDir
                )
                val frontendProjectDir = atomFrontendFileDir.listFiles()?.firstOrNull()
                logger.info("initRepositoryInfo frontendProjectDir is:${frontendProjectDir?.absolutePath}")
                val frontendGitFileDir = File(frontendProjectDir, ".git")
                if (frontendGitFileDir.exists()) {
                    FileSystemUtils.deleteRecursively(frontendGitFileDir)
                }
            }
            // 把task.json中的atomCode修改成用户对应的
            val taskJsonFile = File(atomFileDir, "task.json")
            if (taskJsonFile.exists()) {
                val taskJsonStr = taskJsonFile.readText(Charset.forName("UTF-8"))
                val taskJsonMap = JsonUtil.toMap(taskJsonStr).toMutableMap()
                taskJsonMap["atomCode"] = repositoryName
                val deleteFlag = taskJsonFile.delete()
                if (deleteFlag) {
                    taskJsonFile.createNewFile()
                    taskJsonFile.writeText(JsonUtil.toJson(taskJsonMap), Charset.forName("UTF-8"))
                }
            }
            // 3、重新生成git信息
            CommonScriptUtils.execute("git init", atomFileDir)
            // 4、添加远程仓库
            CommonScriptUtils.execute(
                script = "git remote add origin ${credentialSetter.getCredentialUrl(atomRepositoryUrl)}",
                dir = atomFileDir
            )
            // 5、给文件添加git信息
            CommonScriptUtils.execute("git config user.email \"$gitPublicEmail\"", atomFileDir)
            CommonScriptUtils.execute("git config user.name \"$gitPublicAccount\"", atomFileDir)
            CommonScriptUtils.execute("git add .", atomFileDir)
            // 6、提交本地文件
            CommonScriptUtils.execute("git commit -m init", atomFileDir)
            // 7、提交代码到远程仓库
            CommonScriptUtils.execute("git push origin master", atomFileDir)
            logger.info("initRepositoryInfo finish")
        } catch (e: Exception) {
            logger.error("initRepositoryInfo error is:", e)
            return Result(false)
        } finally {
            FileSystemUtils.deleteRecursively(atomTmpWorkspace)
        }
        return Result(true)
    }

    @BkTimed(extraTags = ["operation", "add_project_member"], value = "bk_tgit_api_time")
    override fun addGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        var gitUserInfo: GitUserInfo?
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/members")
        setToken(tokenType, url, token)
        userIdList.forEach {
            val gitUserInfoResult = getGitUserInfo(it, token, tokenType)
            logger.info("the gitUserInfoResult is :$gitUserInfoResult")
            if (gitUserInfoResult.isNotOk()) {
                return Result(gitUserInfoResult.status, gitUserInfoResult.message, false)
            } else {
                gitUserInfo = gitUserInfoResult.data
            }
            val params = mutableMapOf<String, Any?>()
            params["id"] = repoName
            params["user_id"] = gitUserInfo!!.id
            params["access_level"] = gitAccessLevel.level
            val request = Request.Builder()
                .url(url.toString())
                .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(params)))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                if (!StringUtils.isEmpty(data)) {
                    val dataMap = JsonUtil.toMap(data)
                    val message = dataMap["message"]
                    if (!StringUtils.isEmpty(message)) {
                        val validateResult: Result<String?> = I18nUtil.generateResponseDataObject(
                            messageCode = RepositoryMessageCode.USER_ADD_GIT_CODE_REPOSITORY_MEMBER_FAIL,
                            params = arrayOf(it)
                        )

                        return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                    }
                }
            }
        }
        return Result(true)
    }

    @BkTimed(extraTags = ["operation", "delete_project_member"], value = "bk_tgit_api_time")
    override fun deleteGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        logger.info("deleteGitProjectMember userIdList is:$userIdList,repoName is:$repoName,tokenType is:$tokenType")
        var gitUserInfo: GitUserInfo?
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/members")
        userIdList.forEach {
            val gitUserInfoResult = getGitUserInfo(it, token, tokenType)
            logger.info("the gitUserInfoResult is :$gitUserInfoResult")
            if (gitUserInfoResult.isNotOk()) {
                return Result(gitUserInfoResult.status, gitUserInfoResult.message, false)
            } else {
                gitUserInfo = gitUserInfoResult.data
            }
            if (null != gitUserInfo) {
                val gitProjectMemberInfoResult = getGitProjectMemberInfo(gitUserInfo!!.id, repoName, token, tokenType)
                logger.info("the gitProjectMemberInfoResult is :$gitProjectMemberInfoResult")
                val gitProjectMemberInfo: GitUserInfo?
                if (gitProjectMemberInfoResult.isNotOk()) {
                    return Result(gitProjectMemberInfoResult.status, gitProjectMemberInfoResult.message, false)
                } else {
                    gitProjectMemberInfo = gitProjectMemberInfoResult.data
                }
                if (null == gitProjectMemberInfo) {
                    return@forEach // 兼容历史插件的成员可能未关联代码库的情况
                }
                url.append("/${gitUserInfo!!.id}")
                setToken(tokenType, url, token)
                val request = Request.Builder()
                    .url(url.toString())
                    .delete()
                    .build()
                OkhttpUtils.doHttp(request).use { response ->
                    val data = response.body!!.string()
                    logger.info("deleteGitProjectMember response>> $data")
                    if (!StringUtils.isEmpty(data)) {
                        val dataMap = JsonUtil.toMap(data)
                        val message = dataMap["message"]
                        if (!StringUtils.isEmpty(message)) {
                            val validateResult: Result<String?> = I18nUtil.generateResponseDataObject(
                                messageCode = RepositoryMessageCode.USER_DELETE_GIT_CODE_REPOSITORY_MEMBER_FAIL,
                                params = arrayOf(it),
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            )
                            logger.info("deleteGitProjectMember validateResult>> $validateResult")

                            return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                        }
                    }
                }
            }
        }
        return Result(true)
    }

    @BkTimed(extraTags = ["operation", "get_project_member_info"], value = "bk_tgit_api_time")
    fun getGitProjectMemberInfo(
        memberId: Int,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitUserInfo?> {
        logger.info("getGitProjectMemberInfo memberId is:$memberId,repoName is:$repoName,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/members/$memberId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("getGitProjectMemberInfo  response>> $data")
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (StringUtils.isEmpty(message)) {
                    return Result(JsonUtil.to(data, GitUserInfo::class.java))
                }
            }
            return Result(data = null)
        }
    }

    @BkTimed(extraTags = ["operation", "delete_project_member_info"], value = "bk_tgit_api_time")
    override fun deleteGitProject(repoName: String, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        logger.info("deleteGitProject repoName is:$repoName,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("deleteGitProject  response>> $data")
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> = I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_UPDATE_GIT_CODE_REPOSITORY_FAIL
                    )
                    return Result(
                        status = validateResult.status,
                        message = "${validateResult.message}（git error:$message）"
                    )
                }
            }
            return Result(data = true)
        }
    }

    @BkTimed(extraTags = ["operation", "get_user_info"], value = "bk_tgit_api_time")
    fun getGitUserInfo(userId: String, token: String, tokenType: TokenTypeEnum): Result<GitUserInfo?> {
        logger.info("getGitUserInfo userId is:$userId,tokenType is:$tokenType")
        val url = StringBuilder("${gitConfig.gitApiUrl}/users/$userId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("getGitUserInfo response>> $data")
            if (!it.isSuccessful) return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (StringUtils.isEmpty(message)) {
                    return Result(JsonUtil.to(data, GitUserInfo::class.java))
                }
            }
            return Result(data = null)
        }
    }

    @BkTimed(extraTags = ["operation", "git_project_info"], value = "bk_tgit_api_time")
    override fun getGitProjectInfo(id: String, token: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        logger.info("getGitUserInfo id is:$id,tokenType is:$tokenType")
        val encodeId = URLEncoder.encode(id, "utf-8") // 如果id为NAMESPACE_PATH则需要encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("GitProjectInfo response>> $data")
            if (!it.isSuccessful) return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
            return Result(JsonUtil.to(data, GitProjectInfo::class.java))
        }
    }

    @BkTimed(extraTags = ["operation", "git_repository_tree_info"], value = "bk_tgit_api_time")
    override fun getGitRepositoryTreeInfo(
        userId: String,
        repoName: String,
        refName: String?,
        path: String?,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?> {
        logger.info("getGitRepositoryTreeInfo userId is:$userId,repoName is:$repoName,refName is:$refName")
        logger.info("getGitRepositoryTreeInfo path is:$path,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/tree")
        setToken(tokenType, url, token)
        if (!refName.isNullOrBlank()) {
            url.append("&ref_name=$refName")
        }
        if (!path.isNullOrBlank()) {
            url.append("&path=$path")
        }
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("getGitRepositoryTreeInfo response>> $data")
            if (!StringUtils.isEmpty(data)) {
                var message: String? = null
                if (data.contains("\"message\":")) {
                    val dataMap = JsonUtil.toMap(data)
                    message = dataMap["message"] as? String
                }
                return if (StringUtils.isEmpty(message)) {
                    Result(JsonUtil.to(data, object : TypeReference<List<GitRepositoryDirItem>>() {}))
                } else {
                    val result: Result<String?> = I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.GIT_REPO_PEM_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                    Result(result.status, "${result.message}（git error:$message）")
                }
            }
            return Result(data = null)
        }
    }

    @BkTimed(extraTags = ["operation", "update_project_info"], value = "bk_tgit_api_time")
    override fun updateGitProjectInfo(
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val encodeProjectName = URLEncoder.encode(projectName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .put(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(updateGitProjectInfo)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("updateGitProjectInfo response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val message = dataMap["message"]
            if (!StringUtils.isEmpty(message)) {
                val validateResult: Result<String?> =
                    I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_UPDATE_GIT_CODE_REPOSITORY_FAIL
                    )
                logger.info("updateGitProjectInfo validateResult>> $validateResult")

                return Result(validateResult.status, "${validateResult.message}（git error:$message）")
            }
            return Result(true)
        }
    }

    @BkTimed(extraTags = ["operation", "move_project_group"], value = "bk_tgit_api_time")
    override fun moveProjectToGroup(
        groupCode: String,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        logger.info("updateGitProjectInfo groupCode is:$groupCode,repoName is:$repoName,tokenType is:$tokenType")
        val gitProjectInfo: GitProjectInfo?
        val gitProjectInfoResult = getGitProjectInfo(repoName, token, tokenType)
        logger.info("the gitProjectInfoResult is :$gitProjectInfoResult")
        if (gitProjectInfoResult.isNotOk()) {
            return Result(status = gitProjectInfoResult.status, message = gitProjectInfoResult.message ?: "")
        } else {
            gitProjectInfo = gitProjectInfoResult.data
        }
        if (null == gitProjectInfo) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(repoName),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val projectId = gitProjectInfo.id // 获取工蜂项目ID
        val url = StringBuilder("${gitConfig.gitApiUrl}/groups/$groupCode/projects/$projectId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(mapOf<String, String>())
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                val data = it.body!!.string()
                logger.info("moveProjectToGroup response>> $data")
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                return if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> = I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL,
                        params = arrayOf(groupCode)
                    )
                    logger.info("moveProjectToGroup validateResult>> $validateResult")
                    Result(validateResult.status, "${validateResult.message}（git error:$message）")
                } else {
                    I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL,
                        params = arrayOf(groupCode)
                    )
                }
            }
            return Result(getGitProjectInfo(projectId.toString(), token, tokenType).data)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    @BkTimed(extraTags = ["operation", "mr_info"], value = "bk_tgit_api_time")
    override fun getMrInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrInfo {
        val url = StringBuilder(
            "${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(repoName, "UTF-8")}" +
                "/merge_request/$mrId"
        )
        logger.info("get mr info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get merge info error for $repoName, $mrId(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            logger.info("get mr info response body: $data")
            return JsonUtil.to(data, GitMrInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    @BkTimed(extraTags = ["operation", "mr_review_info"], value = "bk_tgit_api_time")
    override fun getMrReviewInfo(
        id: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrReviewInfo {
        val url = StringBuilder(
            "${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(id, "UTF-8")}/merge_request/$mrId/review"
        )
        logger.info("get mr review info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get merge reviewers info error for $id, $mrId(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            return JsonUtil.to(data, GitMrReviewInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    @BkTimed(extraTags = ["operation", "mr_change_info"], value = "bk_tgit_api_time")
    override fun getMrChangeInfo(
        id: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrChangeInfo {
        val url = StringBuilder(
            "${getApiUrl(repoUrl)}/projects/${
                URLEncoder.encode(
                    id,
                    "UTF-8"
                )
            }/merge_request/$mrId/changes"
        )
        logger.info("get mr changes info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get merge changes request info error for $id, $mrId(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            return JsonUtil.to(data, GitMrChangeInfo::class.java)
        }
    }

    private fun getApiUrl(repoUrl: String?): String {
        return if (repoUrl.isNullOrBlank()) {
            gitConfig.gitApiUrl
        } else {
            GitUtils.getGitApiUrl(gitConfig.gitApiUrl, repoUrl)
        }
    }

    @BkTimed(extraTags = ["operation", "download_git_repo_file"], value = "bk_tgit_api_time")
    override fun downloadGitRepoFile(
        repoName: String,
        sha: String?,
        token: String,
        tokenType: TokenTypeEnum,
        response: HttpServletResponse
    ) {
        logger.info("downloadGitRepoFile  repoName is:$repoName,sha is:$sha,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/archive")
        setToken(tokenType, url, token)
        if (!sha.isNullOrBlank()) {
            url.append("&sha=$sha")
        }
        OkhttpUtils.downloadFile(url.toString(), response)
    }

    @BkTimed(extraTags = ["operation", "add_commit_check"], value = "bk_tgit_api_time")
    fun setToken(tokenType: TokenTypeEnum, url: StringBuilder, token: String) {
        if (TokenTypeEnum.OAUTH == tokenType) {
            url.append("?access_token=$token")
        } else {
            url.append("?private_token=$token")
        }
    }

    @BkTimed(extraTags = ["operation", "repo_members"], value = "bk_tgit_api_time")
    override fun getRepoMembers(accessToken: String, userId: String, repoName: String): List<GitMember> {
        val url = StringBuilder(
            "${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(repoName, "UTF-8")}/members"
        )
        logger.info("get repo member url: $url")
        setToken(TokenTypeEnum.OAUTH, url, accessToken)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get repo members for $userId, $repoName fail(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            return JsonUtil.to(data)
        }
    }

    @BkTimed(extraTags = ["operation", "CI获取项目中成员信息"], value = "bk_tgit_api_time")
    override fun getRepoMemberInfo(
        accessToken: String,
        userId: String,
        repoName: String,
        tokenType: TokenTypeEnum
    ): GitMember {
        return if (TokenTypeEnum.OAUTH == tokenType) {
            GitOauthApi().getRepoMemberInfo(
                host = gitConfig.gitApiUrl,
                token = accessToken,
                userId = userId,
                gitProjectId = repoName
            )
        } else {
            GitApi().getRepoMemberInfo(
                host = gitConfig.gitApiUrl,
                token = accessToken,
                userId = userId,
                gitProjectId = repoName
            )
        }
    }

    @BkTimed(extraTags = ["operation", "获取项目中全部成员信息"], value = "bk_tgit_api_time")
    override fun getRepoAllMembers(accessToken: String, userId: String, repoName: String): List<GitMember> {
        val url = StringBuilder(
            "${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(repoName, "UTF-8")}/members/all"
        )
        logger.info("get repo all member url: $url")
        setToken(TokenTypeEnum.OAUTH, url, accessToken)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get repo all members for $userId, $repoName fail(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            return JsonUtil.to(data)
        }
    }

    @BkTimed(extraTags = ["operation", "repo_recent_commit_info"], value = "bk_tgit_api_time")
    override fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        logger.info("getRepoRecentCommitInfo repoName:$repoName, sha:$sha, tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, Charsets.UTF_8.name())
        val encodeSha = URLEncoder.encode(sha, Charsets.UTF_8.name())
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/commits/$encodeSha")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            if (!it.isSuccessful) return I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (StringUtils.isEmpty(message)) {
                    return Result(JsonUtil.to(data, GitCommit::class.java))
                }
            }
            return Result(data = null)
        }
    }

    override fun unlockHookLock(
        projectId: String?,
        repoName: String,
        mrId: Long
    ) {
        GitOauthApi().unlockHookLock(
            host = gitConfig.gitApiUrl,
            token = gitConfig.hookLockToken,
            projectName = repoName,
            mrId = mrId
        )
    }

    @BkTimed(extraTags = ["operation", "project_group_info"], value = "bk_tgit_api_time")
    override fun getProjectGroupInfo(
        id: String,
        includeSubgroups: Boolean?,
        token: String,
        tokenType: TokenTypeEnum
    ): GitProjectGroupInfo {
        val url = StringBuilder("${gitConfig.gitApiUrl}/groups/$id")
        setToken(tokenType, url, token)
        if (includeSubgroups != null) {
            url.append("&include_subgroups=$includeSubgroups")
        }
        logger.info("getProjectGroupInfo url: $url")

        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "getProjectGroupInfo $id, $includeSubgroups(${it.code}): ${it.message}"
                )
            }
            val data = it.body!!.string()
            logger.info("getProjectGroupInfo response body: $data")
            return JsonUtil.to(data, GitProjectGroupInfo::class.java)
        }
    }

    @BkTimed(extraTags = ["operation", "创建标签"], value = "bk_tgit_api_time")
    override fun createGitTag(
        repoName: String,
        tagName: String,
        ref: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/tags")
        setToken(tokenType, url, token)
        val params = mutableMapOf<String, Any?>()
        params["id"] = repoName
        params["tag_name"] = tagName
        params["ref"] = ref
        val request = Request.Builder()
            .url(url.toString())
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(params)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            logger.info("createGitTag response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val message = dataMap["message"]
            if (!StringUtils.isEmpty(message)) {
                val validateResult: Result<String?> =
                    I18nUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.CREATE_TAG_FAIL
                    )
                logger.info("createGitTag validateResult>> $validateResult")

                return Result(validateResult.status, "${validateResult.message}（git error:$message）")
            }
            return Result(true)
        }
    }

    override fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int
    ): List<ChangeFileInfo> {
        return if (TokenTypeEnum.OAUTH == tokenType) {
            GitOauthApi().getChangeFileList(
                host = gitConfig.gitApiUrl,
                gitProjectId = gitProjectId,
                token = token,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            )
        } else {
            GitApi().getChangeFileList(
                host = gitConfig.gitApiUrl,
                gitProjectId = gitProjectId,
                token = token,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            )
        }
    }

    override fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?,
        tokenType: TokenTypeEnum
    ): List<GitCodeGroup> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
//        val url = "$gitCIUrl/api/v3/groups?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
//            .addParams(
//                mapOf(
//                    "owned" to owned,
//                    "min_access_level" to minAccessLevel?.level
//                )
//            )
//        val request = Request.Builder()
//            .url(url)
//            .get()
//            .build()
//        OkhttpUtils.doHttp(request).use { response ->
//            logger.info("[url=$url]|getProjectGroupList with response=$response")
//            if (!response.isSuccessful) {
//                throw GitCodeUtils.handleErrorMessage(response)
//            }
//            val data = response.body()?.string()?.ifBlank { null } ?: return emptyList()
//            return JsonUtil.to(data, object : TypeReference<List<GitCodeGroup>>() {})
//        }
        return if (TokenTypeEnum.OAUTH == tokenType) {
            GitOauthApi().getProjectGroupsList(
                host = gitCIUrl,
                token = accessToken,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                owned = owned,
                minAccessLevel = minAccessLevel
            )
        } else {
            GitApi().getProjectGroupsList(
                host = gitCIUrl,
                token = accessToken,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                owned = owned,
                minAccessLevel = minAccessLevel
            )
        }
    }

    @BkTimed(extraTags = ["operation", "members"], value = "bk_tgit_api_time")
    override fun getMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>> {
        val url = StringBuilder(
            "${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(gitProjectId, "UTF-8")}/members"
        )
        setToken(tokenType, url, token)
        url.append(
            if (search != null) {
                "&query=$search"
            } else {
                ""
            }
        )
        url.append("&page=$page&per_page=$pageSize")
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = "get repo member error for $gitProjectId(${it.code}): ${it.message}"
                )
            }
            return Result(JsonUtil.to(data, object : TypeReference<List<GitMember>>() {}))
        }
    }

//    override fun getGitUserId(rtxUserId: String, gitProjectId: String): Result<String?> {
//        TODO("Not yet implemented")
//    }

    @BkTimed(extraTags = ["operation", "GIT_CI_USER"], value = "bk_tgit_api_time")
    override fun getGitUserId(
        rtxUserId: String,
        gitProjectId: String,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<String?> {
        try {
            val url = StringBuilder("$gitCIUrl/api/v3/users/$rtxUserId")
            setToken(tokenType, url, token)
            logger.info("[$rtxUserId]|[$gitProjectId]| Get gitUserId: $url")
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val body = response.body!!.string()
                logger.info("[$rtxUserId]|[$gitProjectId]| Get gitUserId response body: $body")
                val userInfo = JsonUtil.to(body, Map::class.java)
                return Result(userInfo["id"].toString())
            }
        } catch (ignore: Exception) {
            logger.error("get git project member fail! gitProjectId: $gitProjectId", ignore)
            return Result(null)
        }
    }

    @BkTimed(extraTags = ["operation", "获取项目中全部成员信息"], value = "bk_tgit_api_time")
    override fun getProjectMembersAll(
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<List<GitMember>> {
        val newPage = if (page == 0) 1 else page
        val newPageSize = if (pageSize > 1000) 1000 else pageSize
        val url = StringBuilder("$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "UTF8")}/members/all")
        setToken(tokenType, url, token)
        url.append(
            if (search != null) {
                "&query=$search"
            } else {
                ""
            } +
                "&page=$newPage" + "&per_page=$newPageSize"
        )
        logger.info("getGitCIAllMembers request url: $url")
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body!!.string()
            if (!it.isSuccessful) throw RuntimeException("fail to getGitCIAllMembers with: $url($data)")
            return Result(JsonUtil.to(data, object : TypeReference<List<GitMember>>() {}))
        }
    }

    @BkTimed(extraTags = ["operation", "git_file_info"], value = "bk_tgit_api_time")
    override fun getGitFileInfo(
        gitProjectId: String,
        filePath: String?,
        token: String,
        ref: String?,
        tokenType: TokenTypeEnum
    ): Result<GitCodeFileInfo> {
        val startEpoch = System.currentTimeMillis()
        try {
            val encodeId = URLEncoder.encode(gitProjectId, "utf-8")
            val url = StringBuilder("$gitCIUrl/api/v3/projects/$encodeId/repository/files")
            setToken(tokenType, url, token)
            url.append(
                if (ref != null) {
                    "&ref=${URLEncoder.encode(ref, "UTF-8")}"
                } else {
                    ""
                }
            )
            url.append(
                if (filePath != null) {
                    "&file_path=${URLEncoder.encode(filePath, "UTF-8")}"
                } else {
                    ""
                }
            )
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
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

    @BkTimed(extraTags = ["operation", "add_mr_commit"], value = "bk_tgit_api_time")
    override fun addMrComment(
        token: String,
        gitProjectId: String,
        mrId: Long,
        mrBody: String,
        tokenType: TokenTypeEnum
    ) {
        logger.info("$gitProjectId|$mrId|addMrComment")
        try {
            val gitApi = if (TokenTypeEnum.OAUTH == tokenType) {
                GitOauthApi()
            } else {
                GitApi()
            }
            gitApi.addMRComment(
                host = "$gitCIUrl/api/v3",
                token = token,
                projectName = gitProjectId,
                requestId = mrId,
                message = mrBody
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

    @BkTimed(extraTags = ["operation", "git_file_tree"], value = "bk_tgit_api_time")
    override fun getGitFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>> {
        logger.info("[$gitProjectId|$path|$ref] Start to get the git file tree")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = StringBuilder("$gitCIUrl/api/v3/projects/$gitProjectId/repository/tree")
            setToken(tokenType, url, token)
            with(url) {
                append(
                    "&path=${URLEncoder.encode(path, "UTF-8")}"
                )
                append(
                    if (!ref.isNullOrBlank()) {
                        "&ref_name=${URLEncoder.encode(ref, "UTF-8")}"
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
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "(${it.code})${it.message}"
                    )
                }
                val data = it.body!!.string()
                return Result(JsonUtil.getObjectMapper().readValue(data) as List<GitFileInfo>)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file tree")
        }
    }

    @BkTimed(extraTags = ["operation", "gitCI拉提交记录"], value = "bk_tgit_api_time")
    override fun getCommits(
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        token: String,
        since: String?,
        until: String?,
        page: Int,
        perPage: Int,
        tokenType: TokenTypeEnum
    ): Result<List<Commit>> {

        logger.info("[$gitProjectId|$filePath|$branch|$since|$until] Start to get the git commits")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = StringBuilder("$gitCIUrl/api/v3/projects/$gitProjectId/repository/commits")
            setToken(tokenType, url, token)
            with(url) {
                append(
                    if (branch != null) {
                        "&ref_name=${URLEncoder.encode(branch, "UTF-8")}"
                    } else {
                        ""
                    }
                )
                append(
                    if (filePath != null) {
                        "&path=${URLEncoder.encode(filePath, "UTF-8")}"
                    } else {
                        ""
                    }
                )
                append(
                    if (since != null) {
                        "&since=${since.replace("+", "%2B")}"
                    } else {
                        ""
                    }
                )
                append(
                    if (until != null) {
                        "&until=${until.replace("+", "%2B")}"
                    } else {
                        ""
                    }
                )
                append("&page=$page&per_page=$perPage")
            }
            logger.info("request url: $url")
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                        message = "(${it.code})${it.message}"
                    )
                }
                val data = it.body!!.string()
                return Result(JsonUtil.getObjectMapper().readValue(data) as List<Commit>)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git commits")
        }
    }

    @BkTimed(extraTags = ["operation", "enableCi"], value = "bk_tgit_api_time")
    override fun enableCi(
        projectName: String,
        token: String,
        tokenType: TokenTypeEnum,
        enable: Boolean?
    ): Result<Boolean> {
        logger.info(
            "enableCi projectName:$projectName," +
                "enable:$enable,tokenType:$tokenType"
        )
        val encodeProjectName = URLEncoder.encode(projectName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/ci/enable")
        setToken(tokenType, url, token)
        url.append("&enable_ci=$enable")
        val request = Request.Builder()
            .url(url.toString())
            .put(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(), "{}"
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                return Result(it.code, "enableCi fail ${it.message}")
            }
            val data = it.body!!.string()
            logger.info("enableCi response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val code = dataMap["code"]
            if (code != 200) {
                // 把工蜂的错误提示抛出去
                return Result(code as Int, "${dataMap["message"]}")
            }
            return Result(true)
        }
    }

    @BkTimed(extraTags = ["operation", "git_create_file"], value = "bk_tgit_api_time")
    override fun gitCreateFile(
        gitProjectId: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val url = StringBuilder("$gitCIUrl/api/v3/projects/$gitProjectId/repository/files")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(gitOperationFile)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            logger.info("request: $request Start to create file resp: $it")
            if (!it.isSuccessful) {
                throw GitCodeUtils.handleErrorMessage(it)
            }
            return Result(true)
        }
    }

    override fun tGitUpdateFile(
        repoUrl: String?,
        repoName: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> = Result(false)

    override fun getGitCodeProjectList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>> {
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
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                return Result(JsonUtil.to(data, object : TypeReference<List<GitCodeProjectInfo>>() {}))
            }
        }
        return Result(res)
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

    override fun getTapdWorkItems(
        accessToken: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        type: String,
        iid: Long
    ): Result<List<TapdWorkItem>> {
        val gitApi = if (tokenType == TokenTypeEnum.OAUTH) {
            GitOauthApi()
        } else {
            GitApi()
        }
        return Result(
            gitApi.getTapdWorkitems(
                host = gitConfig.gitApiUrl,
                token = accessToken,
                id = gitProjectId,
                type = type,
                iid = iid
            )
        )
    }

    override fun getCommitDiff(
        accessToken: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        sha: String,
        path: String?,
        ignoreWhiteSpace: Boolean?
    ): Result<List<GitDiff>> {
        val gitApi = if (tokenType == TokenTypeEnum.OAUTH) {
            GitOauthApi()
        } else {
            GitApi()
        }
        return Result(
            gitApi.getCommitDiff(
                host = gitConfig.gitApiUrl,
                sha = sha,
                token = accessToken,
                projectName = gitProjectId
            )
        )
    }
}
