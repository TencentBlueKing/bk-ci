package com.tencent.devops.process.pojo.scm.code

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.repository.pojo.Repository

interface ScmWebhookMatcher {

    fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): Boolean

    fun getUsername(): String

    fun getRevision(): String

    fun getRepoName(): String

    fun getBranchName(): String?

    fun getEventType(): CodeEventType

    fun getCodeType(): CodeType

    fun getHookSourceUrl(): String? = null

    fun getHookTargetUrl(): String? = null

    fun getEnv() = emptyMap<String, Any>()

    /**
     * Check if the branch match
     * example:
     * branchName: origin/master
     * ref: refs/heads/origin/master
     */
    fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return branchName == eventBranch
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        return fullPath.removePrefix("/").startsWith(prefixPath.removePrefix("/"))
    }

    fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getTag(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getMergeRequestId(): Long? = null

    data class WebHookParams(
        val repositoryConfig: RepositoryConfig,
        var branchName: String? = null,
        var excludeBranchName: String? = null,
        var includePaths: String? = null,
        var excludePaths: String? = null,
        var eventType: CodeEventType? = null,
        var block: Boolean = false,
        var relativePath: String? = null,
        var excludeUsers: String? = "",
        var includeUsers: String? = null,
        var codeType: CodeType = CodeType.GIT
    )
}