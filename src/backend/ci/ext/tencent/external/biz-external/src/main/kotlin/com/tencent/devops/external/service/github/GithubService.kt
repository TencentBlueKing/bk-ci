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

package com.tencent.devops.external.service.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.external.pojo.GithubRepository
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubRepo
import com.tencent.devops.repository.pojo.github.GithubRepoBranch
import com.tencent.devops.repository.pojo.github.GithubRepoTag
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.scm.exception.GithubApiException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
@Suppress("ALL")
class GithubService @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper
) {
    @Value("\${github.signSecret}")
    private lateinit var signSecret: String

    private val GITHUB_API_URL = "https://api.github.com"

    fun webhookCommit(event: String, guid: String, signature: String, body: String) {
        try {
            val removePrefixSignature = signature.removePrefix("sha1=")
            val genSignature = ShaUtils.hmacSha1(signSecret.toByteArray(), body.toByteArray())
            logger.info("signature($removePrefixSignature) and generate signature ($genSignature)")
            if (!ShaUtils.isEqual(removePrefixSignature, genSignature)) {
                logger.warn("signature($removePrefixSignature) and generate signature ($genSignature) not match")
                return
            }
            client.get(ServiceScmWebhookResource::class).webHookCodeGithubCommit(
                GithubWebhook(event, guid, removePrefixSignature, body)
            )
        } catch (t: Throwable) {
            logger.info("Github webhook exception", t)
        }
    }

    fun addCheckRuns(
        token: String,
        projectName: String,
        checkRuns: GithubCheckRuns
    ): GithubCheckRunsResponse {
        logger.info("Github add check [projectName=$projectName, checkRuns=$checkRuns]")

        if ((checkRuns.conclusion != null && checkRuns.completedAt == null) ||
                (checkRuns.conclusion == null && checkRuns.completedAt != null)) {
            logger.warn("conclusion and completedAt must be null or not null together")
        }

        val body = objectMapper.writeValueAsString(checkRuns)
        val request = buildPost(token, "repos/$projectName/check-runs", body)

        return callMethod(OPERATION_ADD_CHECK_RUNS, request, GithubCheckRunsResponse::class.java)
    }

    fun updateCheckRuns(
        token: String,
        projectName: String,
        checkRunId: Long,
        checkRuns: GithubCheckRuns
    ) {
        logger.info("Github add check [projectName=$projectName, checkRuns=$checkRuns]")

        if ((checkRuns.conclusion != null && checkRuns.completedAt == null) ||
            (checkRuns.conclusion == null && checkRuns.completedAt != null)) {
            logger.warn("conclusion and completedAt must be null or not null together")
        }

        val body = objectMapper.writeValueAsString(checkRuns)
        val request = buildPatch(token, "repos/$projectName/check-runs/$checkRunId", body)

        callMethod(OPERATION_UPDATE_CHECK_RUNS, request, GithubCheckRunsResponse::class.java)
    }

    fun getRepositories(token: String): List<GithubRepository> {
        val githubRepos = mutableListOf<GithubRepo>()
        val perPage = 100
        var page = 0
        run outside@{
            while (page < 100) {
                page++
                val request = buildGet(token, "user/repos?page=$page&per_page=$perPage")
                val body = getBody(OPERATION_GET_REPOS, request)
                val repos = objectMapper.readValue<List<GithubRepo>>(body)
                githubRepos.addAll(repos)

                if (repos.size < perPage) {
                    return@outside
                }
            }
        }
        logger.info("GitHub get repos($githubRepos)")

        val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
        return githubRepos.map {
            GithubRepository(
                id = it.id.toString(),
                name = it.name,
                fullName = it.fullName,
                sshUrl = it.sshUrl,
                httpUrl = it.httpUrl,
                updatedAt = ZonedDateTime.parse(it.updateAt, formatter).toEpochSecond() * 1000L
            )
        }
    }

    fun getFileContent(projectName: String, ref: String, filePath: String): String {
        val url = "https://raw.githubusercontent.com/$projectName/$ref/$filePath"
        OkhttpUtils.doGet(url).use {
            logger.info("github content url: $url")
            if (!it.isSuccessful) throw RuntimeException("get github file fail")
            return it.body!!.string()
        }
    }

    fun getBranch(token: String, projectName: String, branch: String?): GithubBranch? {
        logger.info("getBranch| $projectName - $branch")
        return RetryUtils.execute(object : RetryUtils.Action<GithubBranch?> {
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
        }, 1, 500)
    }

    fun getTag(token: String, projectName: String, tag: String): GithubTag? {
        logger.info("getTag| $projectName - $tag")
        return RetryUtils.execute(object : RetryUtils.Action<GithubTag?> {
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
        }, 1, 500)
    }

    fun listBranches(token: String, projectName: String): List<String> {
        logger.info("listBranches| $projectName")
        return RetryUtils.execute(object : RetryUtils.Action<List<String>> {
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
        }, 3, 500)
    }

    fun listTags(token: String, projectName: String): List<String> {
        logger.info("listTags| $projectName")
        return RetryUtils.execute(object : RetryUtils.Action<List<String>> {
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
        }, 3, 500)
    }

    private fun buildGet(token: String, path: String): Request {
        return request(token, path)
                .get()
                .build()
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

    private fun request(token: String, path: String): Request.Builder {
        return Request.Builder()
            .url("$GITHUB_API_URL/$path")
            .header("Authorization", "token $token")
            .header("Accept", " application/vnd.github.antiope-preview+json")
    }

    private fun getBody(operation: String, request: Request): String {
        OkhttpUtils.doHttp(request).use { response ->
//        okHttpClient.newCall(request).execute().use { response ->
            val code = response.code
            val message = response.message
            val body = response.body?.string() ?: ""

            logger.info("getBody operation($operation). response code($code) message($message) body($body)")
            if (!response.isSuccessful) {
                handException(operation, code)
            }
            return body
        }
    }

    private fun <T> callMethod(operation: String, request: Request, classOfT: Class<T>): T {
//        okHttpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val code = response.code
            val message = response.message
            val body = response.body?.string() ?: ""

            logger.info("callMethod operation($operation). response code($code) message($message) body($body)")
            if (!response.isSuccessful) {
                handException(operation, code)
            }
            return objectMapper.readValue(body, classOfT)
        }
    }

    private fun handException(operation: String, code: Int) {
        when (code) {
            400 -> throw GithubApiException(code, "参数错误")
            401 -> throw GithubApiException(code, "GitHub认证失败")
            403 -> throw GithubApiException(code, "账户没有${operation}的权限")
            404 -> throw GithubApiException(code, "GitHub仓库不存在或者是账户没有该项目${operation}的权限")
            else -> throw GithubApiException(code, "GitHub平台${operation}失败")
        }
    }

    private val OPERATION_ADD_CHECK_RUNS = "添加检测任务"
    private val OPERATION_UPDATE_CHECK_RUNS = "更新检测任务"
    private val OPERATION_GET_REPOS = "获取仓库列表"
    private val OPERATION_GET_BRANCH = "获取指定分支"
    private val OPERATION_GET_TAG = "获取指定Tag"
    private val OPERATION_LIST_BRANCHS = "获取分支列表"
    private val OPERATION_LIST_TAGS = "获取Tag列表"

    companion object {
        private val logger = LoggerFactory.getLogger(GithubService::class.java)
//        private val okHttpClient = okhttp3.OkHttpClient.Builder()
//                .connectTimeout(5L, TimeUnit.SECONDS)
//                .readTimeout(60L, TimeUnit.SECONDS)
//                .writeTimeout(60L, TimeUnit.SECONDS)
//                .build()
    }
}
