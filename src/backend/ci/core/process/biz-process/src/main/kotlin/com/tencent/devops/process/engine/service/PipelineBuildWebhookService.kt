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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.engine.service.code.GitWebHookMatcher
import com.tencent.devops.process.engine.service.code.GithubWebHookMatcher
import com.tencent.devops.process.engine.service.code.GitlabWebHookMatcher
import com.tencent.devops.process.engine.service.code.SvnWebHookMatcher
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher.WebHookParams
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.code.git.GitTagPushEvent
import com.tencent.devops.process.pojo.code.github.GithubCreateEvent
import com.tencent.devops.process.pojo.code.github.GithubEvent
import com.tencent.devops.process.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.process.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.process.pojo.scm.code.GitlabCommitEvent
import com.tencent.devops.process.service.scm.GitScmService
import com.tencent.devops.process.util.DateTimeUtils
import com.tencent.devops.process.utils.PIPELINE_REPO_NAME
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_PENDING
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_OPERATION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_TAG_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_COMMIT_TIME
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_INCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_RELATIVE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_REVERSION
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_URL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineBuildWebhookService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    private val logger = LoggerFactory.getLogger(PipelineBuildWebhookService::class.java)

    fun externalCodeSvnBuild(e: String): Boolean {
        logger.info("Trigger code svn build - $e")

        val event = try {
            objectMapper.readValue(e, SvnCommitEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the svn web hook commit event", e)
            return false
        }

        val svnWebHookMatcher = SvnWebHookMatcher(event, pipelineWebhookService)

        return startProcessByWebhook(CodeSVNWebHookTriggerElement.classType, svnWebHookMatcher)
    }

    fun externalCodeGitBuild(e: String): Boolean {
        logger.info("Trigger code git build($e)")

        val event = try {
            objectMapper.readValue<GitEvent>(e)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event", e)
            return false
        }

        when (event) {
            is GitPushEvent -> {
                if (event.total_commits_count <= 0) {
                    logger.info("Git web hook no commit(${event.total_commits_count})")
                    return true
                }
            }
            is GitMergeRequestEvent -> {
                if (event.object_attributes.action == "close" ||
                    (event.object_attributes.action == "update" && event.object_attributes.extension_action != "push-update")
                ) {
                    logger.info("Git web hook is ${event.object_attributes.action} merge request")
                    return true
                }
            }
        }

        val gitWebHookMatcher = GitWebHookMatcher(event)

        return startProcessByWebhook(CodeGitWebHookTriggerElement.classType, gitWebHookMatcher)
    }

    fun externalGitlabBuild(e: String): Boolean {
        logger.info("Trigger gitlab build($e)")

        val event = try {
            objectMapper.readValue(e, GitlabCommitEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the gitlab web hook commit event", e)
            return false
        }

        val gitlabWebHookMatcher = GitlabWebHookMatcher(event)

        return startProcessByWebhook(CodeGitlabWebHookTriggerElement.classType, gitlabWebHookMatcher)
    }

    fun externalCodeGithubBuild(eventType: String, guid: String, signature: String, body: String): Boolean {
        logger.info("Trigger code github build (event=$eventType, guid=$guid, signature=$signature, body=$body)")

        val event: GithubEvent = when (eventType) {
            GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(body)
            GithubCreateEvent.classType -> objectMapper.readValue<GithubCreateEvent>(body)
            GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(body)
            else -> {
                logger.info("Github event($eventType) is ignored")
                return true
            }
        }

        when (event) {
            is GithubPushEvent -> {
                if (event.commits.isEmpty()) {
                    logger.info("Github web hook no commit")
                    return true
                }
            }
            is GithubPullRequestEvent -> {
                if (!(event.action == "opened" || event.action == "reopened" || event.action == "synchronize")) {
                    logger.info("Github pull request no open or update")
                    return true
                }
            }
        }

        val githubWebHookMatcher = GithubWebHookMatcher(event)

        return startProcessByWebhook(CodeGithubWebHookTriggerElement.classType, githubWebHookMatcher)
    }

    private fun startProcessByWebhook(codeRepositoryType: String, matcher: ScmWebhookMatcher): Boolean {
        logger.info("Start process by web hook repo(${matcher.getRepoName()}) and code repo type($codeRepositoryType)")
        val pipelines = pipelineWebhookService.getWebhookPipelines(matcher.getRepoName(), codeRepositoryType).toSet()

        logger.info("Get the hook pipelines $pipelines")
        if (pipelines.isEmpty()) {
            return false
        }

        pipelines.forEach outside@{ pipelineId ->
            try {
                logger.info("pipelineId is $pipelineId")
                val model = pipelineRepositoryService.getModel(pipelineId) ?: run {
                    logger.info("pipeline does not exists, ignore")
                    return@outside
                }

                /**
                 * 验证流水线参数构建启动参数
                 */
                val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                var canWebhookStartup = false
                run lit@{
                    triggerContainer.elements.forEach {
                        when (codeRepositoryType) {
                            CodeSVNWebHookTriggerElement.classType -> {
                                if (it is CodeSVNWebHookTriggerElement && it.isElementEnable()) {
                                    canWebhookStartup = true
                                    return@lit
                                }
                            }
                            CodeGitWebHookTriggerElement.classType -> {
                                if (it is CodeGitWebHookTriggerElement && it.isElementEnable()) {
                                    canWebhookStartup = true
                                    return@lit
                                }
                            }
                            CodeGithubWebHookTriggerElement.classType -> {
                                if (it is CodeGithubWebHookTriggerElement && it.isElementEnable()) {
                                    canWebhookStartup = true
                                    return@lit
                                }
                            }
                            CodeGitlabWebHookTriggerElement.classType -> {
                                if (it is CodeGitlabWebHookTriggerElement && it.isElementEnable()) {
                                    canWebhookStartup = true
                                    return@lit
                                }
                            }
                        }
                    }
                }

                if (!canWebhookStartup) {
                    logger.info("can not start by $codeRepositoryType, ignore")
                    return@outside
                }

                if (webhookTriggerPipelineBuild(pipelineId, codeRepositoryType, matcher)) return@outside
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|webhookTriggerPipelineBuild fail: $e", e)
            }
        }

        return true
    }

    fun webhookTriggerPipelineBuild(
        pipelineId: String,
        codeRepositoryType: String,
        matcher: ScmWebhookMatcher
    ): Boolean {

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId)
            ?: return false

        val model = pipelineRepositoryService.getModel(pipelineId)
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return false
        }

        val projectId = pipelineInfo.projectId
        val userId = pipelineInfo.lastModifyUser
        val variables = mutableMapOf<String, String>()
        val container = model.stages[0].containers[0] as TriggerContainer
        // 解析变量
        container.params.forEach { param ->
            variables[param.id] = param.defaultValue.toString()
        }

        // 寻找代码触发原子
        container.elements.forEach elements@{ element ->
            val webHookParams = getWebhookElementParams(element, variables) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@elements
            }

            logger.info("Get the code trigger pipeline $pipelineId branch ${webHookParams.branchName}")
            val repo = client.get(ServiceRepositoryResource::class)
                .get(projectId, repositoryConfig.getURLEncodeRepositoryId(), repositoryConfig.repositoryType).data
            if (repo == null) {
                logger.error("repo[$repositoryConfig] does not exist")
                return@elements
            }

            val matchResult = matcher.isMatch(projectId, pipelineId, repo, webHookParams)
            if (matchResult.isMatch) {
                logger.info("do git web hook match success for pipeline: $pipelineId on trigger(atom(${element.name}) of repo(${matcher.getRepoName()})) ")
                if (!element.isElementEnable()) {
                    logger.info("Trigger element is disable, can not start pipeline")
                    return@elements
                }

                try {
                    val webhookCommit = WebhookCommit(
                        userId = userId,
                        pipelineId = pipelineId,
                        params = getStartParams(
                            projectId = projectId,
                            element = element,
                            repo = repo,
                            matcher = matcher,
                            variables = variables,
                            params = webHookParams,
                            matchResult = matchResult
                        ),
                        repositoryConfig = repositoryConfig,
                        repoName = matcher.getRepoName(),
                        commitId = matcher.getRevision(),
                        block = webHookParams.block,
                        eventType = matcher.getEventType(),
                        codeType = matcher.getCodeType()
                    )
                    val buildId =
                        client.getGateway(ServiceScmWebhookResource::class).webhookCommit(projectId, webhookCommit).data

                    logger.info("[$pipelineId]| webhook trigger(atom(${element.name}) of repo(${matcher.getRepoName()})) build($buildId)")
                } catch (e: Exception) {
                    logger.warn(
                        "[$pipelineId]| webhook trigger Fail to start the atom(${element.name}) of repo(${matcher.getRepoName()})",
                        e
                    )
                }
                return false
            } else {
                logger.info("do git web hook match unsuccess for pipeline($pipelineId), trigger(atom(${element.name}) of repo(${matcher.getRepoName()}")
            }
        }
        return false
    }

    private fun getWebhookElementParams(element: Element, variables: Map<String, String>): WebHookParams? {
        var params: WebHookParams? = null
        when (element) {
            is CodeSVNWebHookTriggerElement -> {

                params = WebHookParams(
                    repositoryConfig = RepositoryConfigUtils.replaceCodeProp(
                        repositoryConfig = RepositoryConfigUtils.buildConfig(element),
                        variables = variables
                    )
                )
                params.relativePath = EnvUtils.parseEnv(element.relativePath ?: "", variables)
                params.excludeUsers = if (element.excludeUsers == null || element.excludeUsers!!.isEmpty()) {
                    ""
                } else {
                    EnvUtils.parseEnv(element.excludeUsers!!.joinToString(","), variables)
                }
                params.includeUsers = if (element.includeUsers == null || element.includeUsers!!.isEmpty()) {
                    ""
                } else {
                    EnvUtils.parseEnv(element.includeUsers!!.joinToString(","), variables)
                }
                params.excludePaths = EnvUtils.parseEnv(element.excludePaths ?: "", variables)
                params.codeType = CodeType.SVN
            }
            is CodeGitWebHookTriggerElement -> {
                params = WebHookParams(
                    repositoryConfig = RepositoryConfigUtils.replaceCodeProp(
                        repositoryConfig = RepositoryConfigUtils.buildConfig(element),
                        variables = variables
                    )
                )
                params.excludeUsers = if (element.excludeUsers == null || element.excludeUsers!!.isEmpty()) {
                    ""
                } else {
                    EnvUtils.parseEnv(element.excludeUsers!!.joinToString(","), variables)
                }
                if (element.branchName == null) {
                    return null
                }
                params.block = element.block ?: false
                params.branchName = EnvUtils.parseEnv(element.branchName!!, variables)
                params.eventType = element.eventType
                params.excludeBranchName = EnvUtils.parseEnv(element.excludeBranchName ?: "", variables)
                params.includePaths = EnvUtils.parseEnv(element.includePaths ?: "", variables)
                params.excludePaths = EnvUtils.parseEnv(element.excludePaths ?: "", variables)
                params.codeType = CodeType.GIT
            }
            is CodeGithubWebHookTriggerElement -> {
                params = WebHookParams(
                    repositoryConfig = RepositoryConfigUtils.replaceCodeProp(
                        repositoryConfig = RepositoryConfigUtils.buildConfig(element),
                        variables = variables
                    )
                )
                params.excludeUsers = if (element.excludeUsers == null || element.excludeUsers!!.isEmpty()) {
                    ""
                } else {
                    EnvUtils.parseEnv(element.excludeUsers!!, variables)
                }
                if (element.branchName == null) {
                    return null
                }
                params.branchName = EnvUtils.parseEnv(element.branchName!!, variables)
                params.eventType = element.eventType
                params.excludeBranchName = EnvUtils.parseEnv(element.excludeBranchName ?: "", variables)
                params.codeType = CodeType.GITHUB
            }
            is CodeGitlabWebHookTriggerElement -> {
                params = WebHookParams(
                    repositoryConfig = RepositoryConfigUtils.replaceCodeProp(
                        repositoryConfig = RepositoryConfigUtils.buildConfig(element),
                        variables = variables
                    )
                )
                if (element.branchName == null) {
                    return null
                }
                params.branchName = EnvUtils.parseEnv(element.branchName!!, variables)
                params.codeType = CodeType.GITLAB
            }
            else -> {
            }
        }
        return params
    }

    private fun getStartParams(
        projectId: String,
        element: Element,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        val mrRequestId = matcher.getMergeRequestId()
        val startParams = mutableMapOf<String, Any>()
        startParams[PIPELINE_WEBHOOK_REVISION] = matcher.getRevision()
        startParams[PIPELINE_REPO_NAME] = matcher.getRepoName()
        startParams[PIPELINE_START_WEBHOOK_USER_ID] = matcher.getUsername()
        startParams[PIPELINE_START_TASK_ID] = element.id!! // 当前触发节点为启动节点
        startParams[PIPELINE_WEBHOOK_TYPE] = matcher.getCodeType().name
        startParams[PIPELINE_WEBHOOK_EVENT_TYPE] = matcher.getEventType().name

        startParams[PIPELINE_WEBHOOK_REPO] = params.repositoryConfig.getRepositoryId()
        startParams[PIPELINE_WEBHOOK_REPO_TYPE] = params.repositoryConfig.repositoryType.name
        startParams[PIPELINE_WEBHOOK_BLOCK] = params.block
        startParams.putAll(matcher.getEnv())
        startParams.putAll(variables)

        if (!matcher.getBranchName().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_BRANCH] = matcher.getBranchName()!!
        }
        if (!matcher.getHookSourceUrl().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_SOURCE_URL] = matcher.getHookSourceUrl()!!
        }
        if (!matcher.getHookTargetUrl().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_TARGET_URL] = matcher.getHookTargetUrl()!!
        }

        // set new params
        startParams[BK_REPO_WEBHOOK_REPO_TYPE] = params.codeType.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = repo.url
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = repo.projectName
        startParams[BK_REPO_WEBHOOK_REPO_ALIAS_NAME] = repo.aliasName
        startParams[BK_REPO_WEBHOOK_HASH_ID] = repo.repoHashId ?: ""

        if (params.codeType == CodeType.SVN) {
            val triggerElement = element as CodeSVNWebHookTriggerElement
            val svnMatcher = matcher as SvnWebHookMatcher
            val svnEvent = svnMatcher.event
            startParams[BK_REPO_SVN_WEBHOOK_REVERSION] = matcher.getRevision()
            startParams[BK_REPO_SVN_WEBHOOK_USERNAME] = matcher.getUsername()
            startParams[BK_REPO_SVN_WEBHOOK_COMMIT_TIME] = svnEvent.commitTime ?: 0L
            startParams[BK_REPO_SVN_WEBHOOK_RELATIVE_PATH] = triggerElement.relativePath ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS] = triggerElement.excludePaths ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_INCLUDE_USERS] = triggerElement.includeUsers?.joinToString(",") ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS] = triggerElement.excludeUsers?.joinToString(",") ?: ""
        }

        if (params.codeType == CodeType.GIT) {
            val triggerElement = element as CodeGitWebHookTriggerElement
            val gitMatcher = matcher as GitWebHookMatcher
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
            startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = triggerElement.branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = triggerElement.excludeBranchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS] = triggerElement.includePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS] = triggerElement.excludePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = triggerElement.excludeUsers?.joinToString(",") ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] =
                matchResult.extra[GitWebHookMatcher.MATCH_BRANCH] ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH] = matchResult.extra[GitWebHookMatcher.MATCH_PATHS] ?: ""

            if (params.eventType == CodeEventType.MERGE_REQUEST || params.eventType == CodeEventType.MERGE_REQUEST_ACCEPT) {
                // MR提交人
                val gitScmService = SpringContextUtil.getBean(GitScmService::class.java)
                val mrInfo = gitScmService.getMergeRequestInfo(projectId, mrRequestId, repo)
                val gitMrEvent = gitMatcher.event as GitMergeRequestEvent
                val reviewers = gitScmService.getMergeRequestReviewersInfo(projectId, mrRequestId, repo)?.reviewers

                startParams[PIPELINE_WEBHOOK_MR_ID] = mrRequestId!!
                startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = mrInfo?.author?.username ?: ""
                startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
                startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""

                startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = mrInfo?.author?.username ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = matcher.getHookTargetUrl() ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = matcher.getHookSourceUrl() ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = mrInfo?.createTime ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = mrInfo?.updateTime ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] =
                    DateTimeUtils.zoneDateToTimestamp(mrInfo?.createTime)
                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] =
                    DateTimeUtils.zoneDateToTimestamp(mrInfo?.updateTime)
                startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = mrInfo?.mrId ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = mrInfo?.mrNumber ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = mrInfo?.description ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = mrInfo?.title ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = mrInfo?.assignee?.username ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = gitMrEvent.object_attributes.url
                startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] = reviewers?.joinToString(",") { it.username } ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = mrInfo?.milestone?.title ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = mrInfo?.milestone?.dueDate ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = mrInfo?.labels?.joinToString(",") ?: ""
            }

            if (params.eventType == CodeEventType.TAG_PUSH) {
                val gitTagPushEvent = gitMatcher.event as GitTagPushEvent
                startParams[BK_REPO_GIT_WEBHOOK_TAG_NAME] = matcher.getBranchName()
                startParams[BK_REPO_GIT_WEBHOOK_TAG_OPERATION] = gitTagPushEvent.operation_kind ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_TAG_USERNAME] = matcher.getUsername()
            }

            if (params.eventType == CodeEventType.PUSH) {
                startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = matcher.getUsername()
                startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName()
            }
        }

        if (params.codeType == CodeType.GITHUB) {
            val triggerElement = element as CodeGithubWebHookTriggerElement
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
            startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = triggerElement.branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = triggerElement.excludeBranchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = triggerElement.excludeUsers ?: ""

            val githubMatcher = matcher as GithubWebHookMatcher
            if (params.eventType == CodeEventType.PULL_REQUEST) {
                val githubEvent = githubMatcher.event as GithubPullRequestEvent
                startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = githubEvent.sender.login
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = githubEvent.pull_request.base.repo.clone_url
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = githubEvent.pull_request.head.repo.clone_url
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = githubEvent.pull_request.base.ref
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = githubEvent.pull_request.head.ref
                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = githubEvent.pull_request.created_at ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = githubEvent.pull_request.update_at ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = githubEvent.pull_request.id
                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = githubEvent.number
                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = githubEvent.pull_request.comments_url ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = githubEvent.pull_request.title ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] =
                    githubEvent.pull_request.assignees.joinToString(",") { it.login ?: "" }
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = githubEvent.pull_request.url
                startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
                    githubEvent.pull_request.requested_reviewers.joinToString(",") { it.login ?: "" }
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = githubEvent.pull_request.milestone?.title ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] =
                    githubEvent.pull_request.milestone?.due_on ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] =
                    githubEvent.pull_request.labels.joinToString(",") { it.name }
            }

            if (params.eventType == CodeEventType.CREATE) {
                val githubEvent = githubMatcher.event as GithubCreateEvent
                startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME] = githubEvent.ref
                startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE] = githubEvent.ref_type
                startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME] = githubEvent.sender.login
            }

            if (params.eventType == CodeEventType.PUSH) {
                startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = matcher.getUsername()
                startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName()
            }
        }

        if (params.codeType == CodeType.GITLAB) {
            val triggerElement = element as CodeGitlabWebHookTriggerElement
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = triggerElement.branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName() ?: ""
        }

        return startParams
    }

    /**
     * webhookCommitTriggerPipelineBuild 方法是webhook事件触发最后执行方法
     * @link webhookTriggerPipelineBuild 方法接收webhook事件后通过调用网关接口进行分发，从而区分正式和灰度服务
     * @param projectId 项目ID
     * @param webhookCommit webhook事件信息
     *
     */
    fun webhookCommitTriggerPipelineBuild(projectId: String, webhookCommit: WebhookCommit): String {
        val userId = webhookCommit.userId
        val pipelineId = webhookCommit.pipelineId
        val startParams = webhookCommit.params

        val repositoryConfig = webhookCommit.repositoryConfig
        val repoName = webhookCommit.repoName
        val commitId = webhookCommit.commitId
        val block = webhookCommit.block

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId)
            ?: throw RuntimeException("Pipeline($pipelineId) not found")

        val model = pipelineRepositoryService.getModel(pipelineId)
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return ""
        }

        // 添加质量红线原子
        val fullModel = pipelineBuildQualityService.fillingRuleInOutElement(projectId, pipelineId, startParams, model)
        // 兼容从旧v1版本下发过来的请求携带旧的变量命名
        val params = startParams.map { (PipelineVarUtil.oldVarToNewVar(it.key) ?: it.key) to it.value }.toMap()

        val startParamsWithType = mutableListOf<BuildParameters>()
        params.forEach { t, u -> startParamsWithType.add(
            BuildParameters(
                t,
                u
            )
        ) }

        try {
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                readyToBuildPipelineInfo = pipelineInfo,
                startType = StartType.WEB_HOOK,
                startParamsWithType = startParamsWithType,
                channelCode = pipelineInfo.channelCode,
                isMobile = false,
                model = fullModel,
                signPipelineVersion = pipelineInfo.version,
                frequencyLimit = false
            )

            when {
                webhookCommit.eventType == CodeEventType.MERGE_REQUEST && webhookCommit.codeType == CodeType.GIT -> {
                    logger.info("Web hook add git commit check [pipelineId=$pipelineId, buildId=$buildId, repo=$repositoryConfig, commitId=$commitId]")
                    pipelineEventDispatcher.dispatch(
                        GitCommitCheckEvent(
                            source = "codeWebhook_pipeline_build_trigger",
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            repositoryConfig = repositoryConfig,
                            commitId = commitId,
                            state = GIT_COMMIT_CHECK_STATE_PENDING,
                            block = block
                        )
                    )
                }
                webhookCommit.eventType == CodeEventType.PULL_REQUEST && webhookCommit.codeType == CodeType.GITHUB -> {
                    logger.info("Web hook add github pr check [pipelineId=$pipelineId, buildId=$buildId, repo=$repositoryConfig, commitId=$commitId]")
                    pipelineEventDispatcher.dispatch(
                        GithubPrEvent(
                            source = "codeWebhook_pipeline_build_trigger",
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            repositoryConfig = repositoryConfig,
                            commitId = commitId,
                            status = GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS,
                            startedAt = LocalDateTime.now().timestamp(),
                            conclusion = null,
                            completedAt = null
                        )
                    )
                }
                else -> {
                    logger.info("Code web hook event ignored")
                }
            }
            logger.info("[$pipelineId]| webhook trigger of repo($repoName)) build [$buildId]")
            return buildId
        } catch (e: Exception) {
            logger.warn("[$pipelineId]| webhook trigger fail to start repo($repoName): ${e.message}", e)
            return ""
        }
    }
}