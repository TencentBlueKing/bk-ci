package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMrEventAction
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.service.code.matcher.GitWebHookMatcher
import com.tencent.devops.common.webhook.service.code.matcher.GithubWebHookMatcher
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.IssueRule
import com.tencent.devops.process.yaml.v2.models.on.MrRule
import com.tencent.devops.process.yaml.v2.models.on.PushRule
import com.tencent.devops.process.yaml.v2.models.on.TagRule
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting

object TriggerBuilder {

    private const val JOIN_SEPARATOR = ","

    fun buildCodeGitWebHookTriggerElement(
        gitEvent: CodeWebhookEvent,
        triggerOn: TriggerOn?
    ): WebHookTriggerElement? {
        return when (gitEvent) {
            // T_GIT
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
            // GITHUB
            is GithubPushEvent ->
                buildGithubPushEventElement(gitEvent, triggerOn)
            is GithubPullRequestEvent ->
                buildGithubPrEventElement(gitEvent, triggerOn)
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
            includeNoteComment = triggerOn.note?.comment?.joinToString(JOIN_SEPARATOR),
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
    fun buildManualTriggerOn(objectKind: StreamObjectKind): TriggerOn? {
        return when (objectKind) {
            StreamObjectKind.PUSH ->
                TriggerOn(push = PushRule(), mr = null, tag = null)
            StreamObjectKind.TAG_PUSH ->
                TriggerOn(push = null, mr = null, tag = TagRule())
            StreamObjectKind.MERGE_REQUEST ->
                TriggerOn(push = null, mr = MrRule(), tag = null)
            StreamObjectKind.ISSUE ->
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
        streamSetting: StreamTriggerSetting
    ): CodeGitRepository {
        with(streamSetting) {
            val projectName = GitUtils.getProjectName(gitHttpUrl)
            return CodeGitRepository(
                aliasName = projectName,
                url = gitHttpUrl,
                credentialId = "",
                projectName = projectName,
                userName = enableUser,
                authType = RepoAuthType.OAUTH,
                projectId = projectCode,
                repoHashId = null,
                gitProjectId = 0L
            )
        }
    }

    fun buildCodeGitForRepoRepository(
        action: BaseAction
    ): CodeGitRepository {
        val projectName = action.data.eventCommon.gitProjectName ?: ""
        return CodeGitRepository(
            aliasName = projectName,
            url = action.data.context.repoTrigger?.triggerGitHttpUrl ?: "",
            credentialId = "",
            projectName = projectName,
            userName = action.data.getUserId(),
            authType = RepoAuthType.OAUTH,
            projectId = action.getProjectCode(action.data.eventCommon.gitProjectId),
            repoHashId = null,
            gitProjectId = 0L
        )
    }

    fun buildGitWebHookMatcher(gitEvent: CodeWebhookEvent) = when (gitEvent) {
        is GithubEvent -> GithubWebHookMatcher(gitEvent)
        is GitEvent -> GitWebHookMatcher(gitEvent)
        else -> TODO("对接其他Git平台时需要补充")
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

    private fun buildGithubPushEventElement(
        githubPushEvent: GithubPushEvent,
        triggerOn: TriggerOn?
    ): CodeGithubWebHookTriggerElement? {
        if (triggerOn?.push == null) {
            return null
        }
        return CodeGithubWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = githubPushEvent.repository.id.toString(),
            repositoryType = RepositoryType.NAME,
            eventType = CodeEventType.PUSH,
            branchName = triggerOn.push?.branches?.joinToString(JOIN_SEPARATOR),
            excludeBranchName = triggerOn.push?.branchesIgnore?.joinToString(JOIN_SEPARATOR),
            excludeUsers = triggerOn.push?.usersIgnore?.joinToString(JOIN_SEPARATOR)
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

    private fun buildGithubPrEventElement(
        githubPrEvent: GithubPullRequestEvent,
        triggerOn: TriggerOn?
    ): CodeGithubWebHookTriggerElement? {
        if (triggerOn?.mr == null) {
            return null
        }
        return CodeGithubWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = githubPrEvent.pullRequest.base.repo.id.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = triggerOn.mr?.targetBranches?.joinToString(JOIN_SEPARATOR) ?: "*",
            excludeBranchName = null,
            excludeUsers = triggerOn.mr?.usersIgnore?.joinToString(JOIN_SEPARATOR),
            eventType = CodeEventType.PULL_REQUEST
        )
    }
}
