package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.process.pojo.scm.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.scm.code.git.GitEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.scm.code.git.GitTagPushEvent
import com.tencent.devops.process.utils.GIT_MR_NUMBER
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher
import java.util.regex.Pattern

class GitWebHookMatcher(val event: GitEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GitWebHookMatcher::class.java)
        private val regex = Pattern.compile("[,;]")
        private val matcher = AntPathMatcher()
    }

    var finalIncludePath = ""
    var finalIncludeBranch = ""

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): Boolean {
        with(webHookParams) {
            logger.info("do git match for pipeline($pipelineId): ${repository.aliasName}, $branchName, $eventType")

            if (repository !is CodeGitRepository) {
                logger.warn("Is not code repo for git web hook for repo and pipeline: $repository, $pipelineId")
                return false
            }
            if (!matchUrl(repository.url)) {
                logger.warn("Is not match for event and pipeline: $event, $pipelineId")
                return false
            }

            // 检测事件类型是否符合
            if (!doEventTypeMatch(webHookParams.eventType)) {
                logger.warn("Is not match event type for pipeline: ${webHookParams.eventType}, $pipelineId")
                return false
            }

            // 检查用户是否符合
            if (!doUserMatch(webHookParams.excludeUsers)) {
                logger.warn("Is not match user for pipeline: ${webHookParams.excludeUsers}, $pipelineId")
                return false
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
                    true
                }

                null -> {
                    true
                }

                else -> {
                    false
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
    ): Boolean {
        val eventBranch = getBranch()
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
                return false
            }

            if (doExcludePathMatch(changeFiles, excludePaths, pipelineId)) {
                logger.warn("Do mr event match fail for exclude path match for pipeline: $pipelineId")
                return false
            }

            if (!doIncludeBranchMatch(branchName, eventBranch, pipelineId)) {
                logger.warn("Do mr match fail for include branch not match for pipeline: $pipelineId")
                return false
            }

            if (!doIncludePathMatch(changeFiles, includePaths, pipelineId)) {
                logger.warn("Do mr event match fail for include path not match for pipeline: $pipelineId")
                return false
            }

            logger.info("Do mr match success for pipeline: $pipelineId")
            return true
        }
    }

    private fun doPushMatch(webHookParams: ScmWebhookMatcher.WebHookParams, pipelineId: String): Boolean {
        val eventBranch = getBranch()
        with(webHookParams) {
            val commits = (event as GitPushEvent).commits
            val eventPaths = mutableSetOf<String>()
            commits.forEach { commit ->
                eventPaths.addAll(commit.added ?: listOf())
                eventPaths.addAll(commit.removed ?: listOf())
                eventPaths.addAll(commit.modified ?: listOf())
            }

            if (doExcludeBranchMatch(excludeBranchName, eventBranch, pipelineId)) {
                logger.warn("Do push event match fail for exclude branch match for pipeline: $pipelineId")
                return false
            }

            if (doExcludePathMatch(eventPaths, excludePaths, pipelineId)) {
                logger.warn("Do push event match fail for exclude path match for pipeline: $pipelineId")
                return false
            }

            if (!doIncludeBranchMatch(branchName, eventBranch, pipelineId)) {
                logger.warn("Do push event match fail for include branch not match for pipeline: $pipelineId")
                return false
            }

            if (!doIncludePathMatch(eventPaths, includePaths, pipelineId)) {
                logger.warn("Do push event match fail for include path not match for pipeline: $pipelineId")
                return false
            }

            logger.info("Do push match success for pipeline: $pipelineId")
            return true
        }
    }

    private fun doIncludePathMatch(eventPaths: Collection<String>?, includePaths: String?, pipelineId: String): Boolean {
        logger.info("Do include path match for pipeline: $pipelineId, $eventPaths")
        // include的话，为空则为包含，开区间
        if (includePaths.isNullOrBlank()) return true

        val includePathSet = regex.split(includePaths).filter { it.isNotEmpty() }
        logger.info("Include path set(${includePathSet.map { it }} for pipeline: $pipelineId")

        if (doPathMatch(eventPaths, includePathSet, pipelineId)) {
            logger.warn("Do include path match success for pipeline: $pipelineId")
            return true
        }
        return false
    }

    private fun doIncludeBranchMatch(branchName: String?, eventBranch: String, pipelineId: String): Boolean {
        logger.info("Do include branch match for pipeline: $pipelineId, $eventBranch")
        // include的话，为空则为包含，开区间
        if (branchName.isNullOrBlank()) return true

        val includeBranchNameSet = regex.split(branchName)
        logger.info("Include branch set for pipeline: $pipelineId, ${includeBranchNameSet.map { it }}")
        includeBranchNameSet.forEach {
            if (isBranchMatch(it, eventBranch)) {
                finalIncludeBranch = it
                logger.warn("The include branch match the git event branch for pipeline: $pipelineId, $eventBranch")
                return true
            }
        }

        return false
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
        if (doPathMatch(eventPaths, excludePathSet, pipelineId)) {
            logger.warn("Do exclude path match success for pipeline: $pipelineId")
            return true
        }
        return false
    }

    // eventPaths或userPaths为空则直接都是返回false
    private fun doPathMatch(eventPaths: Collection<String>?, userPaths: List<String>, pipelineId: String): Boolean {
        eventPaths?.forEach { eventPath ->
            userPaths.forEach { userPath ->
                if (isPathMatch(eventPath, userPath)) {
                    logger.info("Event path match the user path for pipeline: $pipelineId, $eventPath, $userPath")
                    return true
                }
            }
        }
        return false
    }

    override fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return matcher.match(branchName, eventBranch)
    }

    private fun matchUrl(url: String): Boolean {
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
            is GitTagPushEvent -> event.checkout_sha ?: ""
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
        return sshUrl.removePrefix("git@git.code.oa.com:").removeSuffix(".git")
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
}