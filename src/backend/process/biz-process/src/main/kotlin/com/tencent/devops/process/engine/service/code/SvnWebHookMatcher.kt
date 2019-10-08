package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class SvnWebHookMatcher(
        val event: SvnCommitEvent,
        private val pipelineWebhookService: PipelineWebhookService
) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(SvnWebHookMatcher::class.java)
        private val regex = Pattern.compile("[,;]")
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): Boolean {
        with(webHookParams) {
            logger.info("Code svn $repository")
            if (repository !is CodeSvnRepository) {
                logger.warn("The repo($repository) is not code svn repo for svn web hook")
                return false
            }

            // check project match
            // 如果项目名是三层的，比如ied/ied_kihan_rep/server_proj，那对应的rep_name 是 ied_kihan_rep
            val isMatchProject = repository.projectName == event.rep_name ||
                pipelineWebhookService.getProjectName(repository.projectName) == event.rep_name
            if (!isMatchProject) return false

            logger.info("project macth: ${event.rep_name}")

            // exclude users of commits
            if (!excludeUsers.isNullOrBlank()) {
                val excludeUserSet = regex.split(excludeUsers)
                excludeUserSet.forEach {
                    if (it == getUsername()) {
                        logger.info("The exclude user($excludeUsers) exclude the svn update on pipeline($pipelineId)")
                        return false
                    }
                }
                logger.info("exclude user do not match: ${excludeUserSet.joinToString(",")}")
            }

            // include users of commits
            if (!includeUsers.isNullOrBlank()) {
                val includeUserSet = regex.split(includeUsers)
                if (!includeUserSet.any { it == getUsername() }) {
                    logger.info("include user do not match: ${includeUserSet.joinToString(",")}")
                    return false
                }
            }

            val projectRelativePath = pipelineWebhookService.getRelativePath(repository.url)

            // exclude path of commits
            if (!excludePaths.isNullOrEmpty()) {
                val excludePathSet = regex.split(excludePaths).filter { it.isNotEmpty() }
                logger.info("Exclude path set($excludePathSet)")
                event.paths.forEach { path ->
                    excludePathSet.forEach { excludePath ->
                        val finalRelativePath =
                            ("${projectRelativePath.removeSuffix("/")}/" +
                                excludePath.removePrefix("/")).removePrefix("/")

                        if (path.startsWith(finalRelativePath)) {
                            logger.info("Svn exclude path $path match $finalRelativePath")
                            return false
                        } else {
                            logger.info("Svn exclude path $path not match $finalRelativePath")
                        }
                    }
                }
            }

            // include path of commits
            if (relativePath != null) {
                val relativePathSet = regex.split(relativePath)
                event.paths.forEach { path ->
                    relativePathSet.forEach { relativeSubPath ->
                        val finalRelativePath =
                            ("${projectRelativePath.removeSuffix("/")}/" +
                                relativeSubPath.removePrefix("/")).removePrefix("/")
                        if (path.startsWith(finalRelativePath)) {
                            logger.info("Svn path $path match $finalRelativePath")
                            return true
                        } else {
                            logger.info("Svn path $path not match $finalRelativePath")
                        }
                    }
                }
                return false
            }

            return true
        }
    }

    override fun getUsername() = event.userName

    override fun getRevision() = event.revision.toString()

    override fun getRepoName() = event.rep_name

    override fun getBranchName(): String? = null

    override fun getEventType() = CodeEventType.POST_COMMIT

    override fun getCodeType() = CodeType.SVN
}