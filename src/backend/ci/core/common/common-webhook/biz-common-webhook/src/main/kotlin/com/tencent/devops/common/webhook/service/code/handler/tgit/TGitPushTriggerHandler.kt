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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.DELETE_EVENT
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteBranch
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.PushKindFilter
import com.tencent.devops.common.webhook.service.code.filter.SkipCiFilter
import com.tencent.devops.common.webhook.service.code.filter.ThirdFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.common.webhook.util.WebhookUtils.convert
import com.tencent.devops.process.engine.service.code.filter.CommitMessageFilter
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.WebhookCommit
import com.tencent.devops.scm.utils.code.git.GitUtils
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitPushTriggerHandler(
    private val eventCacheService: EventCacheService,
    private val gitScmService: GitScmService,
    // stream没有这个配置
    @Autowired(required = false)
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry? = null
) : GitHookTriggerHandler<GitPushEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitPushTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GitPushEvent> {
        return GitPushEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.PUSH
    }

    override fun getUrl(event: GitPushEvent): String {
        return event.repository.git_http_url
    }

    override fun getUsername(event: GitPushEvent): String {
        return event.user_name
    }

    override fun getRevision(event: GitPushEvent): String {
        return event.checkout_sha ?: ""
    }

    override fun getRepoName(event: GitPushEvent): String {
        return GitUtils.getProjectName(event.repository.git_ssh_url)
    }

    override fun getBranchName(event: GitPushEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getMessage(event: GitPushEvent): String {
        return if (event.commits.isNullOrEmpty()) {
            ""
        } else {
            event.commits!![0].message
        }
    }

    override fun preMatch(event: GitPushEvent): ScmWebhookMatcher.MatchResult {
        val isMatch = when {
            event.total_commits_count <= 0 -> {
                logger.info("Git web hook no commit(${event.total_commits_count})")
                false
            }
            GitUtils.isPrePushBranch(event.ref) -> {
                logger.info("Git web hook is pre-push event|branchName=${event.ref}")
                false
            }
            else ->
                true
        }
        return ScmWebhookMatcher.MatchResult(isMatch)
    }

    override fun getEventFilters(
        event: GitPushEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val skipCiFilter = SkipCiFilter(
                pipelineId = pipelineId,
                triggerOnMessage = event.commits?.get(0)?.message ?: ""
            )
            val commits = event.commits
            val commitMessageFilter = CommitMessageFilter(
                includeCommitMsg,
                excludeCommitMsg,
                commits?.first()?.message ?: "",
                pipelineId
            )
            var pushChangeFiles: Set<String>? = null
            val pathFilter = object : WebhookFilter {
                override fun doFilter(response: WebhookFilterResponse): Boolean {
                    if (excludePaths.isNullOrBlank() && includePaths.isNullOrBlank()) {
                        return true
                    }
                    val eventPaths = if (event.operation_kind == TGitPushOperationKind.UPDATE_NONFASTFORWORD.value) {
                        eventCacheService.getChangeFileList(
                            projectId = projectId,
                            repo = repository,
                            from = event.after,
                            to = event.before
                        )
                    } else {
                        getPushChangeFiles(event)
                    }
                    pushChangeFiles = eventPaths
                    return PathFilterFactory.newPathFilter(
                        PathFilterConfig(
                            pathFilterType = pathFilterType,
                            pipelineId = pipelineId,
                            triggerOnPath = eventPaths.toList(),
                            includedPaths = convert(includePaths),
                            excludedPaths = convert(excludePaths)
                        )
                    ).doFilter(response)
                }
            }
            val pushKindFilter = PushKindFilter(
                pipelineId = pipelineId,
                checkCreateAndUpdate = event.create_and_update,
                actionList = convert(webHookParams.includePushAction)
            )
            val thirdFilter = ThirdFilter(
                projectId = projectId,
                pipelineId = pipelineId,
                event = event,
                changeFiles = pushChangeFiles,
                enableThirdFilter = enableThirdFilter,
                thirdUrl = thirdUrl,
                thirdSecretToken = thirdSecretToken,
                gitScmService = gitScmService,
                callbackCircuitBreakerRegistry = callbackCircuitBreakerRegistry
            )
            return listOf(skipCiFilter, pathFilter, commitMessageFilter, pushKindFilter, thirdFilter)
        }
    }

    override fun retrieveParams(
        event: GitPushEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = event.user_name
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = event.before
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = event.after
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = event.total_commits_count
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND] = event.action_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = event.operation_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = getBranchName(event)
        startParams.putAll(WebhookUtils.genCommitsParam(commits = event.commits ?: emptyList()))

        // 兼容stream变量
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.git_http_url
        startParams[PIPELINE_GIT_REF] = event.ref
        startParams[CI_BRANCH] = getBranchName(event)
        startParams[PIPELINE_GIT_EVENT] = if (event.isDeleteBranch()) {
            DELETE_EVENT
        } else {
            GitPushEvent.classType
        }
        startParams[PIPELINE_GIT_COMMIT_AUTHOR] =
            event.commits?.firstOrNull { it.id == event.after }?.author?.name ?: ""
        startParams[PIPELINE_GIT_BEFORE_SHA] = event.before
        startParams[PIPELINE_GIT_BEFORE_SHA_SHORT] = GitUtils.getShortSha(event.before)
        startParams[PIPELINE_GIT_ACTION] = when (event.create_and_update) {
            null -> TGitPushActionType.PUSH_FILE.value
            false -> TGitPushActionType.NEW_BRANCH.value
            true -> TGitPushActionType.NEW_BRANCH_AND_PUSH_FILE.value
        }
        startParams[PIPELINE_GIT_EVENT_URL] = "${event.repository.homepage}/commit/${event.commits?.firstOrNull()?.id}"
        return startParams
    }

    override fun getWebhookCommitList(
        event: GitPushEvent,
        projectId: String?,
        repository: Repository?,
        page: Int,
        size: Int
    ): List<WebhookCommit> {
        if (page > 1) {
            // push 请求事件会在第一次请求时将所有的commit记录全部返回，所以如果分页参数不为1，则直接返回空列表
            return emptyList()
        }
        return event.commits!!.map {
            val commitTime =
                DateTimeUtil.convertDateToLocalDateTime(Date(DateTimeUtil.zoneDateToTimestamp(it.timestamp)))
            WebhookCommit(
                commitId = it.id,
                authorName = it.author.name,
                message = it.message,
                repoType = ScmType.CODE_TGIT.name,
                commitTime = commitTime,
                eventType = CodeEventType.PUSH.name,
                mrId = null,
                action = event.action_kind
            )
        }
    }

    private fun getPushChangeFiles(
        event: GitPushEvent
    ): Set<String> {
        val changeFileList = mutableSetOf<String>()
        event.diffFiles?.forEach {
            when {
                // 删除文件
                it.deletedFile -> changeFileList.add(it.oldPath)
                // 重命名文件
                it.renamedFile -> {
                    changeFileList.add(it.newPath)
                    changeFileList.add(it.oldPath)
                }
                // 修改或添加文件
                else -> changeFileList.add(it.newPath)
            }
        }
        return changeFileList
    }
}
