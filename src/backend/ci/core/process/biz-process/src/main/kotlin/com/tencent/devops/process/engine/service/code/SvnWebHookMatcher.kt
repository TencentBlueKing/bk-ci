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
    ): ScmWebhookMatcher.MatchResult {
        with(webHookParams) {
            logger.info("Code svn $repository")
            if (repository !is CodeSvnRepository) {
                logger.warn("The repo($repository) is not code svn repo for svn web hook")
                return ScmWebhookMatcher.MatchResult(false)
            }

            // check project match
            // 如果项目名是三层的，比如ied/ied_kihan_rep/server_proj，那对应的rep_name 是 ied_kihan_rep
            val isMatchProject = repository.projectName == event.rep_name ||
                pipelineWebhookService.getProjectName(repository.projectName) == event.rep_name
            if (!isMatchProject) ScmWebhookMatcher.MatchResult(false)

            logger.info("project macth: ${event.rep_name}")

            // exclude users of commits
            if (!excludeUsers.isNullOrBlank()) {
                val excludeUserSet = regex.split(excludeUsers)
                excludeUserSet.forEach {
                    if (it == getUsername()) {
                        logger.info("The exclude user($excludeUsers) exclude the svn update on pipeline($pipelineId)")
                        return ScmWebhookMatcher.MatchResult(false)
                    }
                }
                logger.info("exclude user do not match: ${excludeUserSet.joinToString(",")}")
            }

            // include users of commits
            if (!includeUsers.isNullOrBlank()) {
                val includeUserSet = regex.split(includeUsers)
                if (!includeUserSet.any { it == getUsername() }) {
                    logger.info("include user do not match: ${includeUserSet.joinToString(",")}")
                    return ScmWebhookMatcher.MatchResult(false)
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
                            return ScmWebhookMatcher.MatchResult(false)
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
                            return ScmWebhookMatcher.MatchResult(true)
                        } else {
                            logger.info("Svn path $path not match $finalRelativePath")
                        }
                    }
                }
                return ScmWebhookMatcher.MatchResult(false)
            }

            return ScmWebhookMatcher.MatchResult(true)
        }
    }

    override fun getUsername() = event.userName

    override fun getRevision() = event.revision.toString()

    override fun getRepoName() = event.rep_name

    override fun getBranchName(): String? = null

    override fun getEventType() = CodeEventType.POST_COMMIT

    override fun getCodeType() = CodeType.SVN
}