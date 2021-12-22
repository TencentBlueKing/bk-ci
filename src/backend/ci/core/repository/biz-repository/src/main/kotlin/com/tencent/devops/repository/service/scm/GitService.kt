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
import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.stringLimit
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
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
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
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Base64
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response

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

    private val redirectUrl: String = gitConfig.redirectUrl

    private val executorService = Executors.newFixedThreadPool(2)

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
                    val data = response.body()!!.string()
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
                                httpUrl = obj["http_url_to_repo"].asString,
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

    override fun getProjectList(accessToken: String, userId: String, page: Int?, pageSize: Int?): List<Project> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val url = "${gitConfig.gitApiUrl}/projects" +
            "?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
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
                    res.add(
                        Project(
                            id = project["id"].asString,
                            name = project["name"].asString,
                            nameWithNameSpace = project["name_with_namespace"].asString,
                            sshUrl = project["ssh_url_to_repo"].asString,
                            httpUrl = project["http_url_to_repo"].asString,
                            lastActivity = DateTimeUtil.convertLocalDateTimeToTimestamp(
                                LocalDateTime.parse(lastActivityTime)) * 1000L
                        ))
                }
            }
        }
        return res
    }

    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?
    ): List<GitBranch> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val repoId = URLEncoder.encode(repository, "utf-8")
        val url = "${gitConfig.gitApiUrl}/projects/$repoId/repository/branches" +
            "?access_token=$accessToken&page=$pageNotNull&per_page=$pageSizeNotNull"
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
            val data = response.body()?.string() ?: return@use
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
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), ""))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to refresh the token")
        }
    }

    override fun getAuthUrl(authParamJsonStr: String): String {
        return "${gitConfig.gitUrl}/oauth/authorize?client_id=${gitConfig.clientId}" +
            "&redirect_uri=${gitConfig.callbackUrl}&response_type=code&state=$authParamJsonStr"
    }

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
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), ""))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    override fun getUserInfoByToken(token: String): GitUserInfo {
        logger.info("Start to get the user info by token[$token]")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.gitUrl}/user?access_token=$token"
            logger.info("getToken url>> $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                return objectMapper.readValue(data, GitUserInfo::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

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
            GitUtils.getGitApiUrl(gitConfig.gitApiUrl, repoUrl!!)
        }
        logger.info("[$repoName|$filePath|$authType|$ref] Start to get the git file content from $apiUrl")
        val startEpoch = System.currentTimeMillis()
        try {
            var url =
                "$apiUrl/projects/${URLEncoder.encode(repoName, "UTF-8")}/repository/blobs/" +
                    "${URLEncoder.encode(ref, "UTF-8")}?filepath=${URLEncoder.encode(filePath, "UTF-8")}"
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
                val data = it.stringLimit(readLimit = MAX_FILE_SIZE, errorMsg = "请求文件不能超过1M")
                if (!it.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                        message = "fail to get git file content with: $url($data)"
                    )
                }
                return data
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the git file content")
        }
    }

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
                val body = response.stringLimit(readLimit = MAX_FILE_SIZE, errorMsg = "请求文件不能超过1M")
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
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(params)))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            val dataMap = JsonUtil.toMap(data)
            val atomRepositoryUrl = dataMap["http_url_to_repo"]
            if (StringUtils.isEmpty(atomRepositoryUrl)) {
                val validateResult: Result<String?> =
                    MessageCodeUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_CREATE_GIT_CODE_REPOSITORY_FAIL
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
                        sampleProjectPath = sampleProjectPath!!,
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
                .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JsonUtil.toJson(params)))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                if (!StringUtils.isEmpty(data)) {
                    val dataMap = JsonUtil.toMap(data)
                    val message = dataMap["message"]
                    if (!StringUtils.isEmpty(message)) {
                        val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(
                            RepositoryMessageCode.USER_ADD_GIT_CODE_REPOSITORY_MEMBER_FAIL,
                            arrayOf(it)
                        )

                        return Result(validateResult.status, "${validateResult.message}（git error:$message）")
                    }
                }
            }
        }
        return Result(true)
    }

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
                    val data = response.body()!!.string()
                    logger.info("deleteGitProjectMember response>> $data")
                    if (!StringUtils.isEmpty(data)) {
                        val dataMap = JsonUtil.toMap(data)
                        val message = dataMap["message"]
                        if (!StringUtils.isEmpty(message)) {
                            val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(
                                RepositoryMessageCode.USER_DELETE_GIT_CODE_REPOSITORY_MEMBER_FAIL,
                                arrayOf(it)
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
            val data = it.body()!!.string()
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
            val data = it.body()!!.string()
            logger.info("deleteGitProject  response>> $data")
            if (!StringUtils.isEmpty(data)) {
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(
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

    fun getGitUserInfo(userId: String, token: String, tokenType: TokenTypeEnum): Result<GitUserInfo?> {
        logger.info("getGitUserInfo userId is:$userId,tokenType is:$tokenType")
        val url = StringBuilder("${gitConfig.gitApiUrl}/users/$userId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("getGitUserInfo response>> $data")
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
        logger.info("getGitUserInfo id is:$id,tokenType is:$tokenType")
        val encodeId = URLEncoder.encode(id, "utf-8") // 如果id为NAMESPACE_PATH则需要encode
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("GitProjectInfo response>> $data")
            if (!it.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            return Result(JsonUtil.to(data, GitProjectInfo::class.java))
        }
    }

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
            val data = it.body()!!.string()
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
                    val result: Result<String?> = MessageCodeUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.GIT_REPO_PEM_FAIL
                    )
                    Result(result.status, "${result.message}（git error:$message）")
                }
            }
            return Result(data = null)
        }
    }

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
                    MediaType.parse("application/json;charset=utf-8"),
                    JsonUtil.toJson(updateGitProjectInfo)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("updateGitProjectInfo response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val message = dataMap["message"]
            if (!StringUtils.isEmpty(message)) {
                val validateResult: Result<String?> =
                    MessageCodeUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.USER_UPDATE_GIT_CODE_REPOSITORY_FAIL
                    )
                logger.info("updateGitProjectInfo validateResult>> $validateResult")

                return Result(validateResult.status, "${validateResult.message}（git error:$message）")
            }
            return Result(true)
        }
    }

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
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(repoName))
        }
        val projectId = gitProjectInfo.id // 获取工蜂项目ID
        val url = StringBuilder("${gitConfig.gitApiUrl}/groups/$groupCode/projects/$projectId")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .post(
                RequestBody.create(
                    MediaType.parse("application/json;charset=utf-8"),
                    JsonUtil.toJson(mapOf<String, String>())
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                val data = it.body()!!.string()
                logger.info("moveProjectToGroup response>> $data")
                val dataMap = JsonUtil.toMap(data)
                val message = dataMap["message"]
                return if (!StringUtils.isEmpty(message)) {
                    val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(
                        RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL,
                        arrayOf(groupCode)
                    )
                    logger.info("moveProjectToGroup validateResult>> $validateResult")
                    Result(validateResult.status, "${validateResult.message}（git error:$message）")
                } else {
                    MessageCodeUtil.generateResponseDataObject(
                        RepositoryMessageCode.USER_GIT_REPOSITORY_MOVE_GROUP_FAIL,
                        arrayOf(groupCode)
                    )
                }
            }
            return Result(getGitProjectInfo(projectId.toString(), token, tokenType).data)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
    override fun getMrInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrInfo {
        val url = StringBuilder("${getApiUrl(repoUrl)}/projects/${URLEncoder.encode(repoName, "UTF-8")}" +
            "/merge_request/$mrId")
        logger.info("get mr info url: $url")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = "get merge info error for $repoName, $mrId(${it.code()}): ${it.message()}"
                )
            }
            val data = it.body()!!.string()
            logger.info("get mr info response body: $data")
            return JsonUtil.to(data, GitMrInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
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
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = "get merge reviewers info error for $id, $mrId(${it.code()}): ${it.message()}"
                )
            }
            val data = it.body()!!.string()
            return JsonUtil.to(data, GitMrReviewInfo::class.java)
        }
    }

    // id = 项目唯一标识或NAMESPACE_PATH/PROJECT_PATH
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
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = "get merge changes request info error for $id, $mrId(${it.code()}): ${it.message()}"
                )
            }
            val data = it.body()!!.string()
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

    fun setToken(tokenType: TokenTypeEnum, url: StringBuilder, token: String) {
        if (TokenTypeEnum.OAUTH == tokenType) {
            url.append("?access_token=$token")
        } else {
            url.append("?private_token=$token")
        }
    }

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
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = "get repo members for $userId, $repoName fail(${it.code()}): ${it.message()}"
                )
            }
            val data = it.body()!!.string()
            return JsonUtil.to(data)
        }
    }

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
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = "get repo all members for $userId, $repoName fail(${it.code()}): ${it.message()}"
                )
            }
            val data = it.body()!!.string()
            return JsonUtil.to(data)
        }
    }

    override fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        logger.info("getRepoRecentCommitInfo repoName:$repoName, sha:$sha, tokenType is:$tokenType")
        val encodeProjectName = URLEncoder.encode(repoName, Charsets.UTF_8.name())
        val url = StringBuilder("${gitConfig.gitApiUrl}/projects/$encodeProjectName/repository/commits/$sha")
        setToken(tokenType, url, token)
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            if (!it.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
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
                    MediaType.parse("application/json;charset=utf-8"),
                    JsonUtil.toJson(params)
                )
            )
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            logger.info("createGitTag response>> $data")
            val dataMap = JsonUtil.toMap(data)
            val message = dataMap["message"]
            if (!StringUtils.isEmpty(message)) {
                val validateResult: Result<String?> =
                    MessageCodeUtil.generateResponseDataObject(
                        messageCode = RepositoryMessageCode.CREATE_TAG_FAIL
                    )
                logger.info("createGitTag validateResult>> $validateResult")

                return Result(validateResult.status, "${validateResult.message}（git error:$message）")
            }
            return Result(true)
        }
    }
}
