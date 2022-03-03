package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.ci.v2.IssueRule
import com.tencent.devops.common.ci.v2.MrRule
import com.tencent.devops.common.ci.v2.PushRule
import com.tencent.devops.common.ci.v2.TagRule
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMrEventAction
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.service.code.matcher.GitWebHookMatcher
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

object TriggerBuilder {

    private const val JOIN_SEPARATOR = ","

    fun buildCodeGitWebHookTriggerElement(
        gitEvent: GitEvent,
        triggerOn: TriggerOn?
    ): CodeGitWebHookTriggerElement? {
        return when (gitEvent) {
            is GitPushEvent ->
                buildGitPushEventElement(gitEvent, triggerOn)
            is GitTagPushEvent ->
                buildGitTagEventElement(gitEvent, triggerOn)
            is GitMergeRequestEvent ->
                buildGitMrEventElement(gitEvent, triggerOn)
            is GitIssueEvent ->
                buildGitIssueElement(gitEvent, triggerOn)
            is GitReviewEvent ->
                buildGitReviewElement(gitEvent, triggerOn)
            is GitNoteEvent ->
                buildGitNoteElement(gitEvent, triggerOn)
            else -> null
        }
    }

    private fun buildGitNoteElement(gitEvent: GitNoteEvent, triggerOn: TriggerOn?): CodeGitWebHookTriggerElement? {
        if (triggerOn?.note == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitEvent.projectId.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = null,
            excludeBranchName = null,
            includePaths = null,
            excludePaths = null,
            excludeUsers = null,
            block = false,
            includeNoteComment = triggerOn.note?.comment,
            includeNoteTypes = triggerOn.note?.types?.map {
                when (it) {
                    "commit" -> "Commit"
                    "merge_request" -> "Review"
                    "issue" -> "Issue"
                    else -> it
                }
            }?.toList(),
            eventType = CodeEventType.NOTE
        )
    }

    private fun buildGitReviewElement(gitEvent: GitReviewEvent, triggerOn: TriggerOn?): CodeGitWebHookTriggerElement? {
        if (triggerOn?.review == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitEvent.projectId.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = null,
            excludeBranchName = null,
            includePaths = null,
            excludePaths = null,
            excludeUsers = null,
            block = false,
            includeCrState = triggerOn.review?.states,
            includeCrTypes = triggerOn.review?.types,
            eventType = CodeEventType.REVIEW
        )
    }

    /**
     * 手工触发模拟代码事件触发时,构造triggerOn
     */
    fun buildManualTriggerOn(gitEvent: GitEvent): TriggerOn? {
        return when (gitEvent) {
            is GitPushEvent ->
                TriggerOn(push = PushRule(), mr = null, tag = null)
            is GitTagPushEvent ->
                TriggerOn(push = null, mr = null, tag = TagRule())
            is GitMergeRequestEvent ->
                TriggerOn(push = null, mr = MrRule(), tag = null)
            is GitIssueEvent ->
                TriggerOn(push = null, mr = null, tag = null, issue = IssueRule())
            else -> null
        }
    }

    private fun buildGitIssueElement(gitEvent: GitIssueEvent, triggerOn: TriggerOn?): CodeGitWebHookTriggerElement? {
        if (triggerOn?.issue == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitEvent.objectAttributes.projectId.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = null,
            excludeBranchName = null,
            includePaths = null,
            excludePaths = null,
            excludeUsers = null,
            block = false,
            includeIssueAction = triggerOn.issue?.action,
            eventType = CodeEventType.ISSUES
        )
    }

    fun buildCodeGitRepository(
        streamSetting: GitCIBasicSetting
    ): CodeGitRepository {
        with(streamSetting) {
            val projectName = GitUtils.getProjectName(gitHttpUrl)
            return CodeGitRepository(
                aliasName = projectName,
                url = gitHttpUrl,
                credentialId = "",
                projectName = projectName,
                userName = enableUserId,
                authType = RepoAuthType.OAUTH,
                projectId = projectCode,
                repoHashId = null
            )
        }
    }

    fun buildGitWebHookMatcher(gitEvent: GitEvent): ScmWebhookMatcher {
        return GitWebHookMatcher(gitEvent)
    }

    private fun buildGitPushEventElement(
        gitPushEvent: GitPushEvent,
        triggerOn: TriggerOn?
    ): CodeGitWebHookTriggerElement? {
        if (triggerOn?.push == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitPushEvent.project_id.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = triggerOn.push?.branches?.joinToString(JOIN_SEPARATOR),
            excludeBranchName = triggerOn.push?.branchesIgnore?.joinToString(JOIN_SEPARATOR),
            pathFilterType = PathFilterType.RegexBasedFilter,
            includePaths = triggerOn.push?.paths?.joinToString(JOIN_SEPARATOR),
            excludePaths = triggerOn.push?.pathsIgnore?.joinToString(JOIN_SEPARATOR),
            excludeUsers = triggerOn.push?.usersIgnore,
            includeUsers = triggerOn.push?.users,
            block = false,
            eventType = CodeEventType.PUSH
        )
    }

    private fun buildGitTagEventElement(
        gitTagPushEvent: GitTagPushEvent,
        triggerOn: TriggerOn?
    ): CodeGitWebHookTriggerElement? {
        if (triggerOn?.tag == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitTagPushEvent.project_id.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = null,
            excludeBranchName = null,
            includePaths = null,
            excludePaths = null,
            tagName = triggerOn.tag?.tags?.joinToString(JOIN_SEPARATOR),
            excludeTagName = triggerOn.tag?.tagsIgnore?.joinToString(JOIN_SEPARATOR),
            excludeUsers = triggerOn.tag?.usersIgnore,
            includeUsers = triggerOn.tag?.users,
            fromBranches = triggerOn.tag?.fromBranches?.joinToString(JOIN_SEPARATOR),
            block = false,
            eventType = CodeEventType.TAG_PUSH
        )
    }

    private fun buildGitMrEventElement(
        gitMergeRequestEvent: GitMergeRequestEvent,
        triggerOn: TriggerOn?
    ): CodeGitWebHookTriggerElement? {
        if (triggerOn?.mr == null) {
            return null
        }
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitMergeRequestEvent.object_attributes.target_project_id.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = triggerOn.mr?.targetBranches?.joinToString(JOIN_SEPARATOR),
            excludeBranchName = null,
            pathFilterType = PathFilterType.RegexBasedFilter,
            includePaths = triggerOn.mr?.paths?.joinToString(JOIN_SEPARATOR),
            excludePaths = triggerOn.mr?.pathsIgnore?.joinToString(JOIN_SEPARATOR),
            includeUsers = triggerOn.mr?.users,
            excludeUsers = triggerOn.mr?.usersIgnore,
            excludeSourceBranchName = triggerOn.mr?.sourceBranchesIgnore?.joinToString(JOIN_SEPARATOR),
            block = false,
            eventType = if (gitMergeRequestEvent.object_attributes.action == "merge") {
                CodeEventType.MERGE_REQUEST_ACCEPT
            } else {
                CodeEventType.MERGE_REQUEST
            },
            includeMrAction = if (triggerOn.mr?.action.isNullOrEmpty()) {
                // 缺省时使用默认值
                listOf(
                    TGitMrEventAction.OPEN.value,
                    TGitMrEventAction.REOPEN.value,
                    TGitMrEventAction.PUSH_UPDATE.value
                )
            } else {
                triggerOn.mr!!.action
            }
        )
    }
}
