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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.RepositoryTypeNew
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCheckRunEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.pojo.code.p4.P4Event
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import com.tencent.devops.process.engine.service.PipelineWebhookBuildLogContext
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.engine.service.code.GitWebhookUnlockDispatcher
import com.tencent.devops.process.engine.service.code.ScmWebhookMatcherBuilder
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.service.builds.PipelineBuildCommitService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.utils.RepositoryUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@Suppress("ALL")
abstract class PipelineBuildWebhookService : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        objectMapper = applicationContext.getBean(ObjectMapper::class.java)
        client = applicationContext.getBean(Client::class.java)
        pipelineWebhookService = applicationContext.getBean(PipelineWebhookService::class.java)
        pipelineRepositoryService = applicationContext.getBean(PipelineRepositoryService::class.java)
        pipelineBuildService = applicationContext.getBean(PipelineBuildService::class.java)
        scmWebhookMatcherBuilder = applicationContext.getBean(ScmWebhookMatcherBuilder::class.java)
        gitWebhookUnlockDispatcher = applicationContext.getBean(GitWebhookUnlockDispatcher::class.java)
        pipelineWebHookQueueService = applicationContext.getBean(PipelineWebHookQueueService::class.java)
        buildLogPrinter = applicationContext.getBean(BuildLogPrinter::class.java)
        pipelinebuildWebhookService = applicationContext.getBean(PipelineBuildWebhookService::class.java)
        pipelineBuildCommitService = applicationContext.getBean(PipelineBuildCommitService::class.java)
        webhookBuildParameterService = applicationContext.getBean(WebhookBuildParameterService::class.java)
    }

    companion object {
        lateinit var objectMapper: ObjectMapper
        lateinit var client: Client
        lateinit var pipelineWebhookService: PipelineWebhookService
        lateinit var pipelineRepositoryService: PipelineRepositoryService
        lateinit var pipelineBuildService: PipelineBuildService
        lateinit var scmWebhookMatcherBuilder: ScmWebhookMatcherBuilder
        lateinit var gitWebhookUnlockDispatcher: GitWebhookUnlockDispatcher
        lateinit var pipelineWebHookQueueService: PipelineWebHookQueueService
        lateinit var buildLogPrinter: BuildLogPrinter
        lateinit var pipelinebuildWebhookService: PipelineBuildWebhookService // 给AOP调用
        lateinit var pipelineBuildCommitService: PipelineBuildCommitService
        lateinit var webhookBuildParameterService: WebhookBuildParameterService
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookService::class.java)
    }

    fun externalCodeSvnBuild(e: String): Boolean {
        logger.info("Trigger code svn build - $e")

        val event = try {
            objectMapper.readValue(e, SvnCommitEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the svn web hook commit event", e)
            return false
        }

        val svnWebHookMatcher = scmWebhookMatcherBuilder.createSvnWebHookMatcher(event)

        return startProcessByWebhook(CodeSVNWebHookTriggerElement.classType, svnWebHookMatcher)
    }

    fun externalCodeGitBuild(codeRepositoryType: String, event: String?, body: String): Boolean {
        logger.info("Trigger code git build($body|$event)")

        val gitEvent = try {
            if (event == "Review Hook") {
                objectMapper.readValue<GitReviewEvent>(body)
            } else {
                objectMapper.readValue<GitEvent>(body)
            }
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event", e)
            return false
        }

        val gitWebHookMatcher = scmWebhookMatcherBuilder.createGitWebHookMatcher(gitEvent)
        if (!gitWebHookMatcher.preMatch().isMatch) {
            return true
        }

        return startProcessByWebhook(codeRepositoryType, gitWebHookMatcher)
    }

    fun externalGitlabBuild(e: String): Boolean {
        logger.info("Trigger gitlab build($e)")

        val event = try {
            objectMapper.readValue(e, GitEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the gitlab web hook commit event", e)
            return false
        }

        val gitlabWebHookMatcher = scmWebhookMatcherBuilder.createGitlabWebHookMatcher(event)

        return startProcessByWebhook(CodeGitlabWebHookTriggerElement.classType, gitlabWebHookMatcher)
    }

    fun externalCodeGithubBuild(eventType: String, guid: String, signature: String, body: String): Boolean {
        logger.info("Trigger code github build (event=$eventType, guid=$guid, signature=$signature, body=$body)")

        val event: GithubEvent = when (eventType) {
            GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(body)
            GithubCreateEvent.classType -> objectMapper.readValue<GithubCreateEvent>(body)
            GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(body)
            GithubCheckRunEvent.classType -> objectMapper.readValue<GithubCheckRunEvent>(body)
            else -> {
                logger.info("Github event($eventType) is ignored")
                return true
            }
        }
        val githubWebHookMatcher = scmWebhookMatcherBuilder.createGithubWebHookMatcher(event)
        if (!githubWebHookMatcher.preMatch().isMatch) {
            return true
        }
        if (event is GithubCheckRunEvent) {
            if (event.action != "rerequested") {
                logger.info("Unsupported check run action:${event.action}")
                return true
            }
            if (event.checkRun.externalId == null) {
                logger.info("github check run externalId is empty")
                return true
            }
            val buildInfo = event.checkRun.externalId!!.split("_")
            if (buildInfo.size < 4) {
                logger.info("the buildInfo of github check run is error")
                return true
            }
            client.get(ServiceBuildResource::class).retry(
                userId = buildInfo[0],
                projectId = buildInfo[1],
                pipelineId = buildInfo[2],
                buildId = buildInfo[3],
                channelCode = ChannelCode.BS
            )
            return true
        }
        return startProcessByWebhook(CodeGithubWebHookTriggerElement.classType, githubWebHookMatcher)
    }

    fun externalP4Build(body: String): Boolean {
        logger.info("Trigger p4 build($body)")

        val event = try {
            objectMapper.readValue(body, P4Event::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the p4 web hook event", e)
            return false
        }

        val p4WebHookMatcher = scmWebhookMatcherBuilder.createP4WebHookMatcher(event)

        return startProcessByWebhook(CodeP4WebHookTriggerElement.classType, p4WebHookMatcher)
    }

    private fun startProcessByWebhook(codeRepositoryType: String, matcher: ScmWebhookMatcher): Boolean {
        val watcher = Watcher("${matcher.getRepoName()}|${matcher.getRevision()}|webhook trigger")
        PipelineWebhookBuildLogContext.addRepoInfo(repoName = matcher.getRepoName(), commitId = matcher.getRevision())
        try {
            watcher.start("getWebhookPipelines")
            logger.info("startProcessByWebhook|repo(${matcher.getRepoName()})|type($codeRepositoryType)")
            val pipelines = pipelineWebhookService.getWebhookPipelines(
                name = matcher.getRepoName(),
                type = codeRepositoryType
            )

            if (pipelines.isEmpty()) {
                gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
                return false
            }

            watcher.start("webhookTriggerPipelineBuild")
            EventCacheUtil.initEventCache()
            pipelines.forEach outside@{ pipeline ->
                val projectId = pipeline.first
                val pipelineId = pipeline.second
                try {
                    logger.info("pipelineId is $pipelineId")
                    val model = pipelineRepositoryService.getModel(projectId, pipelineId) ?: run {
                        logger.info("$pipelineId|pipeline does not exists, ignore")
                        return@outside
                    }

                    /**
                     * 验证流水线参数构建启动参数
                     */
                    val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                    val canWebhookStartup = canWebhookStartup(triggerContainer, codeRepositoryType)

                    if (!canWebhookStartup) {
                        logger.info("$pipelineId|can not start by $codeRepositoryType, ignore")
                        return@outside
                    }

                    if (pipelinebuildWebhookService.webhookTriggerPipelineBuild(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            codeRepositoryType = codeRepositoryType,
                            matcher = matcher
                        )
                    ) return@outside
                } catch (e: Throwable) {
                    logger.warn("[$pipelineId]|webhookTriggerPipelineBuild fail: $e", e)
                }
            }
            /* #3131,当对mr的commit check有强依赖，但是蓝盾与git的commit check交互存在一定的时延，可以增加双重锁。
                git发起mr时锁住mr,称为webhook锁，由蓝盾主动发起解锁，解锁有三种情况：
                1. 仓库没有配置蓝盾的流水线，需要解锁
                2. 仓库配置了蓝盾流水线，但是流水线都不需要锁住mr，需要解锁
                3. 仓库配置了蓝盾流水线并且需要锁住mr，需要等commit check发送完成，再解锁
                 @see com.tencent.devops.plugin.service.git.CodeWebhookService.addGitCommitCheck
             */
            gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
            return true
        } finally {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "webhook event repository cache: ${JsonUtil.toJson(EventCacheUtil.getAll(), false)}"
                )
            }
            EventCacheUtil.remove()
            logger.info("$watcher")
        }
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
                        if ((it is CodeSVNWebHookTriggerElement && it.isElementEnable())) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGitWebHookTriggerElement.classType -> {
                        if ((it is CodeGitWebHookTriggerElement && it.isElementEnable())) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGithubWebHookTriggerElement.classType -> {
                        if ((it is CodeGithubWebHookTriggerElement && it.isElementEnable())) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeGitlabWebHookTriggerElement.classType -> {
                        if ((it is CodeGitlabWebHookTriggerElement && it.isElementEnable())) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeTGitWebHookTriggerElement.classType -> {
                        if ((it is CodeTGitWebHookTriggerElement && it.isElementEnable())) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                    CodeP4WebHookTriggerElement.classType -> {
                        if (it is CodeP4WebHookTriggerElement && it.isElementEnable()) {
                            canWebhookStartup = true
                            return@lit
                        }
                    }
                }
            }
        }
        return canWebhookStartup
    }

    open fun webhookTriggerPipelineBuild(
        projectId: String,
        pipelineId: String,
        codeRepositoryType: String,
        matcher: ScmWebhookMatcher
    ): Boolean {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return false

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return false
        }

        val userId = pipelineInfo.lastModifyUser
        val variables = mutableMapOf<String, String>()
        val container = model.stages[0].containers[0] as TriggerContainer
        // 解析变量
        container.params.forEach { param ->
            variables[param.id] = param.defaultValue.toString()
        }

        // 寻找代码触发原子
        container.elements.forEach elements@{ element ->
            if (!element.isElementEnable() || element !is WebHookTriggerElement) {
                logger.info("Trigger element is disable, can not start pipeline")
                return@elements
            }
            val webHookParams = WebhookElementParamsRegistrar.getService(element)
                .getWebhookElementParams(element, variables) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@elements
            }

            logger.info("$pipelineId|${element.name}|Get the code trigger pipeline")
            // #2958 如果仓库找不到,会抛出404异常,就不会继续往下遍历
            val repo = try {
                if (element is CodeGitGenericWebHookTriggerElement &&
                    element.data.input.repositoryType == RepositoryTypeNew.URL
                ) {
                    RepositoryUtils.buildRepository(
                        projectId = pipelineInfo.projectId,
                        userName = pipelineInfo.lastModifyUser,
                        scmType = ScmType.valueOf(element.data.input.scmType),
                        repositoryUrl = repositoryConfig.repositoryName!!,
                        credentialId = element.data.input.credentialId
                    )
                } else {
                    client.get(ServiceRepositoryResource::class)
                        .get(
                            projectId,
                            repositoryConfig.getURLEncodeRepositoryId(),
                            repositoryConfig.repositoryType
                        ).data
                }
            } catch (e: Exception) {
                null
            }
            if (repo == null) {
                logger.warn("$pipelineId|repo[$repositoryConfig] does not exist")
                return@elements
            }

            val matchResult = matcher.isMatch(projectId, pipelineId, repo, webHookParams)
            if (matchResult.isMatch) {
                try {
                    val webhookCommit = WebhookCommit(
                        userId = userId,
                        pipelineId = pipelineId,
                        params = WebhookStartParamsRegistrar.getService(element).getStartParams(
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
                    PipelineWebhookBuildLogContext.addLogBuildInfo(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = element.id!!,
                        taskName = element.name,
                        success = true,
                        triggerResult = buildId,
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("PIPELINE_WEBHOOK_BUILD_LOG_DETAIL").data
                    )
                    logger.info("$pipelineId|$buildId|webhook trigger|(${element.name}|repo(${matcher.getRepoName()})")
                    if (!buildId.isNullOrEmpty()) {
                        pipelineBuildCommitService.create(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            matcher = matcher,
                            repo = repo
                        )
                    }
                } catch (ignore: Exception) {
                    logger.warn("$pipelineId|webhook trigger|(${element.name})|repo(${matcher.getRepoName()})", ignore)
                }
                return false
            } else {
                logger.info(
                    "$pipelineId|webhook trigger match unsuccess|(${element.name})|repo(${matcher.getRepoName()})"
                )
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

        val repoName = webhookCommit.repoName

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw IllegalArgumentException("Pipeline($pipelineId) not found")
        checkPermission(pipelineInfo.lastModifyUser, projectId = projectId, pipelineId = pipelineId)

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return ""
        }

        // 兼容从旧v1版本下发过来的请求携带旧的变量命名
        val params = mutableMapOf<String, Any>()
        val pipelineParamMap = HashMap<String, BuildParameters>(startParams.size, 1F)
        startParams.forEach {
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                params[it.key] = it.value
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value)
            } else if (!params.contains(newVarName)) { // 新变量还不存在，加入
                params[newVarName] = it.value
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value)
            }
        }

        val startEpoch = System.currentTimeMillis()
        try {
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipelineInfo,
                startType = StartType.WEB_HOOK,
                pipelineParamMap = HashMap(pipelineParamMap),
                channelCode = pipelineInfo.channelCode,
                isMobile = false,
                model = model,
                signPipelineVersion = pipelineInfo.version,
                frequencyLimit = false
            ).id
            pipelineWebHookQueueService.onWebHookTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = webhookCommit.params
            )
            // #2958 webhook触发在触发原子上输出变量
            buildLogPrinter.addLines(
                buildId = buildId,
                logMessages = pipelineParamMap.map {
                    LogMessage(
                        message = "${it.key}=${it.value.value}",
                        timestamp = System.currentTimeMillis(),
                        tag = startParams[PIPELINE_START_TASK_ID]?.toString() ?: ""
                    )
                }
            )
            if (buildId.isNotBlank()) {
                webhookBuildParameterService.save(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildParameters = pipelineParamMap.values.toList()
                )
            }
            return buildId
        } catch (ignore: Exception) {
            logger.warn("[$pipelineId]| webhook trigger fail to start repo($repoName): ${ignore.message}", ignore)
            return ""
        } finally {
            logger.info("$pipelineId|WEBHOOK_TRIGGER|repo=$repoName|time=${System.currentTimeMillis() - startEpoch}")
        }
    }

    abstract fun checkPermission(userId: String, projectId: String, pipelineId: String)
}
