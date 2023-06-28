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
import com.tencent.devops.common.api.constant.CommonMessageCode
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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_ADD_CHECK_RUNS
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_GET_BRANCH
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_GET_REPOS
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_GET_TAG
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_LIST_BRANCHS
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_LIST_TAGS
import com.tencent.devops.repository.constant.RepositoryMessageCode.OPERATION_UPDATE_CHECK_RUNS
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
        val operation = getMessageByLocale(OPERATION_ADD_CHECK_RUNS)
        return callMethod(operation, request, GithubCheckRunsResponse::class.java)
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
        val operation = getMessageByLocale(OPERATION_UPDATE_CHECK_RUNS)
        callMethod(operation, request, GithubCheckRunsResponse::class.java)
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
        val operation = getMessageByLocale(OPERATION_GET_REPOS)
        run outside@{
            while (page < PAGE_SIZE) {
                page++
                val request = buildGet(token, "user/repos?page=$page&per_page=$PAGE_SIZE")
                val body = getBody(operation, request)
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
                    logger.warn("BKSystemMonitor|getBranch fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): GithubBranch? {
                    val sBranch = branch ?: "master"
                    val path = "repos/$projectName/branches/$sBranch"
                    val request = buildGet(token, path)
                    val operation = getMessageByLocale(OPERATION_GET_BRANCH)
                    val body = getBody(operation, request)
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
                    logger.warn("BKSystemMonitor|getTag fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): GithubTag? {
                    val path = "repos/$projectName/git/refs/tags/$tag"
                    val request = buildGet(token, path)
                    val operation = getMessageByLocale(OPERATION_GET_TAG)
                    val body = getBody(operation, request)
                    return objectMapper.readValue(body)
                }
            },
            1, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    override fun getFileContent(projectName: String, ref: String, filePath: String): String {
        val url = "https://raw.githubusercontent.com/$projectName/$ref/$filePath"
        OkhttpUtils.doGet(url).use {
            logger.info("github content url: $url")
            if (!it.isSuccessful) {
                throw CustomException(
                    status = Response.Status.fromStatusCode(it.code) ?: Response.Status.BAD_REQUEST,
                    message = it.body!!.toString()
                )
            }
            return it.body!!.string()
        }
    }

    override fun listBranches(token: String, projectName: String): List<String> {
        logger.info("listBranches| $projectName")
        return RetryUtils.execute(
            object : RetryUtils.Action<List<String>> {
                override fun fail(e: Throwable): List<String> {
                    logger.warn("BKSystemMonitor|listBranches fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): List<String> {
                    val path = "repos/$projectName/branches?page=1&per_page=100"
                    val request = buildGet(token, path)
                    val operation = getMessageByLocale(OPERATION_LIST_BRANCHS)
                    val body = getBody(operation, request)
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
                    logger.warn("BKSystemMonitor|listTags fail| e=${e.message}", e)
                    throw e
                }

                override fun execute(): List<String> {
                    val path = "repos/$projectName/tags?page=1&per_page=100"
                    val request = buildGet(token, path)
                    val operation = getMessageByLocale(OPERATION_LIST_TAGS)
                    val body = getBody(operation, request)
                    return objectMapper.readValue<List<GithubRepoTag>>(body).map { it.name }
                }
            },
            3, SLEEP_MILLS_FOR_RETRY_500
        )
    }

    private fun buildPost(token: String, path: String, body: String): Request {
        return request(token, path)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()
    }

    private fun buildPatch(token: String, path: String, body: String): Request {
        return request(token, path)
            .patch(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
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
            val code = response.code
            val message = response.message
            val body = response.body?.string() ?: ""
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
            val code = response.code
            val message = response.message
            val body = response.body?.string() ?: ""
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
            HTTP_400 -> getMessageByLocale(CommonMessageCode.PARAM_ERROR)
            HTTP_401 -> getMessageByLocale(CommonMessageCode.AUTH_FAIL, arrayOf("GitHub token"))
            HTTP_403 -> getMessageByLocale(CommonMessageCode.ACCOUNT_NO_OPERATION_PERMISSIONS, arrayOf(operation))
            HTTP_404 -> getMessageByLocale(
                CommonMessageCode.REPO_NOT_EXIST_OR_NO_OPERATION_PERMISSION,
                arrayOf("GitHub", operation)
            )
            else -> "GitHub platform $operation fail"
        }
        throw GithubApiException(
            code = code,
            message = msg
        )
    }

    private fun getMessageByLocale(messageCode: String, params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = messageCode,
            params = params
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GithubService::class.java)
        private const val PAGE_SIZE = 100
        private const val SLEEP_MILLS_FOR_RETRY_500: Long = 500
        private const val GITHUB_API_URL = "https://api.github.com"
    }
}
