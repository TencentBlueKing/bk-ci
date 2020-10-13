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

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.code.git.GitTagPushEvent
import com.tencent.devops.process.service.scm.GitScmService
import com.tencent.devops.process.utils.GIT_MR_NUMBER
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher
import java.util.regex.Pattern

open class GitWebHookMatcher(val event: GitEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GitWebHookMatcher::class.java)
        private val regex = Pattern.compile("[,;]")
        private val matcher = AntPathMatcher()
        const val MATCH_BRANCH = "matchBranch"
        const val MATCH_PATHS = "matchPaths"
        const val EXCLUDE_MSG = "[skip ci]"
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): ScmWebhookMatcher.MatchResult {
        with(webHookParams) {
            logger.info("do git match for pipeline($pipelineId): ${repository.aliasName}, $branchName, $eventType")

            if (repository !is CodeGitRepository && repository !is CodeTGitRepository) {
                logger.warn("Is not code repo for git web hook for repo and pipeline: $repository, $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }
            if (!matchUrl(repository.url)) {
                logger.warn("Is not match for event and pipeline: $event, $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            // 检测事件类型是否符合
            if (!doEventTypeMatch(webHookParams.eventType)) {
                logger.warn("Is not match event type for pipeline: ${webHookParams.eventType}, $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            // 检查用户是否符合
            if (!doUserMatch(webHookParams.excludeUsers)) {
                logger.warn("Is not match user for pipeline: ${webHookParams.excludeUsers}, $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            // 真正对事件进行检查
            return when (eventType) {
                CodeEventType.PUSH -> {
                    doPushMatch(webHookParams, pipelineId)
                }

                CodeEventType.MERGE_REQUEST, CodeEventType.MERGE_REQUEST_ACCEPT -> {
                    doMrMatch(webHookParams, projectId, pipelineId, repository)
                }

                CodeEventType.TAG_PUSH -> {
                    doTagMatch(webHookParams, pipelineId)
                }

                null -> {
                    ScmWebhookMatcher.MatchResult(true)
                }

                else -> {
                    ScmWebhookMatcher.MatchResult(false)
                }
            }
        }
    }

    private fun doUserMatch(excludeUsers: String?): Boolean {
        val eventBranch = getBranch()
        val eventUsername = getUser()
        if (excludeUsers != null) {
            val excludeUserSet = regex.split(excludeUsers)
            excludeUserSet.forEach {
                if (it == eventUsername) {
                    logger.info("The exclude user($excludeUsers) exclude the git update one($eventBranch)")
                    return false
                }
            }
        }
        return true
    }

    private fun doEventTypeMatch(eventType: CodeEventType?): Boolean {
        if (eventType != null) {
            // mr事件还有多种，还要匹配action
            if (eventType == CodeEventType.MERGE_REQUEST || eventType == CodeEventType.MERGE_REQUEST_ACCEPT) {
                if (event !is GitMergeRequestEvent) {
                    logger.warn("Git mr web hook not match with event type(${event::class.java})")
                    return false
                }
                val action = event.object_attributes.action
                if (eventType == CodeEventType.MERGE_REQUEST && action == "merge") {
                    logger.warn("Git mr web hook not match with action($action)")
                    return false
                }
                if (eventType == CodeEventType.MERGE_REQUEST_ACCEPT && action != "merge") {
                    logger.warn("Git mr web hook accept not match with action($action)")
                    return false
                }
            } else {
                if (eventType != getEventType()) {
                    logger.warn("Git web hook event($event) type(${getEventType()}) not match $eventType")
                    return false
                }
            }
        }
        return true
    }

    private fun doMrMatch(
        webHookParams: ScmWebhookMatcher.WebHookParams,
        projectId: String,
        pipelineId: String,
        repository: Repository
    ): ScmWebhookMatcher.MatchResult {
        val eventBranch = getBranch()
        val eventSourceBranch = (event as GitMergeRequestEvent).object_attributes.source_branch
        with(webHookParams) {
            // get mr change file list
            val gitScmService = SpringContextUtil.getBean(GitScmService::class.java)
            val mrChangeInfo = gitScmService.getMergeRequestChangeInfo(projectId, getMergeRequestId()!!, repository)
            val changeFiles = mrChangeInfo?.files?.map {
                if (it.deletedFile) {
                    it.oldPath
                } else {
                    it.newPath
                }
            }

            if (doExcludeBranchMatch(excludeBranchName, eventBranch, pipelineId)) {
                logger.warn("Do mr match fail for exclude branch match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (doExcludePathMatch(changeFiles, excludePaths, pipelineId)) {
                logger.warn("Do mr event match fail for exclude path match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (doExcludeSourceBranchMatch(excludeSourceBranchName, eventSourceBranch, pipelineId)) {
                logger.warn("Do mr event match fail for exclude source branch match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchBranch = doIncludeBranchMatch(branchName, eventBranch, pipelineId)
            if (matchBranch == null) {
                logger.warn("Do mr match fail for include branch not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchPaths = doIncludePathMatch(changeFiles, includePaths, pipelineId)
            if (matchPaths == null) {
                logger.warn("Do mr event match fail for include path not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchSourceBranch = doIncludeSourceBranchMatch(includeSourceBranchName, eventSourceBranch, pipelineId)
            if (matchSourceBranch == null) {
                logger.warn("Do mr match fail for include source branch not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            logger.info("Do mr match success for pipeline: $pipelineId")
            return ScmWebhookMatcher.MatchResult(true, mapOf(MATCH_BRANCH to matchBranch, MATCH_PATHS to matchPaths))
        }
    }

    private fun doPushMatch(webHookParams: ScmWebhookMatcher.WebHookParams, pipelineId: String): ScmWebhookMatcher.MatchResult {
        val eventBranch = getBranch()
        with(webHookParams) {
            val commits = (event as GitPushEvent).commits
            val eventPaths = mutableSetOf<String>()
            val commitMsg = mutableListOf<String>()
            commits.forEach { commit ->
                eventPaths.addAll(commit.added ?: listOf())
                eventPaths.addAll(commit.removed ?: listOf())
                eventPaths.addAll(commit.modified ?: listOf())
                commitMsg.add(commit.message)
            }

            if (doExcludeBranchMatch(excludeBranchName, eventBranch, pipelineId)) {
                logger.warn("Do push event match fail for exclude branch match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (doExcludePathMatch(eventPaths, excludePaths, pipelineId)) {
                logger.warn("Do push event match fail for exclude path match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (doExcludeMsgMatch(commitMsg[0], pipelineId)) {
                logger.warn("Do push event match fail for exclude message match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchBranch = doIncludeBranchMatch(branchName, eventBranch, pipelineId)
            if (matchBranch == null) {
                logger.warn("Do push event match fail for include branch not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchPaths = doIncludePathMatch(eventPaths, includePaths, pipelineId)
            if (matchPaths == null) {
                logger.warn("Do push event match fail for include path not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            logger.info("Do push match success for pipeline: $pipelineId")
            return ScmWebhookMatcher.MatchResult(true, mapOf(MATCH_BRANCH to matchBranch, MATCH_PATHS to matchPaths))
        }
    }

    private fun doTagMatch(webHookParams: ScmWebhookMatcher.WebHookParams, pipelineId: String): ScmWebhookMatcher.MatchResult {
        // 只触发tag创建事件
        val gitTagPushEvent = event as GitTagPushEvent
        val isCreateTag = gitTagPushEvent.operation_kind == "create"
        if (!isCreateTag) {
            logger.info("Do tag match event fail for pipeline: $pipelineId, ${gitTagPushEvent.operation_kind}")
            return ScmWebhookMatcher.MatchResult(false)
        }

        // 匹配
        val eventTag = getTag(gitTagPushEvent.ref)
        with(webHookParams) {
            if (doExcludeBranchMatch(excludeTagName, eventTag, pipelineId)) {
                logger.warn("Do tag event match fail for exclude branch match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val matchBranch = doIncludeBranchMatch(tagName, eventTag, pipelineId)
            if (matchBranch == null) {
                logger.warn("Do tag event match fail for include branch not match for pipeline: $pipelineId")
                return ScmWebhookMatcher.MatchResult(false)
            }

            logger.info("Do tag match success for pipeline: $pipelineId")
            return ScmWebhookMatcher.MatchResult(true, mapOf(MATCH_BRANCH to matchBranch, MATCH_PATHS to ""))
        }
    }

    // null 表示没匹配上
    private fun doIncludePathMatch(eventPaths: Collection<String>?, includePaths: String?, pipelineId: String): String? {
        logger.info("Do include path match for pipeline: $pipelineId, $eventPaths")
        // include的话，为空则为包含，开区间
        if (includePaths.isNullOrBlank()) return ""

        val includePathSet = regex.split(includePaths).filter { it.isNotEmpty() }
        logger.info("Include path set(${includePathSet.map { it }} for pipeline: $pipelineId")

        val matchPaths = doPathMatch(eventPaths, includePathSet, pipelineId)
        return if (matchPaths.isNotEmpty()) {
            logger.warn("Do include path match success for pipeline: $pipelineId")
            matchPaths.joinToString(",")
        } else {
            null
        }
    }

    // null 表示没匹配上
    private fun doIncludeBranchMatch(branchName: String?, eventBranch: String, pipelineId: String): String? {
        logger.info("Do include branch match for pipeline: $pipelineId, $eventBranch")
        // include的话，为空则为包含，开区间
        if (branchName.isNullOrBlank()) return ""

        val includeBranchNameSet = regex.split(branchName)
        logger.info("Include branch set for pipeline: $pipelineId, ${includeBranchNameSet.map { it }}")
        includeBranchNameSet.forEach {
            if (isBranchMatch(it, eventBranch)) {
                logger.warn("The include branch match the git event branch for pipeline: $pipelineId, $eventBranch")
                return it
            }
        }

        return null
    }

    private fun doExcludeBranchMatch(excludeBranchName: String?, eventBranch: String, pipelineId: String): Boolean {
        logger.info("Do exclude branch match for pipeline: $pipelineId, $eventBranch")
        // 排除的话，为空则为不包含，闭区间
        if (excludeBranchName.isNullOrBlank()) return false

        val excludeBranchNameSet = regex.split(excludeBranchName).toSet()
        logger.info("Exclude branch set for pipeline: $pipelineId, ${excludeBranchNameSet.map { it }}")
        excludeBranchNameSet.forEach {
            if (isBranchMatch(it, eventBranch)) {
                logger.warn("The exclude branch match the git event branch for pipeline: $pipelineId, $eventBranch")
                return true
            }
        }
        return false
    }

    private fun doExcludePathMatch(eventPaths: Collection<String>?, excludePaths: String?, pipelineId: String): Boolean {
        logger.info("Do exclude path match for pipeline: $pipelineId, $eventPaths")
        // 排除的话，为空则为不包含，闭区间
        if (excludePaths.isNullOrBlank()) return false

        val excludePathSet = regex.split(excludePaths).filter { it.isNotEmpty() }
        logger.info("Exclude path set(${excludePathSet.map { it }}) for pipeline: $pipelineId")
        if (doPathMatch(eventPaths, excludePathSet, pipelineId).isNotEmpty()) {
            logger.warn("Do exclude path match success for pipeline: $pipelineId")
            return true
        }
        return false
    }

    // eventPaths或userPaths为空则直接都是返回false
    private fun doPathMatch(eventPaths: Collection<String>?, userPaths: List<String>, pipelineId: String): Set<String> {
        val matchPaths = mutableSetOf<String>()
        eventPaths?.forEach { eventPath ->
            userPaths.forEach { userPath ->
                if (isPathMatch(eventPath, userPath)) {
                    logger.info("Event path match the user path for pipeline: $pipelineId, $eventPath, $userPath")
                    matchPaths.add(userPath)
                }
            }
        }
        return matchPaths
    }

    override fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return matcher.match(branchName, eventBranch)
    }

    open fun matchUrl(url: String): Boolean {
        return when (event) {
            is GitPushEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                    event.repository.git_http_url.removePrefix("http://").removePrefix("https://")
                url == event.repository.git_ssh_url || repoHttpUrl == eventHttpUrl
            }
            is GitTagPushEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                    event.repository.git_http_url.removePrefix("http://").removePrefix("https://")
                url == event.repository.git_ssh_url || repoHttpUrl == eventHttpUrl
            }
            is GitMergeRequestEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                    event.object_attributes.target.http_url.removePrefix("http://").removePrefix("https://")
                url == event.object_attributes.target.ssh_url || repoHttpUrl == eventHttpUrl
            }
            else -> {
                false
            }
        }
    }

    private fun getUser(): String {
        return when (event) {
            is GitPushEvent -> event.user_name
            is GitTagPushEvent -> event.user_name
            is GitMergeRequestEvent -> event.user.username
            else -> ""
        }
    }

    private fun getBranch(): String {
        return when (event) {
            is GitPushEvent -> getBranch(event.ref)
            is GitTagPushEvent -> getBranch(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun doExcludeMsgMatch(commitMsg: String, pipelineId: String): Boolean {
        logger.info("Do exclude msg match for pipeline: $pipelineId, $commitMsg")
        if (commitMsg.contains(EXCLUDE_MSG)) {
            logger.warn("Do exclude msg match success for pipeline: $pipelineId")
            return true
        }
        return false
    }

    private fun doExcludeSourceBranchMatch(excludeSourceBranchName: String?, eventSourceBranch: String, pipelineId: String): Boolean {
        logger.info("Do exclude source branch match for pipeline: $pipelineId, $eventSourceBranch")
        if (excludeSourceBranchName.isNullOrBlank()) return false

        val excludeSourceBranchNameSet = regex.split(excludeSourceBranchName).toSet()
        logger.info("Exclude source branch set for pipeline: $pipelineId, ${excludeSourceBranchNameSet.map { it }}")
        excludeSourceBranchNameSet.forEach {
            if (isBranchMatch(it, eventSourceBranch)) {
                logger.warn("The exclude source branch match the git event branch for pipeline: $pipelineId, $eventSourceBranch")
                return true
            }
        }
        return false
    }

    private fun doIncludeSourceBranchMatch(sourceBranchName: String?, eventSourceBranch: String, pipelineId: String): String? {
        logger.info("Do include source branch match for pipeline: $pipelineId, $eventSourceBranch")
        if (sourceBranchName.isNullOrBlank()) return ""

        val includeSourceBranchNameSet = regex.split(sourceBranchName)
        logger.info("Include source branch set for pipeline: $pipelineId, ${includeSourceBranchNameSet.map { it }}")
        includeSourceBranchNameSet.forEach {
            if (isBranchMatch(it, eventSourceBranch)) {
                logger.warn("The include source branch match the git event branch for pipeline: $pipelineId, $eventSourceBranch")
                return it
            }
        }

        return null
    }

    override fun getUsername(): String {
        return when (event) {
            is GitPushEvent -> event.user_name
            is GitTagPushEvent -> event.user_name
            is GitMergeRequestEvent -> event.user.username
            else -> ""
        }
    }

    override fun getRevision(): String {
        return when (event) {
            is GitPushEvent -> event.checkout_sha ?: ""
            is GitTagPushEvent -> event.commits[0].id
            is GitMergeRequestEvent -> event.object_attributes.last_commit.id
            else -> ""
        }
    }

    override fun getEventType(): CodeEventType {
        return when (event) {
            is GitPushEvent -> CodeEventType.PUSH
            is GitTagPushEvent -> CodeEventType.TAG_PUSH
            is GitMergeRequestEvent -> CodeEventType.MERGE_REQUEST
            else -> CodeEventType.PUSH
        }
    }

    override fun getHookSourceUrl(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.source.http_url else null
    }

    override fun getHookTargetUrl(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.target.http_url else null
    }

    override fun getCodeType() = CodeType.GIT

    override fun getEnv(): Map<String, Any> {
        if (event is GitMergeRequestEvent) {
            return mapOf(GIT_MR_NUMBER to event.object_attributes.iid)
        }
        return super.getEnv()
    }

    override fun getRepoName(): String {
        val sshUrl = when (event) {
            is GitPushEvent -> event.repository.git_ssh_url
            is GitTagPushEvent -> event.repository.git_ssh_url
            is GitMergeRequestEvent -> event.object_attributes.target.ssh_url
            else -> ""
        }
        return GitUtils.getProjectName(sshUrl)
    }

    override fun getBranchName(): String {
        return when (event) {
            is GitPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GitTagPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    override fun getMergeRequestId(): Long? {
        return when (event) {
            is GitMergeRequestEvent -> event.object_attributes.id
            else -> null
        }
    }

    override fun getMessage(): String? {
        return when (event) {
            is GitPushEvent -> event.commits[0].message
            is GitTagPushEvent -> event.commits[0].message
            is GitMergeRequestEvent -> event.object_attributes.last_commit.message
            else -> ""
        }
    }
}