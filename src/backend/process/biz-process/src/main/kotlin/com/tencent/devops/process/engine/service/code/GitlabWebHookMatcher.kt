package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.process.pojo.code.GitlabCommitEvent
import com.tencent.devops.process.pojo.scm.code.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory

class GitlabWebHookMatcher(private val event: GitlabCommitEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GitlabWebHookMatcher::class.java)
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): Boolean {
        with(webHookParams) {
            if (repository !is CodeGitlabRepository) {
                logger.warn("The repo($repository) is not code git repo for git web hook")
                return false
            }
            if (repository.url != event.repository.git_http_url) {
                return false
            }

            if (branchName.isNullOrEmpty()) {
                return true
            }

            val match = isBranchMatch(branchName!!, event.ref)
            if (!match) {
                logger.info("The branch($branchName) is not match the git update one(${event.ref})")
            }
            return match
        }
    }

    override fun getUsername() = event.user_name

    override fun getRevision() = event.checkout_sha

    override fun getRepoName() = event.project.path_with_namespace

    override fun getBranchName() = org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)

    override fun getEventType() = CodeEventType.PUSH

    override fun getCodeType() = CodeType.GITLAB

    override fun getMergeRequestId() = null
}