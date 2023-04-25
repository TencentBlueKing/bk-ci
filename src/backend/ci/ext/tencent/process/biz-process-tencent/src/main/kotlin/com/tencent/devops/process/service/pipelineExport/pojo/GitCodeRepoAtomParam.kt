package com.tencent.devops.process.service.pipelineExport.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GitCodeRepoAtomParam(
    var repositoryType: RepositoryType? = null,
    var repositoryHashId: String? = null,
    var repositoryName: String? = null,
    var localPath: String? = null,
    var strategy: CodePullStrategy? = null,
    var enableSubmodule: Boolean? = null,
    var submodulePath: String? = null,
    var enableVirtualMergeBranch: Boolean? = null,
    var enableSubmoduleRemote: Boolean = false,
    var enableSubmoduleRecursive: Boolean? = false,
    var newEnableSubmoduleRecursive: Boolean? = null,
    var enableAutoCrlf: Boolean? = null,
    var autoCrlf: String? = null,
    var pullType: GitPullModeType? = null,
    var branchName: String? = null,
    var tagName: String? = null,
    var commitId: String? = null,
    var includePath: String? = null,
    var excludePath: String? = null,
    var fetchDepth: Int? = null,
    var enableGitClean: Boolean? = null,
    var enableGitCleanIgnore: Boolean? = null,
    var enableGitLfs: Boolean? = null,

    // 非前端传递的参数
    @JsonProperty("pipeline.start.type")
    val pipelineStartType: String? = null,
    val hookEventType: String? = null,
    val hookSourceBranch: String? = null,
    val hookTargetBranch: String? = null,
    val hookSourceUrl: String? = null,
    val hookTargetUrl: String? = null,

    @JsonProperty("git_mr_number")
    val gitMrNumber: String? = null,

    @JsonProperty("BK_CI_REPO_WEBHOOK_REPO_URL")
    val hookRepoUrl: String? = null,

    @JsonProperty("BK_CI_HOOK_REVISION")
    val hookRevision: String? = null,

    @JsonProperty("BK_CI_HOOK_BRANCH")
    val hookBranch: String? = null,

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE")
    val gitHookEventType: String? = null,

    @JsonProperty("BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA")
    val mrMergeCommitSha: String? = null,

    @JsonProperty("pipeline.start.channel")
    var channelCode: String? = null,

// 重试时检出的commitId
    var retryStartPoint: String? = null
) {
    @JsonIgnore
    fun getBranch(): String? {
        return when (pullType) {
            GitPullModeType.BRANCH -> branchName
            GitPullModeType.TAG -> tagName
            GitPullModeType.COMMIT_ID -> commitId
            else -> null
        }
    }

    @JsonIgnore
    fun getRepositoryConfig(): RepositoryConfig {
        return RepositoryConfig(
            repositoryHashId = repositoryHashId,
            repositoryName = repositoryName,
            repositoryType = repositoryType ?: RepositoryType.ID
        )
    }
}
