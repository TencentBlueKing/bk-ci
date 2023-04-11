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

package com.tencent.devops.common.webhook.util

import com.google.common.base.Splitter
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_DESC
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_IID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_PROPOSER
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_TITLE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequest
import com.tencent.devops.common.webhook.pojo.code.p4.P4Event
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object WebhookUtils {

    private val separatorPattern = Pattern.compile("[,;]")
    private const val MAX_VARIABLE_COUNT = 32
    // p4自定义触发器插件版本号
    const val CUSTOM_P4_TRIGGER_VERSION = 2

    private val logger = LoggerFactory.getLogger(WebhookUtils::class.java)

    fun convert(commaSeparatedString: String?): List<String> {
        if (commaSeparatedString == null) {
            return emptyList()
        }
        return Splitter.on(separatorPattern)
            .omitEmptyStrings()
            .trimResults()
            .split(commaSeparatedString)
            .toList()
    }

    fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getTag(ref: String): String {
        return ref.removePrefix("refs/tags/")
    }

    @SuppressWarnings("ALL")
    fun getRelativePath(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }

        val path = urlArray[1]
        val repoSplit = path.split("/")
        if (repoSplit.size < 4) {
            return ""
        }
        val domain = repoSplit[0]
        val first = repoSplit[1]
        val second = repoSplit[2]

        return path.removePrefix("$domain/$first/$second").removePrefix("/")
    }

    fun getFullPath(projectRelativePath: String, relativeSubPath: String): String {
        return (
            "${projectRelativePath.removeSuffix("/")}/" +
                relativeSubPath.removePrefix("/")
            ).removePrefix("/")
    }

    fun genCommitsParam(commits: List<GitCommit>): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        var addCount = 0
        var modifyCount = 0
        var deleteCount = 0
        commits.forEachIndexed { index, gitCommit ->
            val curIndex = index + 1
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX + curIndex] = gitCommit.id
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX + curIndex] = gitCommit.message
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX + curIndex] =
                DateTimeUtil.zoneDateToTimestamp(gitCommit.timestamp)
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX + curIndex] = gitCommit.author
            addCount += gitCommit.added?.size ?: 0
            modifyCount += gitCommit.modified?.size ?: 0
            deleteCount += gitCommit.removed?.size ?: 0

            var count = 0
            run {
                gitCommit.added?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.modified?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.removed?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }
        }
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT] = addCount
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT] = modifyCount
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT] = deleteCount
        return startParams
    }

    @SuppressWarnings("ComplexMethod")
    fun mrStartParam(
        mrInfo: GitMrInfo?,
        reviewInfo: GitMrReviewInfo?,
        mrRequestId: Long,
        homepage: String? = null
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID] = mrInfo?.sourceProjectId ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_PROJECT_ID] = mrInfo?.targetProjectId ?: ""
        startParams[PIPELINE_WEBHOOK_MR_ID] = mrRequestId
        startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = mrInfo?.createTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = mrInfo?.updateTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.createTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.updateTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = mrInfo?.mrId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = mrInfo?.mrNumber ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = mrInfo?.description ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = mrInfo?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = mrInfo?.assignee?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = mrInfo?.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = mrInfo?.milestone?.dueDate ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = mrInfo?.labels?.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT] = mrInfo?.baseCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT] = mrInfo?.targetCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT] = mrInfo?.sourceCommit ?: ""

        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
            reviewInfo?.reviewers?.joinToString(",") { it.username } ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_STATE] = reviewInfo?.state ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_OWNER] = reviewInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_ID] = reviewInfo?.mrId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_IID] = reviewInfo?.mrNumber ?: ""

        // 兼容stream变量
        startParams[PIPELINE_GIT_HEAD_REF] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_GIT_BASE_REF] = mrInfo?.targetBranch ?: ""
        startParams[PIPELINE_GIT_MR_ID] = mrInfo?.mrId ?: ""
        startParams[PIPELINE_GIT_MR_IID] = mrInfo?.mrNumber ?: ""
        startParams[PIPELINE_GIT_MR_TITLE] = mrInfo?.title ?: ""
        startParams[PIPELINE_GIT_MR_DESC] = mrInfo?.description ?: ""
        startParams[PIPELINE_GIT_MR_PROPOSER] = mrInfo?.author?.username ?: ""
        if (!homepage.isNullOrBlank()) {
            startParams[PIPELINE_GIT_MR_URL] = "$homepage/merge_requests/${mrInfo?.mrNumber}"
        }
        return startParams
    }

    @SuppressWarnings("ComplexMethod")
    fun prStartParam(
        pullRequest: GithubPullRequest,
        homepage: String? = null
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = pullRequest.head.ref ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = pullRequest.base.ref ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = pullRequest.base.ref ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = pullRequest.head.ref ?: ""
        startParams[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID] = pullRequest.head.repo.id ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_PROJECT_ID] = pullRequest.base.repo.id ?: ""
        startParams[PIPELINE_WEBHOOK_MR_ID] = pullRequest.id
        startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = pullRequest.user.login ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = pullRequest.user.login ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = pullRequest.createdAt ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = pullRequest.updatedAt ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(pullRequest.createdAt)
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(pullRequest.updatedAt)
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = pullRequest.id ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = pullRequest.number ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = pullRequest.body ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = pullRequest.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = pullRequest.assignee?.login ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
            pullRequest.requestedReviewers.joinToString(",") { it.login } ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = pullRequest.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = pullRequest.milestone?.dueOn ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = pullRequest.labels.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT] = pullRequest.base.sha ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT] = pullRequest.base.sha ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT] = pullRequest.head.sha ?: ""

        // 兼容stream变量
        startParams[PIPELINE_GIT_HEAD_REF] = pullRequest.head.ref ?: ""
        startParams[PIPELINE_GIT_BASE_REF] = pullRequest.base.ref ?: ""
        startParams[PIPELINE_GIT_MR_ID] = pullRequest.id ?: ""
        startParams[PIPELINE_GIT_MR_IID] = pullRequest.number ?: ""
        startParams[PIPELINE_GIT_MR_TITLE] = pullRequest.title ?: ""
        startParams[PIPELINE_GIT_MR_DESC] = pullRequest.body ?: ""
        startParams[PIPELINE_GIT_MR_PROPOSER] = pullRequest.user.login ?: ""
        if (!homepage.isNullOrBlank()) {
            startParams[PIPELINE_GIT_MR_URL] = "$homepage/merge_requests/${pullRequest.number}"
        }
        return startParams
    }

    /**
     * p4版本过滤器,2.0以后版本由用户自定义触发器,插件不再主动注册触发器
     *
     * 如果p4服务器已经配置过1.0版本的触发器,用户再增加一个2.0的触发器.因为2.0的插件上没有路径过滤，就会导致全部匹配，使2.0配置的流水线都触发
     *
     */
    fun getP4Filter(
        projectId: String,
        pipelineId: String,
        event: P4Event,
        webHookParams: WebHookParams
    ): WebhookFilter =
        object : WebhookFilter {
            override fun doFilter(response: WebhookFilterResponse): Boolean {
                logger.info(
                    "$pipelineId|triggerOn:${webHookParams.version}|" +
                        "projectId:$projectId|eventProjectId:${event.projectId}|version filter"
                )
                return if (isCustomP4TriggerVersion(webHookParams.version)) {
                    // 2.0以后,由用户配置的触发器触发
                    // 2.0以后，触发的项目必须跟匹配的项目相同才能触发
                    event.isCustomTrigger() && projectId == event.projectId
                } else {
                    // 1.0 由插件注册的触发器触发
                    !event.isCustomTrigger()
                }
            }
        }

    fun getMajorVersion(version: String?): Int {
        return version?.split(".")?.get(0)?.toInt() ?: 1
    }

    fun isCustomP4TriggerVersion(version: String?): Boolean {
        return getMajorVersion(version) >= CUSTOM_P4_TRIGGER_VERSION
    }
}
