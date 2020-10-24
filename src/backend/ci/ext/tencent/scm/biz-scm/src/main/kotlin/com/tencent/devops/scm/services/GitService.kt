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

package com.tencent.devops.scm.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.CommonScriptUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitMrInfo
import com.tencent.devops.repository.pojo.git.GitMrReviewInfo
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.gitlab.GitlabFileInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.CodeGitOauthCredentialSetter
import com.tencent.devops.scm.code.git.CodeGitUsernameCredentialSetter
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitBranchCommit
import com.tencent.devops.scm.code.git.api.GitOauthApi
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.code.git.api.GitTagCommit
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.OwnerInfo
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.utils.code.git.GitUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Base64
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletResponse

@Service
class GitService @Autowired constructor(
    private val gitConfig: GitConfig,
    private val objectMapper: ObjectMapper,
    private val sampleProjectGitFileService: SampleProjectGitFileService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitService::class.java)
        private val gitOauthApi = GitOauthApi()
    }
    @Value("\${gitCI.clientId}")
    private lateinit var gitCIClientId: String

    @Value("\${gitCI.clientSecret}")
    private lateinit var gitCIClientSecret: String

    @Value("\${gitCI.url}")
    private lateinit var gitCIUrl: String

    @Value("\${gitCI.oauthUrl}")
    private lateinit var gitCIOauthUrl: String

    private val clientId: String = gitConfig.clientId
    private val clientSecret: String = gitConfig.clientSecret
    private val callbackUrl: String = gitConfig.callbackUrl
    private val redirectUrl: String = gitConfig.redirectUrl

    @Value("\${git.public.account}")
    private lateinit var gitPublicAccount: String

    @Value("\${git.public.email}")
    private lateinit var gitPublicEmail: String

    @Value("\${git.public.secret}")
    private lateinit var gitPublicSecret: String

    private val executorService = Executors.newFixedThreadPool(2)

    fun getProject(accessToken: String, userId: String): List<Project> {

        logger.info("Start to get the projects by user $userId with token $accessToken")

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
                    val data = response.body()!!.string()
                    val repoList = JsonParser().parse(data).asJsonArray
                    repoList.forEach {
                        val obj = it.asJsonObject
                        val lastActivityTime = obj["last_activity_at"].asString.removeSuffix("+0000")
                        result.add(
                            Project(
                                obj["id"].asString,
                                obj["name"].asString,
                                obj["name_with_namespace"].asString,
                                obj["ssh_url_to_repo"].asString,
                                obj["http_url_to_repo"].asString,
                                DateTimeUtil.convertLocalDateTimeToTimestamp(LocalDateTime.parse(lastActivityTime)) * 1000L
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

    fun getProjectList(accessToken: String, userId: String, page: Int?, pageSize: Int?): List<Project> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val url = "${gitConfig.gitApiUrl}/projects?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
        val res = mutableListOf<Project>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()?.string() ?: return@use
            val repoList = JsonParser().parse(data).asJsonArray
            if (!repoList.isJsonNull) {
                repoList.forEach {
                    val project = it.asJsonObject
                    val lastActivityTime = project["last_activity_at"].asString.removeSuffix("+0000")
                    res.add(Project(
                        project["id"].asString,
                        project["name"].asString,
                        project["name_with_namespace"].asString,
                        project["ssh_url_to_repo"].asString,
                        project["http_url_to_repo"].asString,
                        DateTimeUtil.convertLocalDateTimeToTimestamp(LocalDateTime.parse(lastActivityTime)) * 1000L
                    ))
                }
            }
        }
        return res
    }

    fun getBranch(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): List<GitBranch> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get the $userId's $repository branch by accessToken: page: $pageNotNull pageSize: $pageSizeNotNull")
        val repoId = URLEncoder.encode(repository, "utf-8")
        val url = "${gitConfig.gitApiUrl}/projects/$repoId/repository/branches?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
        val res = mutableListOf<GitBranch>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()?.string() ?: return@use
            val branList = JsonParser().parse(data).asJsonArray
            if (!branList.isJsonNull) {
                branList.forEach {
                    val branch = it.asJsonObject
                    val commit = branch["commit"].asJsonObject
                    if (!branch.isJsonNull && !commit.isJsonNull) {
                        res.add(GitBranch(name = if (branch["name"].isJsonNull) "" else branch["name"].asString,
                            commit = GitBranchCommit(
                                id = if (commit["id"].isJsonNull) "" else commit["id"].asString,
                                message = if (commit["message"].isJsonNull) "" else commit["message"].asString,
                                authoredDate = if (commit["authored_date"].isJsonNull) "" else commit["authored_date"].asString,
                                authorEmail = if (commit["author_email"].isJsonNull) "" else commit["author_email"].asString,
                                authorName = if (commit["author_name"].isJsonNull) "" else commit["author_name"].asString,
                                title = if (commit["title"].isJsonNull) "" else commit["title"].asString
                            )))
                    }
                }
            }
        }
        return res
    }

    fun getTag(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): List<GitTag> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get the $userId's $repository tag by accessToken: $accessToken  page: $pageNotNull pageSize: $pageSizeNotNull")
        val repoId = URLEncoder.encode(repository, "utf-8")
        val url = "${gitConfig.gitApiUrl}/projects/$repoId/repository/tags?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
        val res = mutableListOf<GitTag>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()?.string() ?: return@use
            val tagList = JsonParser().parse(data).asJsonArray
            if (!tagList.isJsonNull) {
                tagList.forEach {
                    val tag = it.asJsonObject
                    val commit = tag["commit"].asJsonObject
                    if (!tag.isJsonNull && !commit.isJsonNull) {
                        res.add(GitTag(name = if (tag["name"].isJsonNull) "" else tag["name"].asString, message = if (tag["message"].isJsonNull) "" else tag["message"].asString,
                            commit = GitTagCommit(
                                id = if (commit["id"].isJsonNull) "" else commit["id"].asString,
                                message = if (commit["message"].isJsonNull) "" else commit["message"].asString,
                                authoredDate = if (commit["authored_date"].isJsonNull) "" else commit["authored_date"].asString,
                                authorName = if (commit["author_name"].isJsonNull) "" else commit["author_name"].asString,
                                authorEmail = if (commit["author_email"].isJsonNull) "" else commit["author_email"].asString
                            )
                        ))
                    }
                }
            }
        }
        return res
    }

    fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        logger.info("Start to refresh the token of user $userId by token $accessToken")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.gitUrl}/oauth/token?client_id=$clientId&client_secret=$clientSecret" +
                "&grant_type=refresh_token&refresh_token=${accessToken.refreshToken}&redirect_uri=$callbackUrl"
            val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), ""))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                logger.info("refreshToken>>> $data")
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to refresh the token")
        }
    }

    fun getAuthUrl(authParamJsonStr: String): String {
        return "${gitConfig.gitUrl}/oauth/authorize?client_id=$clientId&redirect_uri=$callbackUrl&response_type=code&state=$authParamJsonStr"
    }

    fun getToken(userId: String, code: String): GitToken {
        logger.info("Start to get the token of user $userId by code $code")
        val startEpoch = System.currentTimeMillis()
        try {
            val tokenUrl =
                "${gitConfig.gitUrl}/oauth/token?client_id=$clientId&client_secret=$clientSecret&code=$code&grant_type=authorization_code&redirect_uri=$redirectUrl"
            logger.info("getToken url>> $tokenUrl")
            val request = Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), ""))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                logger.info("getToken>> $data")
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    fun getToken(gitProjectId: String): GitToken {
        logger.info("Start to get the token for git project($gitProjectId)")
        val startEpoch = System.currentTimeMillis()
        try {
            val tokenUrl = "$gitCIOauthUrl/oauth/token?client_id=$gitCIClientId&client_secret=$gitCIClientSecret&grant_type=client_credentials&scope=project:${URLEncoder.encode(gitProjectId, "UTF8")}"
            logger.info("getToken url>> $tokenUrl")
            val request = Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), ""))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                logger.info("getToken>> $data")
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

