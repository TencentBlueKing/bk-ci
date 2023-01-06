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

package com.tencent.devops.repository.service.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubRepo
import com.tencent.devops.repository.pojo.github.GithubRepoBranch
import com.tencent.devops.repository.pojo.github.GithubRepoTag
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.exception.GithubApiException
import com.tencent.devops.scm.pojo.Project
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
@Suppress("ALL")
class GithubService @Autowired constructor(
    private val githubTokenService: GithubTokenService,
    private val githubOAuthService: GithubOAuthService,
    private val objectMapper: ObjectMapper,
    private val gitConfig: GitConfig,
    private val client: Client
) : IGithubService {

    override fun webhookCommit(event: String, guid: String, signature: String, body: String) {
        try {
            val removePrefixSignature = signature.removePrefix("sha1=")
            val genSignature = ShaUtils.hmacSha1(gitConfig.signSecret.toByteArray(), body.toByteArray())
            logger.info("signature($removePrefixSignature) and generate signature ($genSignature)")
            if (!ShaUtils.isEqual(removePrefixSignature, genSignature)) {
                logger.warn("signature($removePrefixSignature) and generate signature ($genSignature) not match")
                return
            }

            client.get(ServiceScmWebhookResource::class)
                .webHookCodeGithubCommit(GithubWebhook(event, guid, removePrefixSignature, body))
        } catch (t: Throwable) {
            logger.info("Github webhook exception", t)
        }
    }

    override fun addCheckRuns(
        token: String,
        projectName: String,
        checkRuns: GithubCheckRuns
    ): GithubCheckRunsResponse {
        logger.info("Github add check [projectName=$projectName, checkRuns=$checkRuns]")

        if ((checkRuns.conclusion != null && checkRuns.completedAt == null) ||
            (checkRuns.conclusion == null && checkRuns.completedAt != null)
        ) {
            logger.warn("conclusion and completedAt must be null or not null together")
        }

        val body = objectMapper.writeValueAsString(checkRuns)
        val request = buildPost(token, "repos/$projectName/check-runs", body)

        return callMethod(OPERATION_ADD_CHECK_RUNS, request, GithubCheckRunsResponse::class.java)
    }

    override fun updateCheckRuns(
        token: String,
        projectName: String,
        checkRunId: Long,
        checkRuns: GithubCheckRuns
    ) {
        logger.info("Github add check [projectName=$projectName, checkRuns=$checkRuns]")

        if ((checkRuns.conclusion != null && checkRuns.completedAt == null) ||
            (checkRuns.conclusion == null && checkRuns.completedAt != null)
        ) {
            logger.warn("conclusion and completedAt must be null or not null together")
        }

        val body = objectMapper.writeValueAsString(checkRuns)
        val request = buildPatch(token, "repos/$projectName/check-runs/$checkRunId", body)

        callMethod(OPERATION_UPDATE_CHECK_RUNS, request, GithubCheckRunsResponse::class.java)
    }

    override fun getProject(projectId: String, userId: String, repoHashId: String?): AuthorizeResult {
        val accessToken = githubTokenService.getAccessToken(userId)
        if (accessToken == null) {
            val url = githubOAuthService.getGithubOauth(projectId, userId, repoHashId).redirectUrl
            return AuthorizeResult(HTTP_403, url)
        }

        return try {
            val repos = getRepositories(accessToken.accessToken)
            val fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
            val projects = repos.map {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    nameWithNameSpace = it.fullName,
                    sshUrl = it.sshUrl,
                    httpUrl = it.httpUrl,
                    lastActivity = TimeUnit.SECONDS.toMillis(ZonedDateTime.parse(it.updateAt, fmt).toEpochSecond())
                )
            }.toMutableList()

            AuthorizeResult(HTTP_200, "", projects)
        } catch (ignored: Throwable) {
            logger.warn("Github get project fail", ignored)
            val url = githubOAuthService.getGithubOauth(projectId, userId, repoHashId).redirectUrl
            AuthorizeResult(HTTP_403, url)
        }
    }

    fun getRepositories(token: String): List<GithubRepo> {
        val githubRepos = mutableListOf<GithubRepo>()
        var page = 0
        run outside@{
            while (page < PAGE_SIZE) {
                page++
                val request = buildGet(token, "user/repos?page=$page&per_page=$PAGE_SIZE")
                val body = getBody(OPERATION_GET_REPOS, request)
                val repos = objectMapper.readValue<List<GithubRepo>>(body)
                githubRepos.addAll(repos)

                if (repos.size < PAGE_SIZE) {
                    return@outside
                }
            }
        }
        logger.info("GitHub get repos($githubRepos)")

        return githubRepos
    }

    override fun getBranch(token: String, projectName: String, branch: String?): GithubBranch? {
        logger.info("getBranch| $projectName - $branch")

        return RetryUtils.execute(
            object : RetryUtils.Action<GithubBranch?> {
                override fun fail(e: Throwable): GithubBranch? {
                    logger.error("getBranch fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): GithubBranch? {
                    val sBranch = branch ?: "master"
                    val path = "repos/$projectName/branches/$sBranch"
                    val request = buildGet(token, path)
                    val body = getBody(OPERATION_GET_BRANCH, request)
                    return objectMapper.readValue(body)
                }
            },
            1, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    override fun getTag(token: String, projectName: String, tag: String): GithubTag? {
        logger.info("getTag| $projectName - $tag")
        return RetryUtils.execute(
            object : RetryUtils.Action<GithubTag?> {
                override fun fail(e: Throwable): GithubTag? {
                    logger.error("getTag fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): GithubTag? {
                    val path = "repos/$projectName/git/refs/tags/$tag"
                    val request = buildGet(token, path)
                    val body = getBody(OPERATION_GET_TAG, request)
                    return objectMapper.readValue(body)
                }
            },
            1, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    // TODO:脱敏
    override fun getFileContent(projectName: String, ref: String, filePath: String): String {
        val url = "https://raw.githubusercontent.com/$projectName/$ref/$filePath"
        OkhttpUtils.doGet(url).use {
            logger.info("github content url: $url")
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code()) ?: Response.Status.BAD_REQUEST,
                    message = it.body()!!.toString()
                )
            }
            return it.body()!!.string()
        }
    }

    override fun listBranches(token: String, projectName: String): List<String> {
        logger.info("listBranches| $projectName")
        return RetryUtils.execute(
            object : RetryUtils.Action<List<String>> {
                override fun fail(e: Throwable): List<String> {
                    logger.error("listBranches fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): List<String> {
                    val path = "repos/$projectName/branches?page=1&per_page=100"
                    val request = buildGet(token, path)
                    val body = getBody(OPERATION_LIST_BRANCHS, request)
                    return objectMapper.readValue<List<GithubRepoBranch>>(body).map { it.name }
                }
            },
            3, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    override fun listTags(token: String, projectName: String): List<String> {
        logger.info("listTags| $projectName")
        return RetryUtils.execute(
            object : RetryUtils.Action<List<String>> {
                override fun fail(e: Throwable): List<String> {
                    logger.error("listTags fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): List<String> {
                    val path = "repos/$projectName/tags?page=1&per_page=100"
                    val request = buildGet(token, path)
                    val body = getBody(OPERATION_LIST_TAGS, request)
                    return objectMapper.readValue<List<GithubRepoTag>>(body).map { it.name }
                }
            },
            3, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    private fun buildPost(token: String, path: String, body: String): Request {
        return request(token, path)
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()
    }

    private fun buildPatch(token: String, path: String, body: String): Request {
        return request(token, path)
            .patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()
    }

    private fun buildGet(token: String, path: String): Request {
        return request(token, path)
            .get()
            .build()
    }

    private fun request(token: String, path: String): Request.Builder {
        return Request.Builder()
            .url("$GITHUB_API_URL/$path")
            .header("Authorization", "token $token")
            .header("Accept", " application/vnd.github.antiope-preview+json")
    }

    private fun getBody(operation: String, request: Request): String {
        OkhttpUtils.doHttp(request).use { response ->
            val code = response.code()
            val message = response.message()
            val body = response.body()?.string() ?: ""
            if (logger.isDebugEnabled) {
                logger.debug("getBody operation($operation). response code($code) message($message) body($body)")
            }
            if (!response.isSuccessful) {
                handException(operation, code)
            }
            return body
        }
    }

    private fun <T> callMethod(operation: String, request: Request, classOfT: Class<T>): T {
        OkhttpUtils.doHttp(request).use { response ->
            val code = response.code()
            val message = response.message()
            val body = response.body()?.string() ?: ""
            if (logger.isDebugEnabled) {
                logger.debug("callMethod operation($operation). response code($code) message($message) body($body)")
            }
            if (!response.isSuccessful) {
                handException(operation, code)
            }
            return objectMapper.readValue(body, classOfT)
        }
    }

    private fun handException(operation: String, code: Int) {
        val msg = when (code) {
            HTTP_400 -> "参数错误"
            HTTP_401 -> "GitHub认证失败"
            HTTP_403 -> "账户没有${operation}的权限"
            HTTP_404 -> "GitHub仓库不存在或者是账户没有该项目${operation}的权限"
            else -> "GitHub平台${operation}失败"
        }
        throw GithubApiException(code, msg)
    }

    // TODO:脱敏
    companion object {
        private val logger = LoggerFactory.getLogger(GithubService::class.java)
        private const val PAGE_SIZE = 100
        private const val SLEEP_MILLS_FOR_RETRY_500: Long = 500
        private const val GITHUB_API_URL = "https://api.github.com"
        private const val OPERATION_ADD_CHECK_RUNS = "添加检测任务"
        private const val OPERATION_UPDATE_CHECK_RUNS = "更新检测任务"
        private const val OPERATION_GET_REPOS = "获取仓库列表"
        private const val OPERATION_GET_BRANCH = "获取指定分支"
        private const val OPERATION_GET_TAG = "获取指定Tag"
        private const val OPERATION_LIST_BRANCHS = "获取分支列表"
        private const val OPERATION_LIST_TAGS = "获取Tag列表"
    }
}
