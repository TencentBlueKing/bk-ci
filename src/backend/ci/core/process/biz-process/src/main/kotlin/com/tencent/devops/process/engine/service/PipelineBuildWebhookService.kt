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
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.engine.service.code.GitWebHookMatcher
import com.tencent.devops.process.engine.service.code.GithubWebHookMatcher
import com.tencent.devops.process.engine.service.code.GitlabWebHookMatcher
import com.tencent.devops.process.engine.service.code.ScmWebhookParamsFactory
import com.tencent.devops.process.engine.service.code.SvnWebHookMatcher
import com.tencent.devops.process.engine.utils.RepositoryUtils
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.code.github.GithubCreateEvent
import com.tencent.devops.process.pojo.code.github.GithubEvent
import com.tencent.devops.process.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.process.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.process.pojo.scm.code.GitlabCommitEvent
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_PENDING
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

    fun externalCodeGitBuild(codeRepositoryType: String, e: String): Boolean {
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

        return startProcessByWebhook(codeRepositoryType, gitWebHookMatcher)
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
                var canWebhookStartup = canWebhookStartup(triggerContainer, codeRepositoryType)

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

    private fun canWebhookStartup(
        triggerContainer: TriggerContainer,
        codeRepositoryType: String
    ): Boolean {
        var canWebhookStartup = false
        run lit@{
            triggerContainer.elements.forEach {
                when (codeRepositoryType) {
                    CodeSVNWebHookTriggerElement.classType -> {
                        if ((it is CodeSVNWebHookTriggerElement && it.isElementEnable()) ||
                            canGitGenericWebhookStartUp(it)
                        ) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGitWebHookTriggerElement.classType -> {
                        if ((it is CodeGitWebHookTriggerElement && it.isElementEnable()) ||
                            canGitGenericWebhookStartUp(it)) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGithubWebHookTriggerElement.classType -> {
                        if ((it is CodeGithubWebHookTriggerElement && it.isElementEnable()) ||
                            canGitGenericWebhookStartUp(it)
                        ) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGitlabWebHookTriggerElement.classType -> {
                        if ((it is CodeGitlabWebHookTriggerElement && it.isElementEnable()) ||
                            canGitGenericWebhookStartUp(it)
                        ) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeTGitWebHookTriggerElement.classType -> {
                        if ((it is CodeTGitWebHookTriggerElement && it.isElementEnable()) ||
                            canGitGenericWebhookStartUp(it)
                        ) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                }
            }
        }
        return canWebhookStartup
    }

    private fun canGitGenericWebhookStartUp(
        element: Element
    ): Boolean {
        if (element is CodeGitGenericWebHookTriggerElement && element.isElementEnable()) {
            return true
        }
        return false
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
            val webHookParams = ScmWebhookParamsFactory.getWebhookElementParams(element, variables) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@elements
            }

            logger.info("Get the code trigger pipeline $pipelineId branch ${webHookParams.branchName}")
            val repo = if (element is CodeGitGenericWebHookTriggerElement) {
                RepositoryUtils.buildRepository(
                    projectId = pipelineInfo.projectId,
                    userName = pipelineInfo.lastModifyUser,
                    scmType = ScmType.valueOf(element.data.input.scmType),
                    repositoryUrl = repositoryConfig.repositoryName!!,
                    credentialId = element.data.input.credentialId
                )
            } else {
                client.get(ServiceRepositoryResource::class)
                    .get(projectId, repositoryConfig.getURLEncodeRepositoryId(), repositoryConfig.repositoryType).data
            }
            if (repo == null) {
                logger.warn("repo[$repositoryConfig] does not exist")
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
                        params = ScmWebhookParamsFactory.getStartParams(
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
        val params = mutableMapOf<String, Any>()
        val startParamsWithType = mutableListOf<BuildParameters>()
        startParams.forEach {
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                params[it.key] = it.value
                startParamsWithType.add(BuildParameters(it.key, it.value))
            } else if (!params.contains(newVarName)) { // 新变量还不存在，加入
                params[newVarName] = it.value
                startParamsWithType.add(BuildParameters(newVarName, it.value))
            }
        }

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
                (webhookCommit.eventType == CodeEventType.MERGE_REQUEST ||
                    webhookCommit.eventType == CodeEventType.PUSH) &&
                    webhookCommit.codeType == CodeType.GIT -> {
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