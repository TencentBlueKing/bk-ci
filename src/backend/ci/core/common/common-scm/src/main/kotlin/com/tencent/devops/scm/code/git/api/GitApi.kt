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

package com.tencent.devops.scm.code.git.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.constant.HTTP_405
import com.tencent.devops.common.api.constant.HTTP_422
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.prometheus.BkTimedAspect
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.TapdWorkItem
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException

@Suppress("ALL")
open class GitApi {

    companion object {
        private val logger = LoggerFactory.getLogger(GitApi::class.java)
        private const val BRANCH_LIMIT = 200
        private const val TAG_LIMIT = 200
        private const val HOOK_LIMIT = 200
    }

    private fun getMessageByLocale(messageCode: String, params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = messageCode,
            params = params
        )
    }

    fun listBranches(
        host: String,
        token: String,
        projectName: String,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<String> {
        logger.info("Start to list branches of host $host by project $projectName")
        var searchReq = "page=$page&per_page=$pageSize"
        if (!search.isNullOrBlank()) {
            searchReq = "$searchReq&search=$search"
        }
        val request =
            get(host, token, "projects/${urlEncode(projectName)}/repository/branches", searchReq)
        val result = JsonUtil.getObjectMapper().readValue<List<GitBranch>>(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_BRANCH), request)
        )
        return result.sortedByDescending { it.commit.authoredDate }.map { it.name }
    }

    fun listTags(
        host: String,
        token: String,
        projectName: String,
        search: String? = null
    ): List<String> {
        var searchReq = "page=1&per_page=100&order_by=updated&sort=desc"
        if (!search.isNullOrBlank()) {
            searchReq = "$searchReq&search=$search"
        }
        val request =
            get(host, token, "projects/${urlEncode(projectName)}/repository/tags", searchReq)
        val result: List<GitTag> = JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_TAG), request)
        )
        return result.sortedByDescending { it.commit.authoredDate }.map { it.name }
    }

    fun getBranch(host: String, token: String, projectName: String, branchName: String): GitBranch {
        val request = get(
            host = host,
            token = token,
            url = "projects/${urlEncode(projectName)}/repository/branches/${urlEncode(branchName)}",
            page = ""
        )
        return callMethod(getMessageByLocale(CommonMessageCode.OPERATION_BRANCH), request, GitBranch::class.java)
    }

    fun addWebhook(
        host: String,
        token: String,
        projectName: String,
        hookUrl: String,
        event: String?,
        secret: String? = null
    ) {
        logger.info("[$host|$projectName|$hookUrl|$event] Start add the web hook")
        val existHooks = getHooks(host, token, projectName)
        if (existHooks.isNotEmpty()) {
            existHooks.forEach {
                if (it.url == hookUrl) {
                    val exist = when (event) {
                        null -> {
                            it.pushEvents
                        }
                        CodeGitWebhookEvent.PUSH_EVENTS.value -> {
                            it.pushEvents
                        }
                        CodeGitWebhookEvent.TAG_PUSH_EVENTS.value -> {
                            it.tagPushEvents
                        }
                        CodeGitWebhookEvent.ISSUES_EVENTS.value -> {
                            it.issuesEvents
                        }
                        CodeGitWebhookEvent.NOTE_EVENTS.value -> {
                            it.noteEvents
                        }
                        CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value -> {
                            it.mergeRequestsEvents
                        }
                        CodeGitWebhookEvent.REVIEW_EVENTS.value -> {
                            it.reviewEvents
                        }
                        else -> false
                    }

                    if (exist) {
                        logger.info("The web hook url($hookUrl) and event($event) is already exist($it)")
                        if (!secret.isNullOrBlank()) {
                            updateHook(
                                host = host,
                                hookId = it.id,
                                token = token,
                                projectName = projectName,
                                hookUrl = hookUrl,
                                event = event,
                                secret = secret
                            )
                        }
                        return
                    }
                }
            }
        }

        // Add the wed hook
        addHook(host, token, projectName, hookUrl, event, secret)
    }

    fun addCommitCheck(
        host: String,
        token: String,
        projectName: String,
        commitId: String,
        state: String,
        detailUrl: String,
        context: String,
        description: String,
        block: Boolean,
        targetBranch: List<String>?
    ) {
        val params = mapOf(
            "state" to state,
            "target_url" to detailUrl,
            "description" to description,
            "context" to context,
            "block" to block,
            "target_branches" to targetBranch
        )

        val body = JsonUtil.getObjectMapper().writeValueAsString(params)
        val request = post(host, token, "projects/${urlEncode(projectName)}/commit/$commitId/statuses", body)
        try {
            callMethod(
                operation = getMessageByLocale(CommonMessageCode.OPERATION_ADD_COMMIT_CHECK),
                request = request,
                classOfT = GitCommitCheck::class.java
            )
        } catch (t: GitApiException) {
            if (t.code == 403) {
                throw GitApiException(t.code, getMessageByLocale(CommonMessageCode.COMMIT_CHECK_ADD_FAIL))
            }
            throw t
        }
    }

    fun addMRComment(host: String, token: String, projectName: String, requestId: Long, message: String) {
        val params = mapOf(
            "body" to message
        )

        val body = JsonUtil.getObjectMapper().writeValueAsString(params)
        val url = "projects/${urlEncode(projectName)}/merge_requests/$requestId/notes"
        logger.info("add mr comment for project($projectName): url($url), $params")
        val request = post(host, token, url, body)
        try {
            callMethod(
                operation = getMessageByLocale(CommonMessageCode.OPERATION_ADD_MR_COMMENT),
                request = request,
                classOfT = GitMRComment::class.java
            )
        } catch (t: GitApiException) {
            if (t.code == 403) {
                throw GitApiException(t.code, getMessageByLocale(CommonMessageCode.ADD_MR_COMMENTS_FAIL))
            }
            throw t
        }
    }

    fun createBranch(host: String, token: String, projectName: String, branch: String, ref: String): GitBranch {
        logger.info("Start to create branches of host $host by project $projectName")
        val body = JsonUtil.getObjectMapper().writeValueAsString(
            mapOf(
                Pair("branch", branch),
                Pair("ref", ref)
            )
        )
        val request = post(host, token, "projects/${urlEncode(projectName)}/repository/branches", body)
        return callMethod(getMessageByLocale(CommonMessageCode.CREATE_BRANCH), request, GitBranch::class.java)
    }

    fun deleteBranch(host: String, token: String, projectName: String, branch: String) {
        logger.info("Start to create branches of host $host by project $projectName")
        val body = JsonUtil.getObjectMapper().writeValueAsString(emptyMap<String, String>())
        val request = delete(host, token, "projects/${urlEncode(projectName)}/repository/branches/$branch", body)
        callMethod(getMessageByLocale(CommonMessageCode.DELETE_BRANCH), request, String::class.java)
    }

    private fun addHook(
        host: String,
        token: String,
        projectName: String,
        hookUrl: String,
        event: String? = null,
        secret: String? = null
    ): GitHook {
        val body = webhookBody(hookUrl, event, secret)
        val request = post(host, token, "projects/${urlEncode(projectName)}/hooks", body)
        try {
            return callMethod(getMessageByLocale(CommonMessageCode.OPERATION_ADD_WEBHOOK), request, GitHook::class.java)
        } catch (t: GitApiException) {
            if (t.code == HTTP_403) {
                throw GitApiException(
                    t.code,
                    getMessageByLocale(CommonMessageCode.WEBHOOK_ADD_FAIL, arrayOf("Developer"))
                )
            }
            throw t
        }
    }

    private fun webhookBody(hookUrl: String, event: String?, secret: String?): String {
        val params = mutableMapOf<String, String>()

        params["url"] = hookUrl
        if (event == null) {
            params[CodeGitWebhookEvent.PUSH_EVENTS.value] = true.toString()
        } else {
            val codeGitEvent = CodeGitWebhookEvent.find(event)
            params[codeGitEvent!!.value] = true.toString()
            if (codeGitEvent != CodeGitWebhookEvent.PUSH_EVENTS) {
                params[CodeGitWebhookEvent.PUSH_EVENTS.value] = false.toString()
            }
        }
        if (!secret.isNullOrBlank()) {
            params["token"] = secret
        }
        params[CodeGitWebhookEvent.ENABLE_SSL_VERIFICATION.value] = false.toString()
        return JsonUtil.getObjectMapper().writeValueAsString(params)
    }

    private fun updateHook(
        host: String,
        hookId: Long,
        token: String,
        projectName: String,
        hookUrl: String,
        event: String? = null,
        secret: String? = null
    ): GitHook {
        logger.info("Start to update webhook of host $host by project $projectName")
        val body = webhookBody(hookUrl, event, secret)
        val request = put(host, token, "projects/${urlEncode(projectName)}/hooks/$hookId", body)
        try {
            return callMethod(
                operation = getMessageByLocale(CommonMessageCode.OPERATION_UPDATE_WEBHOOK),
                request = request,
                classOfT = GitHook::class.java
            )
        } catch (t: GitApiException) {
            if (t.code == HTTP_403) {
                throw GitApiException(t.code, getMessageByLocale(CommonMessageCode.WEBHOOK_UPDATE_FAIL))
            }
            throw t
        }
    }

    private fun getHooks(host: String, token: String, projectName: String): List<GitHook> {
        try {
            val request = get(host, token, "projects/${urlEncode(projectName)}/hooks", "")
            val result = JsonUtil.getObjectMapper().readValue<List<GitHook>>(
                getBody(getMessageByLocale(CommonMessageCode.OPERATION_LIST_WEBHOOK), request)
            )
            return result.sortedBy { it.createdAt }.reversed()
        } catch (t: GitApiException) {
            if (t.code == HTTP_403) {
                throw GitApiException(t.code, getMessageByLocale(CommonMessageCode.WEBHOOK_ADD_FAIL, arrayOf("master")))
            }
            throw t
        }
    }

    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    fun post(host: String, token: String, url: String, body: String) =
        request(host, token, url, "").post(RequestBody.create(mediaType, body)).build()

    private fun delete(host: String, token: String, url: String, body: String) =
        request(host, token, url, "").delete(RequestBody.create(mediaType, body)).build()

    private fun get(host: String, token: String, url: String, page: String) =
        request(host, token, url, page).get().build()

    private fun put(host: String, token: String, url: String, body: String) =
        request(host, token, url, "").put(RequestBody.create(mediaType, body)).build()

    protected open fun request(host: String, token: String, url: String, page: String): Request.Builder {
        return if (page.isNotEmpty()) Request.Builder()
            .url("$host/$url?$page")
            .header("PRIVATE-TOKEN", token)
        else Request.Builder()
            .url("$host/$url")
            .header("PRIVATE-TOKEN", token)
    }

    private fun <T> callMethod(operation: String, request: Request, classOfT: Class<T>): T {
        val sample = Timer.start(SpringContextUtil.getBean(MeterRegistry::class.java))
        var exceptionClass = BkTimedAspect.DEFAULT_EXCEPTION_TAG_VALUE
        try {
            return OkhttpUtils.doRedirectHttp(request) { response ->
                if (!response.isSuccessful) {
                    handleApiException(operation, response.code, response.body?.string() ?: "")
                }
                JsonUtil.getObjectMapper().readValue(response.body!!.string(), classOfT)
            }
        } catch (err: Exception) {
            exceptionClass = err.javaClass.simpleName
            throw err
        } finally {
            val tags = Tags.of(
                "operation", operation
            )
            record("bk_tgit_api_time", tags, "工蜂接口耗时度量", sample, exceptionClass)
        }
    }

    fun record(
        metricName: String,
        tags: Iterable<Tag>,
        description: String? = null,
        sample: Timer.Sample,
        exceptionClass: String,
        applicationName: String? = null
    ) {
        try {
            val registry = SpringContextUtil.getBean(MeterRegistry::class.java)
            sample.stop(
                Timer.builder(metricName)
                    .description(description)
                    .tags(BkTimedAspect.EXCEPTION_TAG, exceptionClass)
                    .tags(tags)
                    .tag(BkTimedAspect.APPLICATION_TAG, applicationName ?: "")
                    .register(registry)
            )
        } catch (err: BeansException) {
            logger.warn("registry get failed")
            throw err
        } catch (ignore: Exception) {
            logger.warn("record failed", ignore)
        }
    }

    private fun getBody(operation: String, request: Request): String {
        val sample = Timer.start(SpringContextUtil.getBean(MeterRegistry::class.java))
        var exceptionClass = BkTimedAspect.DEFAULT_EXCEPTION_TAG_VALUE
        try {
            OkhttpUtils.doHttp(request).use { response ->
                if (!response.isSuccessful) {
                    handleApiException(operation, response.code, response.body?.string() ?: "")
                }
                return response.body!!.string()
            }
        } catch (err: Exception) {
            exceptionClass = err.javaClass.simpleName
            throw err
        } finally {
            val tags = Tags.of(
                "operation", operation
            )
            record("bk_tgit_api_time", tags, "工蜂接口耗时度量", sample, exceptionClass)
        }
    }

    private fun handleApiException(operation: String, code: Int, body: String) {
        logger.warn("Fail to call git api because of code $code and message $body")
        val msg = when (code) {
            HTTP_400 -> getMessageByLocale(CommonMessageCode.PARAM_ERROR)
            HTTP_401 -> getMessageByLocale(CommonMessageCode.AUTH_FAIL, arrayOf("Git token"))
            HTTP_403 -> getMessageByLocale(CommonMessageCode.ACCOUNT_NO_OPERATION_PERMISSIONS, arrayOf(operation))
            HTTP_404 -> getMessageByLocale(
                CommonMessageCode.REPO_NOT_EXIST_OR_NO_OPERATION_PERMISSION,
                arrayOf("GIT", operation)
            )
            HTTP_405 -> getMessageByLocale(CommonMessageCode.GIT_INTERFACE_NOT_EXIST, arrayOf("GIT", operation))
            HTTP_422 -> getMessageByLocale(CommonMessageCode.GIT_CANNOT_OPERATION, arrayOf("GIT", operation))
            else -> "Git platform $operation fail"
        }
        throw GitApiException(code, msg)
    }

    fun listCommits(
        host: String,
        branch: String?,
        token: String,
        projectName: String,
        all: Boolean,
        page: Int,
        size: Int
    ): List<GitCommit> {
        val request = get(
            host, token, "projects/${urlEncode(projectName)}/repository/commits?page=$page&per_page=$size"
                .plus(if (branch.isNullOrBlank()) "" else "&ref_name=$branch").plus(if (all) "&all=true" else ""), ""
        )
        val result: List<GitCommit> = JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_COMMIT), request)
        )
        logger.info(
            "The url to listCommits is($host/projects/${urlEncode(projectName)}/repository/commits)"
        )
        return result
    }

    fun getCommitDiff(host: String, sha: String, token: String, projectName: String): List<GitDiff> {
        val request = get(host, token, "projects/${urlEncode(projectName)}/repository/commits/$sha/diff", "")
        val result: List<GitDiff> = JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_COMMIT_DIFF), request)
        )
        logger.info(
            "The url to listCommits is($host/projects/${urlEncode(projectName)}/repository/commits/$sha/diff)"
        )
        return result
    }

    fun unlockHookLock(host: String, token: String, projectName: String, mrId: Long, retryTimes: Int = 5) {

        val url = "projects/${urlEncode(projectName)}/merge_request/$mrId/unlock_hook_lock"
        logger.info("unlock hook lock for project($projectName): url($url)")
        val request = put(host, token, url, "")
        try {
            val result = callMethod(
                operation = getMessageByLocale(CommonMessageCode.OPERATION_UNLOCK_HOOK_LOCK),
                request = request,
                classOfT = String::class.java
            )
            // 工蜂解锁可能会失败,增加重试
            if (result == "false" && retryTimes > 0) {
                Thread.sleep(500)
                unlockHookLock(host, token, projectName, mrId, retryTimes - 1)
            }
        } catch (t: GitApiException) {
            if (t.code == 403) {
                throw GitApiException(t.code, getMessageByLocale(CommonMessageCode.WEBHOOK_LOCK_UNLOCK_FAIL))
            }
            throw t
        }
    }

    fun getMergeRequestChangeInfo(host: String, token: String, url: String): GitMrChangeInfo {
        logger.info("get mr changes info url: $url")
        val request = get(host, token, url, "")
        return callMethod(
            operation = getMessageByLocale(CommonMessageCode.OPERATION_MR_CHANGE),
            request = request,
            classOfT = GitMrChangeInfo::class.java
        )
    }

    fun getMrInfo(host: String, token: String, url: String): GitMrInfo {
        logger.info("get mr info url: $url")
        val request = get(host, token, url, "")
        return callMethod(getMessageByLocale(CommonMessageCode.OPERATION_MR_INFO), request, GitMrInfo::class.java)
    }

    fun getMrCommitList(host: String, token: String, url: String, page: Int, size: Int): List<GitCommit> {
        logger.info("get mr commit list url: $url")
        val searchReq = "page=$page&per_page=$size"
        val request = get(host, token, url, searchReq)
        val result: List<GitCommit> =
            JsonUtil.getObjectMapper().readValue(
                getBody(getMessageByLocale(CommonMessageCode.OPERATION_GET_MR_COMMIT_LIST), request)
            )
        return result
    }

    fun getMrReviewInfo(host: String, token: String, url: String): GitMrReviewInfo {
        logger.info("get mr review url: $url")
        val request = get(host, token, url, "")
        return callMethod(getMessageByLocale(CommonMessageCode.OPERATION_MR_INFO), request, GitMrReviewInfo::class.java)
    }

    fun getChangeFileList(
        host: String,
        gitProjectId: String,
        token: String,
        from: String,
        to: String,
        straight: Boolean? = false,
        page: Int,
        pageSize: Int
    ): List<ChangeFileInfo> {
        val url = "projects/${urlEncode(gitProjectId)}/repository/compare/changed_files/list"
        val queryParam = "from=$from&to=$to&straight=$straight&page=$page&pageSize=$pageSize"
        val request = get(host, token, url, queryParam)
        return JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_GET_CHANGE_FILE_LIST), request)
        )
    }

    fun getRepoMemberInfo(
        host: String,
        token: String,
        userId: String,
        gitProjectId: String
    ): GitMember {
        val url = "projects/${urlEncode(gitProjectId)}/members/all/$userId"
        val request = get(host, token, url, "")
        return JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_PROJECT_USER_INFO), request)
        )
    }
