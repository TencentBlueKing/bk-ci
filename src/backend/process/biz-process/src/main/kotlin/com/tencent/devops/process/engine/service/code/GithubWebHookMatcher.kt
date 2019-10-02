package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.process.pojo.scm.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.scm.code.github.GithubCreateEvent
import com.tencent.devops.process.pojo.scm.code.github.GithubEvent
import com.tencent.devops.process.pojo.scm.code.github.GithubPullRequestEvent
import com.tencent.devops.process.pojo.scm.code.github.GithubPushEvent
import com.tencent.devops.process.utils.GITHUB_PR_NUMBER
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.github.GithubRepository
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class GithubWebHookMatcher(val event: GithubEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubWebHookMatcher::class.java)
        private val regex = Pattern.compile("[,;]")
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): Boolean {
        with(webHookParams) {
            if (repository !is GithubRepository) {
                logger.warn("The repo($repository) is not code git repo for github web hook")
                return false
            }
            if (!matchUrl(repository.url)) {
                logger.info("The repo($repository) is not match event($event)")
                return false
            }

            val eventUsername = getUsername()
            val eventBranch = getBranch()

            // 检测事件类型是否符合
            if (eventType != null && eventType != getEventType()) {
                logger.info("Git web hook event($event) (${getEventType()}) not match $eventType")
                return false
            }

            if (excludeUsers != null) {
                val excludeUserSet = regex.split(excludeUsers)
                excludeUserSet.forEach {
                    if (it == eventUsername) {
                        logger.info("The exclude user($excludeUsers) exclude the git update one($eventBranch)")
                        return false
                    }
                }
            }

            if (eventType == CodeEventType.CREATE) {
                return true
            }

            if (excludeBranchName != null) {
                val excludeBranchNameSet = regex.split(excludeBranchName).toSet()
                excludeBranchNameSet.forEach {
                    if (isBranchMatch(it, eventBranch)) {
                        logger.info("The exclude branch($excludeBranchName) exclude the git update one($eventBranch)")
                        return false
                    }
                }
            }

            if (branchName.isNullOrBlank()) {
                logger.info("Git trigger ignore branch name")
                return true
            } else {
                val includeBranchNameSet = regex.split(branchName)
                includeBranchNameSet.forEach {
                    if (isBranchMatch(it, eventBranch)) {
                        logger.info("The include branch($branchName) include the git update one($eventBranch)")
                        return true
                    }
                }
            }

            logger.info("The include branch($branchName) doesn't include the git update one($eventBranch)")
            return false
        }
    }

    private fun matchUrl(url: String): Boolean {
        return when (event) {
            is GithubPushEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            is GithubCreateEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            is GithubPullRequestEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            else -> {
                false
            }
        }
    }

    private fun getBranch(): String {
        return when (event) {
            is GithubPushEvent -> event.ref
            is GithubCreateEvent -> event.ref
            is GithubPullRequestEvent -> event.pull_request.base.ref
            else -> ""
        }
    }

    override fun getUsername(): String {
        return when (event) {
            is GithubPushEvent -> event.sender.login
            is GithubCreateEvent -> event.sender.login
            is GithubPullRequestEvent -> event.sender.login
            else -> ""
        }
    }

    override fun getRevision(): String {
        return when (event) {
            is GithubPushEvent -> event.head_commit.id
            is GithubPullRequestEvent -> event.pull_request.head.sha
            else -> ""
        }
    }

    override fun getEventType(): CodeEventType {
        return when (event) {
            is GithubPushEvent -> CodeEventType.PUSH
            is GithubCreateEvent -> CodeEventType.CREATE
            is GithubPullRequestEvent -> CodeEventType.PULL_REQUEST
            else -> CodeEventType.PUSH
        }
    }

    override fun getHookSourceUrl(): String? {
        return if (event is GithubPullRequestEvent) event.pull_request.head.repo.clone_url else null
    }

    override fun getHookTargetUrl(): String? {
        return if (event is GithubPullRequestEvent) event.pull_request.base.repo.clone_url else null
    }

    override fun getCodeType() = CodeType.GITHUB

    override fun getRepoName(): String {
        val sshUrl = when (event) {
            is GithubPushEvent -> event.repository.ssh_url
            is GithubCreateEvent -> event.repository.ssh_url
            is GithubPullRequestEvent -> event.repository.ssh_url
            else -> ""
        }
        return sshUrl.removePrefix("git@github.com:").removeSuffix(".git")
    }

    override fun getBranchName(): String {
        return when (event) {
            is GithubPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GithubCreateEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GithubPullRequestEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.pull_request.head.ref)
            else -> ""
        }
    }

    override fun getEnv(): Map<String, Any> {
        if (event is GithubPullRequestEvent) {
            return mapOf(GITHUB_PR_NUMBER to event.number)
        }
        return super.getEnv()
    }

    override fun getMergeRequestId() = null
}