/*    fun checkUserGitAuth(userId: String, gitProjectId: String): Boolean {
        var page = 1
        var dataSize: Int
        do {
            try {
                val token = getToken(gitProjectId)
                val url = "$gitCIOauthUrl/api/v3/projects/$gitProjectId/members?page=$page&per_page=100&access_token=${token.accessToken}"

                var ownerList = listOf<OwnerInfo>()
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                OkhttpUtils.doHttp(request).use { response ->
                    val body = response.body()!!.string()
                    logger.info("Get gongfeng project members response body: $body")
                    ownerList = JsonUtil.to(body, object : TypeReference<List<OwnerInfo>>() {})
                }

                if (ownerList.isEmpty()) {
                    break
                }
                dataSize = ownerList.size
                ownerList.forEach {
                    if (userId == it.userName && it.accessLevel!! >= 35)
                        return true
                }
                page++
            } catch (e: Exception) {
                logger.error("get project member list fail! project id: $gitProjectId", e)
                return false
            }
        } while (dataSize >= 100)

        return false
    }*/

    fun checkUserGitAuth(userId: String, gitProjectId: String): Boolean {
        try {
            val token = getToken(gitProjectId)
            val url = "$gitCIOauthUrl/api/v3/projects/$gitProjectId/members/all/$userId?access_token=${token.accessToken}"

            logger.info("[$userId]|[$gitProjectId]| Get gongfeng project member utl: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val body = response.body()!!.string()
                logger.info("[$userId]|[$gitProjectId]| Get gongfeng project member response body: $body")
                val ownerInfo = JsonUtil.to(body, OwnerInfo::class.java)
                if (ownerInfo.accessLevel!! >= 30) {
                    return true
                }
            }
        } catch (e: Exception) {
            logger.error("get gongfeng project member fail! gitProjectId: $gitProjectId", e)
            return false
        }

        return false
    }

    fun getGitCIFileContent(gitProjectId: Long, filePath: String, token: String, ref: String): String {
        logger.info("[$gitProjectId|$filePath|$token|$ref] Start to get the git file content")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "$gitCIUrl/api/v3/projects/$gitProjectId/repository/blobs/" +
                "${URLEncoder.encode(ref, "UTF-8")}?filepath=${URLEncoder.encode(filePath, "UTF-8")}" +
                "&access_token=$token"
            logger.info("request url: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                val data = it.body()!!.string()
                if (!it.isSuccessful) throw RuntimeException("fail to get git file content with: $url($data)")
                return data
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    fun getRedirectUrl(authParamJsonStr: String): String {
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

    fun getGitFileContent(repoName: String, filePath: String, authType: RepoAuthType?, token: String, ref: String): String {
        logger.info("[$repoName|$filePath|$authType|$token|$ref] Start to get the git file content")
        val startEpoch = System.currentTimeMillis()
        try {
            var url = "${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(repoName, "UTF-8")}/repository/blobs/" +
                "${URLEncoder.encode(ref, "UTF-8")}?filepath=${URLEncoder.encode(filePath, "UTF-8")}"
            logger.info("$url ($token)")
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
                val data = it.body()!!.string()
                if (!it.isSuccessful) throw RuntimeException("fail to get git file content with: $url($data)")
                return data
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

    fun getGitlabFileContent(repoName: String, filePath: String, ref: String, accessToken: String): String {
        logger.info("[$repoName|$filePath|$ref|$accessToken] Start to get the gitlab file content")
        val startEpoch = System.currentTimeMillis()
        try {
            val headers = mapOf("PRIVATE-TOKEN" to accessToken)
            // 查询文件内容
            val encodeFilePath = URLEncoder.encode(filePath, "utf-8")
            val encodeRef = URLEncoder.encode(ref, "utf-8")
            val encodeProjectName = URLEncoder.encode(repoName, "utf-8")
            val projectFileUrl = "${gitConfig.gitlabApiUrl}/projects/$encodeProjectName/repository/files/$encodeFilePath?ref=$encodeRef"
            logger.info(projectFileUrl)
            OkhttpUtils.doGet(projectFileUrl, headers).use { response ->
                val body = response.body()!!.string()
                logger.info("get gitlab content response body: $body")
                val fileInfo = objectMapper.readValue(body, GitlabFileInfo::class.java)
                return String(Base64.getDecoder().decode(fileInfo.content))
            }
        } catch (e: Exception) {
            logger.warn("Fail to get the gitlab content of repo($repoName) in path($filePath)/ref($ref): ${e.message}", e)
            return ""
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the gitlab file content")
        }
    }

    fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum?
    ): Result<GitRepositoryResp?> {
        logger.info("createGitRepository userId is:$userId,token is:$token, repositoryName is:$repositoryName, sampleProjectPath is:$sampleProjectPath")
        logger.info("createGitRepository  namespaceId is:$namespaceId, visibilityLevel is:$visibilityLevel, tokenType is:$tokenType")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects")
        setToken(tokenType, url, token)
        logger.info("createGitRepository token is:$token, url>> $url")
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
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(params)))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("createGitRepository token is:$token, response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val repositoryUrl = dataMap["http_url_to_repo"]
            if (StringUtils.isEmpty(repositoryUrl)) {
                val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_CREATE_GIT_CODE_REPOSITORY_FAIL)
                logger.info("createOAuthCodeRepository validateResult>> $validateResult")
                // 把工蜂的错误提示抛出去
                return Result(validateResult.status, "${validateResult.message}（git error:$data）")
            }
            val nameSpaceName = dataMap["name_with_namespace"] as String
            // 把需要创建项目代码库的用户加入为对应项目的owner用户
            executorService.submit<Unit> {
                // 添加开发成员
                addGitProjectMember(listOf(userId), nameSpaceName, GitAccessLevelEnum.MASTER, token, tokenType)
                if (!sampleProjectPath.isNullOrBlank()) {
                    // 把样例工程代码添加到用户的仓库
                    initRepositoryInfo(
                        userId = userId,
                        nameSpaceName = nameSpaceName,
                        sampleProjectPath = sampleProjectPath!!,
                        token = token,
                        tokenType = tokenType,
                        repositoryName = repositoryName,
                        repositoryUrl = repositoryUrl as String,
                        frontendType = frontendType
                    )
                }
            }
            return Result(GitRepositoryResp(nameSpaceName, repositoryUrl as String))
        }
    }

    fun initRepositoryInfo(
        userId: String,
        nameSpaceName: String,
        sampleProjectPath: String,
        token: String,
        tokenType: TokenTypeEnum,
        repositoryName: String,
        repositoryUrl: String,
        frontendType: FrontendTypeEnum?
    ): Result<Boolean> {
        logger.info("initRepositoryInfo userId is:$userId,sampleProjectPath is:$sampleProjectPath,repositoryUrl is:$repositoryUrl")
        logger.info("initRepositoryInfo nameSpaceName is:$nameSpaceName,token is:$token,tokenType is:$tokenType,repositoryName is:$repositoryName")
        val tmpWorkspace = Files.createTempDirectory(repositoryName).toFile()
        logger.info("initRepositoryInfo tmpWorkspace is:${tmpWorkspace.absolutePath}")
        try {
            // 1、clone插件示例工程代码到插件工作空间下
            val credentialSetter = if (tokenType == TokenTypeEnum.OAUTH) {
                CodeGitOauthCredentialSetter(token)
            } else {
                CodeGitUsernameCredentialSetter(gitPublicAccount, gitPublicSecret)
            }
            CommonScriptUtils.execute("git clone ${credentialSetter.getCredentialUrl(sampleProjectPath)}", tmpWorkspace)
            // 2、删除下载下来示例工程的git信息
            val fileDir = tmpWorkspace.listFiles()?.firstOrNull()
            logger.info("initRepositoryInfo atomFileDir is:${fileDir?.absolutePath}")
            val gitFileDir = File(fileDir, ".git")
            if (gitFileDir.exists()) {
                FileSystemUtils.deleteRecursively(gitFileDir)
            }
            // 处理示例工程的文件
            val handleFileResult =
                sampleProjectGitFileService.handleSampleProjectGitFile(nameSpaceName, repositoryName, fileDir)
            if (handleFileResult.isNotOk()) {
                return handleFileResult
            }
            // 如果用户选的是自定义UI方式开发插件，则需要初始化UI开发脚手架
            if (FrontendTypeEnum.SPECIAL == frontendType) {
                val frontendFileDir = File(fileDir, BK_FRONTEND_DIR_NAME)
                if (!frontendFileDir.exists()) {
                    frontendFileDir.mkdirs()
                }
                CommonScriptUtils.execute("git clone ${credentialSetter.getCredentialUrl(gitConfig.frontendSampleProjectUrl)}", frontendFileDir)
                val frontendProjectDir = frontendFileDir.listFiles()?.firstOrNull()
                logger.info("initRepositoryInfo frontendProjectDir is:${frontendProjectDir?.absolutePath}")
                val frontendGitFileDir = File(frontendProjectDir, ".git")
                if (frontendGitFileDir.exists()) {
                    FileSystemUtils.deleteRecursively(frontendGitFileDir)
                }
                FileSystemUtils.copyRecursively(frontendProjectDir, frontendFileDir)
                FileSystemUtils.deleteRecursively(frontendProjectDir)
            }
            // 3、重新生成git信息
            CommonScriptUtils.execute("git init", fileDir)
            // 4、添加远程仓库
            CommonScriptUtils.execute("git remote add origin ${credentialSetter.getCredentialUrl(repositoryUrl)}", fileDir)
            // 5、给文件添加git信息
            CommonScriptUtils.execute("git config user.email \"$gitPublicEmail\"", fileDir)
            CommonScriptUtils.execute("git config user.name \"$gitPublicAccount\"", fileDir)
            CommonScriptUtils.execute("git add .", fileDir)
            // 6、提交本地文件
            CommonScriptUtils.execute("git commit -m init", fileDir)
            // 7、提交代码到远程仓库
            CommonScriptUtils.execute("git push origin master", fileDir)
            logger.info("initRepositoryInfo finish")
        } catch (e: Exception) {
            logger.error("initRepositoryInfo error is:", e)
            return Result(false)
        } finally {
            FileSystemUtils.deleteRecursively(tmpWorkspace)
        }
        return Result(true)
    }

    fun addGitProjectMember(userIdList: List<String>, repoName: String, gitAccessLevel: GitAccessLevelEnum, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        logger.info("addGitProjectMember token is:$token, userIdList is:$userIdList,repoName is:$repoName,gitAccessLevel is:$gitAccessLevel,tokenType is:$tokenType")
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
                .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(params)))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                logger.info("addGitProjectMember token is:$token, response>> $data")
                if (!StringUtils.isEmpty(data)) {
                    val dataMap = JsonUtil.toMap(data)
                    val message = dataMap["message"]
                    if (!StringUtils.isEmpty(message)) {
                        val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_ADD_GIT_CODE_REPOSITORY_MEMBER_FAIL, arrayOf(it))
                        logger.info("addGitProjectMember validateResult>> $validateResult")
                        // 把工蜂的错误提示抛出去
                        return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                    }
                }
            }
        }
        return Result(true)
    }

    fun deleteGitProjectMember(userIdList: List<String>, repoName: String, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        logger.info("deleteGitProjectMember token is:$token, userIdList is:$userIdList,repoName is:$repoName,tokenType is:$tokenType")
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
                    val data = response.body()!!.string()
                    logger.info("deleteGitProjectMember token is:$token, response>> $data")
                    if (!StringUtils.isEmpty(data)) {
                        val dataMap = JsonUtil.toMap(data)
                        val message = dataMap["message"]
                        if (!StringUtils.isEmpty(message)) {
                            val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_DELETE_GIT_CODE_REPOSITORY_MEMBER_FAIL, arrayOf(it))
                            logger.info("deleteGitProjectMember validateResult>> $validateResult")
                            // 把工蜂的错误提示抛出去
                            return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                        }
                    }
                }
            }
        }
        return Result(true)
    }

    fun getGitProjectMemberInfo(memberId: Int, repoName: String, token: String, tokenType: TokenTypeEnum): Result<GitUserInfo?> {
        logger.info("getGitProjectMemberInfo memberId is:$memberId,repoName is:$repoName,token is:$token,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/members/$memberId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("getGitProjectMemberInfo token is:$token, response>> $data")
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

    fun deleteGitProject(repoName: String, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        logger.info("deleteGitProject repoName is:$repoName,token is:$token,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8") // 为代码库名称字段encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("deleteGitProject token is:$token, response>> $data")
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> =
                        MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_UPDATE_GIT_CODE_REPOSITORY_FAIL)
                    // 把工蜂的错误提示抛出去
                    return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                }
            }
            return Result(data = true)
        }
    }

    fun getGitUserInfo(userId: String, token: String, tokenType: TokenTypeEnum): Result<GitUserInfo?> {
        logger.info("getGitUserInfo token is:$token, userId is:$userId,tokenType is:$tokenType")
        val url = StringBuilder("${gitConfig.gitApiUrl}/users/$userId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("getGitUserInfo token is:$token, response>> $data")
            if (!it.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
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

    fun getGitProjectInfo(id: String, token: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        logger.info("getGitUserInfo token is:$token, id is:$id,tokenType is:$tokenType")
        val encodeId = URLEncoder.encode(id, "utf-8") // 如果id为NAMESPACE_PATH则需要encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("GitProjectInfo token is:$token, response>> $data")
            if (!it.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            return Result(JsonUtil.to(data, GitProjectInfo::class.java))
        }
    }

    fun getGitRepositoryTreeInfo(
        userId: String,
        repoName: String,
        refName: String?,
        path: String?,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?> {
        logger.info("getGitRepositoryTreeInfo userId is:$userId,repoName is:$repoName,refName is:$refName")
        logger.info("getGitRepositoryTreeInfo path is:$path,token is:$token,tokenType is:$tokenType")
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
            val data = it.body()!!.string()
            logger.info("getGitRepositoryTreeInfo token is:$token, response>> $data")
            if (!StringUtils.isEmpty(data)) {
                var message: String? = null
                if (data.contains("\"message\":")) {
                    val dataMap = JsonUtil.toMap(data)
                    message = dataMap["message"] as? String
                }
                return if (StringUtils.isEmpty(message)) {
                    Result(JsonUtil.to(data, object : TypeReference<List<GitRepositoryDirItem>>() {}))
                } else {
                    val result: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.GIT_REPO_PEM_FAIL)
                    // 把工蜂的错误提示抛出去
                    Result(result.status, "${result.message}（git error:$message）")
                }
            }
            return Result(data = null)
        }
    }

    fun getGitCIProjectInfo(gitProjectId: String, token: String): Result<GitCIProjectInfo?> {
        logger.info("[gitProjectId=$gitProjectId]|getGitCIProjectInfo with token=$token")
        val encodeId = URLEncoder.encode(gitProjectId, "utf-8") // 如果id为NAMESPACE_PATH则需要encode
        val url = StringBuilder("$gitCIUrl/api/v3/projects/$encodeId?access_token=$token")
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val response = it.body()!!.string()
            logger.info("[gitProjectId=$gitProjectId]|getGitCIProjectInfo with response=$response")
            if (!it.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            return Result(JsonUtil.to(response, GitCIProjectInfo::class.java))
        }
    }

    fun updateGitProjectInfo(projectName: String, updateGitProjectInfo: UpdateGitProjectInfo, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        logger.info("updateGitProjectInfo token is:$token, projectName is:$projectName,updateGitProjectInfo is:$updateGitProjectInfo,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(projectName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .put(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(updateGitProjectInfo)))
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("updateGitProjectInfo token is:$token, response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val message = dataMap["message"]
            if (!StringUtils.isEmpty(message)) {
                val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_UPDATE_GIT_CODE_REPOSITORY_FAIL)
                logger.info("updateGitProjectInfo validateResult>> $validateResult")
                // 把工蜂的错误提示抛出去
                return Result(validateResult.status, "${validateResult.message}（git error:$message）")
            }
            return Result(true)
        }
    }

    fun moveProjectToGroup(groupCode: String, repoName: String, token: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        logger.info("updateGitProjectInfo token is:$token, groupCode is:$groupCode,repoName is:$repoName,tokenType is:$tokenType")
        val gitProjectInfo: GitProjectInfo?
        val gitProjectInfoResult = getGitProjectInfo(repoName, token, tokenType)
        logger.info("the gitProjectInfoResult is :$gitProjectInfoResult")
        if (gitProjectInfoResult.isNotOk()) {
            return Result(status = gitProjectInfoResult.status, message = gitProjectInfoResult.message ?: "")
        } else {
            gitProjectInfo = gitProjectInfoResult.data
        }
        if (null == gitProjectInfo) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(repoName))
        }
        val projectId = gitProjectInfo.id // 获取工蜂项目ID
        val url = StringBuilder("${gitConfig.gitApiUrl}/groups/$groupCode/projects/$projectId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(mapOf<String, String>())))
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                val data = it.body()!!.string()
                logger.info("moveProjectToGroup token is:$token, response>> $data")
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                return if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL, arrayOf(groupCode))
                    logger.info("moveProjectToGroup validateResult>> $validateResult")
                    // 把工蜂的错误提示抛出去
                    Result(validateResult.status, "${validateResult.message}（git error:$message）")
                } else {
                    MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL, arrayOf(groupCode))
                }
            }
            return Result(getGitProjectInfo(projectId.toString(), token, tokenType).data)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    fun getMrInfo(
        id: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrInfo {
        val url = StringBuilder("${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(id, "UTF-8")}/merge_request/$mrId")
        logger.info("get mr info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw RuntimeException("get merge request info error for $id, $mrId(${it.code()}): ${it.message()}")
            }
            val data = it.body()!!.string()
            logger.info("get mr info response body: $data")
            return JsonUtil.to(data, GitMrInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    fun getMrReviewInfo(
        id: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrReviewInfo {
        val url = StringBuilder("${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(id, "UTF-8")}/merge_request/$mrId/review")
        logger.info("get mr review info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw RuntimeException("get merge reviewers request info error for $id, $mrId(${it.code()}): ${it.message()}")
            }
            val data = it.body()!!.string()
            logger.info("get mr review info response body: $data")
            return JsonUtil.to(data, GitMrReviewInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    fun getMrChangeInfo(
        id: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrChangeInfo {
        val url = StringBuilder("${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(id, "UTF-8")}/merge_request/$mrId/changes")
        logger.info("get mr changes info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw RuntimeException("get merge changes request info error for $id, $mrId(${it.code()}): ${it.message()}")
            }
            val data = it.body()!!.string()
            logger.info("get mr changes info response body: $data")
            return JsonUtil.to(data, GitMrChangeInfo::class.java)
        }
    }

    private fun getApiUrl(repoUrl: String?): String {
        return if (repoUrl.isNullOrBlank()) {
            gitConfig.gitApiUrl
        } else {
            GitUtils.getGitApiUrl(gitConfig.gitApiUrl, repoUrl!!)
        }
    }

    fun downloadGitRepoFile(repoName: String, sha: String?, token: String, tokenType: TokenTypeEnum, response: HttpServletResponse) {
        logger.info("downloadGitRepoFile token is:$token, repoName is:$repoName,sha is:$sha,tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, "utf-8")
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/archive")
        setToken(tokenType, url, token)
        if (!sha.isNullOrBlank()) {
            url.append("&sha=$sha")
        }
        OkhttpUtils.downloadFile(url.toString(), response)
    }

    fun addCommitCheck(request: CommitCheckRequest) {
        val startEpoch = System.currentTimeMillis()
        try {
            with(request) {
                if (token == null || token == "") {
                    throw RuntimeException("Git Token为空")
                }
                gitOauthApi.addCommitCheck(
                    "$gitCIUrl/api/v3",
                    token!!, projectName, commitId, state, targetUrl, context, description, block)
            }
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add commit check")
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    fun getRepoMembers(repoName: String, tokenType: TokenTypeEnum, token: String): List<GitMember> {
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(repoName, "UTF-8")}/members")
        logger.info("get repo member url: $url")
        setToken(tokenType, url, token)

        val result = mutableListOf<GitMember>()
        // 限制最多50页
        for (page in 1..10) {
            val request = Request.Builder()
                .url("$url&page=$page&per_page=1000")
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw RuntimeException("get repo member error for $repoName(${it.code()}): ${it.message()}")
                }
                val data = it.body()!!.string()
                logger.info("get repo member response body: $data")
                val pageResult = JsonUtil.to(data, object : TypeReference<List<GitMember>>() {})
                result.addAll(pageResult)
                if (pageResult.size < 1000) return result
            }
        }
        return result
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    fun getRepoAllMembers(repoName: String, tokenType: TokenTypeEnum, token: String): List<GitMember> {
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/${URLEncoder.encode(repoName, "UTF-8")}/members/all")
        logger.info("get repo member url: $url")
        setToken(tokenType, url, token)

        val result = mutableListOf<GitMember>()
        // 限制最多50页
        for (page in 1..10) {
            val request = Request.Builder()
                .url("$url&page=$page&per_page=1000")
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    throw RuntimeException("get repo member error for $repoName(${it.code()}): ${it.message()}")
                }
                val data = it.body()!!.string()
                logger.info("get repo member response body: $data")
                val pageResult = JsonUtil.to(data, object : TypeReference<List<GitMember>>() {})
                result.addAll(pageResult)
                if (pageResult.size < 1000) return result
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
}