//    private val OPERATION_BRANCH = "拉分支"
//    private val OPERATION_TAG = "拉标签"
//    private val OPERATION_ADD_WEBHOOK = "添加WEBHOOK"
//    private val OPERATION_LIST_WEBHOOK = "查询WEBHOOK"
//    private val OPERATION_ADD_COMMIT_CHECK = "添加COMMIT CHECK"
//    private val OPERATION_ADD_MR_COMMENT = "添加MR COMMENT"

    private fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    fun getProjectGroupsList(
        host: String,
        token: String,
        page: Int,
        pageSize: Int,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeGroup> {
        val url = "api/v3/groups"
        val queryParam = "page=$page&per_page=$pageSize"
            .addParams(
                mapOf(
                    "owned" to owned,
                    "min_access_level" to minAccessLevel?.level
                )
            )
        val request = get(host, token, url, queryParam)
        return JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_PROJECT_USER_INFO), request)
        )
    }

    fun getTapdWorkitems(
        host: String,
        token: String,
        id: String,
        type: String,
        iid: Long
    ): List<TapdWorkItem> {
        val url = "projects/$id/tapd_workitems"
        val queryParam = "type=$type&iid=$iid"
        val request = get(host, token, url, queryParam)
        return JsonUtil.getObjectMapper().readValue(
            getBody(getMessageByLocale(CommonMessageCode.OPERATION_TAPD_WORKITEMS), request)
        )
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

    fun getProjectInfo(host: String, token: String, url: String): GitProjectInfo {
        val request = get(host, token, url, StringUtils.EMPTY)
        return JsonUtil.getObjectMapper().readValue(
            getBody(
                getMessageByLocale(CommonMessageCode.GET_PROJECT_INFO),
                request
            )
        )
    }
}
