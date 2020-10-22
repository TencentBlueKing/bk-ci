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

package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.ScmWebhookStartParams
import com.tencent.devops.process.pojo.code.git.GitCommit
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.code.git.GitTagPushEvent
import com.tencent.devops.process.service.scm.GitScmService
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_OPERATION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_USERNAME
import org.slf4j.LoggerFactory

open class GitWebHookStartParam(
    private val projectId: String,
    private val repo: Repository,
    private val params: ScmWebhookMatcher.WebHookParams,
    private val matcher: GitWebHookMatcher,
    private val matchResult: ScmWebhookMatcher.MatchResult
) : ScmWebhookStartParams<CodeGitWebHookTriggerElement> {

    override fun getStartParams(element: CodeGitWebHookTriggerElement): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = element.branchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = element.excludeBranchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS] = element.includePaths ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS] = element.excludePaths ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = element.excludeUsers?.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] =
            matchResult.extra[GitWebHookMatcher.MATCH_BRANCH] ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH] = matchResult.extra[GitWebHookMatcher.MATCH_PATHS] ?: ""
        getEventTypeStartParams(startParams)

        return startParams
    }

    open fun getEventTypeStartParams(startParams: MutableMap<String, Any>) {
        when (params.eventType) {
            CodeEventType.MERGE_REQUEST, CodeEventType.MERGE_REQUEST_ACCEPT ->
                mergeRequestEventStartParams(startParams)
            CodeEventType.TAG_PUSH ->
                tagPushEventStartParam(startParams)
            CodeEventType.PUSH ->
                pushEventStartParam(startParams)
            else ->
                logger.info("git webhook startparam eventType error, eventType:${params.eventType}, ignore")
        }
    }

    private fun mergeRequestEventStartParams(startParams: MutableMap<String, Any>) {
        mrStartParam(startParams)
        val gitMrEvent = matcher.event as GitMergeRequestEvent
        startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = gitMrEvent.object_attributes.url
        val lastCommit = matcher.event.object_attributes.last_commit
        startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT] = lastCommit.id
        startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG] = lastCommit.message
    }

    private fun tagPushEventStartParam(startParams: MutableMap<String, Any>) {
        val gitTagPushEvent = matcher.event as GitTagPushEvent
        startParams[BK_REPO_GIT_WEBHOOK_TAG_NAME] = matcher.getBranchName()
        startParams[BK_REPO_GIT_WEBHOOK_TAG_OPERATION] = gitTagPushEvent.operation_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = gitTagPushEvent.total_commits_count
        startParams[BK_REPO_GIT_WEBHOOK_TAG_USERNAME] = matcher.getUsername()
        genCommitsParam(startParams, gitTagPushEvent.commits)
    }

    private fun pushEventStartParam(startParams: MutableMap<String, Any>) {
        val gitPushEvent = matcher.event as GitPushEvent
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = matcher.getUsername()
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = gitPushEvent.before
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = gitPushEvent.after
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = gitPushEvent.total_commits_count
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND] = gitPushEvent.action_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = gitPushEvent.operation_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName()
        genCommitsParam(startParams, gitPushEvent.commits)
    }

    private fun genCommitsParam(startParams: MutableMap<String, Any>, commits: List<GitCommit>) {
        commits.forEachIndexed { index, gitCommit ->
            val curIndex = index + 1
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX + curIndex] = gitCommit.id
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX + curIndex] = gitCommit.message
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX + curIndex] =
                DateTimeUtil.zoneDateToTimestamp(gitCommit.timestamp)
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX + curIndex] = gitCommit.author
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT] = gitCommit.added?.size ?: 0
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT] = gitCommit.modified?.size ?: 0
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT] = gitCommit.removed?.size ?: 0

            var count = 0
            run {
                gitCommit.added?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARITABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.modified?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARITABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.removed?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARITABLE_COUNT) return@run
                }
            }
        }
    }

    private fun mrStartParam(startParams: MutableMap<String, Any>) {
        val mrRequestId = matcher.getMergeRequestId()
        // MR提交人
        val gitScmService = SpringContextUtil.getBean(GitScmService::class.java)
        val mrInfo = gitScmService.getMergeRequestInfo(projectId, mrRequestId, repo)
        val reviewers = gitScmService.getMergeRequestReviewersInfo(projectId, mrRequestId, repo)?.reviewers

        startParams[PIPELINE_WEBHOOK_MR_ID] = mrRequestId!!
        startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = mrInfo?.author?.username ?: ""
        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""

        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = matcher.getHookTargetUrl() ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = matcher.getHookSourceUrl() ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = mrInfo?.createTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = mrInfo?.updateTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] =
            DateTimeUtil.zoneDateToTimestamp(mrInfo?.createTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] =
            DateTimeUtil.zoneDateToTimestamp(mrInfo?.updateTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = mrInfo?.mrId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = mrInfo?.mrNumber ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = mrInfo?.description ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = mrInfo?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = mrInfo?.assignee?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] = reviewers?.joinToString(",") { it.username } ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = mrInfo?.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = mrInfo?.milestone?.dueDate ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = mrInfo?.labels?.joinToString(",") ?: ""
    }

    companion object {
        private const val MAX_VARITABLE_COUNT = 32
        private val logger = LoggerFactory.getLogger(GitWebHookStartParam::class.java)
    }
}