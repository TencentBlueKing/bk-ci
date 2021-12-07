package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
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
        triggerOn: TriggerOn
    ): CodeGitWebHookTriggerElement? {
        return when (gitEvent) {
            is GitPushEvent ->
                buildGitPushEventElement(gitEvent, triggerOn)
            is GitTagPushEvent ->
                buildGitTagEventElement(gitEvent, triggerOn)
            is GitMergeRequestEvent ->
                buildGitMrEventElement(gitEvent, triggerOn)
            else -> null
        }
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
        triggerOn: TriggerOn
    ): CodeGitWebHookTriggerElement {
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
            block = false,
            eventType = CodeEventType.PUSH
        )
    }

    private fun buildGitTagEventElement(
        gitTagPushEvent: GitTagPushEvent,
        triggerOn: TriggerOn
    ): CodeGitWebHookTriggerElement {
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
            block = false,
            eventType = CodeEventType.TAG_PUSH
        )
    }

    private fun buildGitMrEventElement(
        gitMergeRequestEvent: GitMergeRequestEvent,
        triggerOn: TriggerOn
    ): CodeGitWebHookTriggerElement {
        return CodeGitWebHookTriggerElement(
            id = "0",
            repositoryHashId = null,
            repositoryName = gitMergeRequestEvent.object_attributes.target_project_id.toString(),
            repositoryType = RepositoryType.NAME,
            branchName = triggerOn.mr?.targetBranches?.joinToString(JOIN_SEPARATOR),
            excludeBranchName = null,
            includePaths = triggerOn.mr?.paths?.joinToString(JOIN_SEPARATOR),
            excludePaths = triggerOn.mr?.pathsIgnore?.joinToString(JOIN_SEPARATOR),
            excludeUsers = triggerOn.mr?.usersIgnore,
            excludeSourceBranchName = triggerOn.mr?.sourceBranchesIgnore?.joinToString(JOIN_SEPARATOR),
            block = false,
            eventType = CodeEventType.MERGE_REQUEST
        )
    }
}
