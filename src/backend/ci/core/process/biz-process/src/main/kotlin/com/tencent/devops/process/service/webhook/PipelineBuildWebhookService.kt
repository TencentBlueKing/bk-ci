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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.engine.service.code.GitWebhookUnlockDispatcher
import com.tencent.devops.process.engine.service.code.ScmWebhookMatcherBuilder
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.builds.PipelineBuildCommitService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
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
        pipelineTriggerEventService = applicationContext.getBean(PipelineTriggerEventService::class.java)
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
        lateinit var pipelineTriggerEventService: PipelineTriggerEventService
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookService::class.java)
    }

    fun dispatchTriggerPipelines(
        matcher: ScmWebhookMatcher,
        triggerEvent: PipelineTriggerEvent,
        triggerPipelines: List<WebhookTriggerPipeline>
    ): Boolean {
        try {
            logger.info("dispatch pipeline webhook subscriber|repo(${matcher.getRepoName()})")

            if (triggerPipelines.isEmpty()) {
                gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
                return false
            }

            EventCacheUtil.initEventCache()
            // 代码库触发的事件ID,一个代码库会触发多条流水线,但应该只有一条触发事件
            val repoEventIdMap = mutableMapOf<String, Long>()
            triggerPipelines.forEach outside@{ subscriber ->
                val projectId = subscriber.projectId
                val pipelineId = subscriber.pipelineId
                try {
                    logger.info("pipelineId is $pipelineId")
                    val builder = PipelineTriggerDetailBuilder()
                        .projectId(projectId)
                        .pipelineId(pipelineId)

                    webhookTriggerPipelineBuild(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        matcher = matcher,
                        builder = builder
                    )
                    saveTriggerEvent(
                        projectId = projectId,
                        builder = builder,
                        triggerEvent = triggerEvent,
                        repoEventIdMap = repoEventIdMap
                    )
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
        }
    }

    private fun saveTriggerEvent(
        projectId: String,
        builder: PipelineTriggerDetailBuilder,
        triggerEvent: PipelineTriggerEvent,
        repoEventIdMap: MutableMap<String, Long>
    ) {
        if (!builder.getEventSource().isNullOrBlank()) {
            triggerEvent.eventSource = builder.getEventSource()
            triggerEvent.projectId = projectId
            val eventId = repoEventIdMap[builder.getEventSource()] ?: run {
                val eventId = pipelineTriggerEventService.getEventId(
                    projectId = projectId, requestId = triggerEvent.requestId, eventSource = triggerEvent.eventSource!!
                )
                repoEventIdMap[builder.getEventSource()!!] = eventId
                eventId
            }
            triggerEvent.eventId = eventId
            builder.eventId(eventId)
            pipelineTriggerEventService.saveEvent(
                triggerEvent = triggerEvent,
                triggerDetail = builder.build()
            )
        }
    }

    open fun webhookTriggerPipelineBuild(
        projectId: String,
        pipelineId: String,
        matcher: ScmWebhookMatcher,
        builder: PipelineTriggerDetailBuilder
    ): Boolean {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return false

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return false
        }
        // 触发事件保存流水线名称
        builder.pipelineName(pipelineInfo.pipelineName)
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
                client.get(ServiceRepositoryResource::class).get(
                    projectId,
                    repositoryConfig.getURLEncodeRepositoryId(),
                    repositoryConfig.repositoryType
                ).data
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
                    logger.info("$pipelineId|$buildId|webhook trigger|(${element.name}|repo(${matcher.getRepoName()})")
                    if (!buildId.isNullOrEmpty()) {
                        pipelineBuildCommitService.create(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            matcher = matcher,
                            repo = repo
                        )
                        val buildDetail = client.getGateway(ServiceBuildResource::class).getBuildDetail(
                            userId = userId,
                            buildId = buildId,
                            pipelineId = pipelineId,
                            projectId = projectId,
                            channelCode = ChannelCode.BS
                        ).data
                        builder.buildId(buildId)
                            .status(PipelineTriggerStatus.SUCCEED.name)
                            .eventSource(eventSource = repo.repoHashId!!)
                            .reason(PipelineTriggerReason.TRIGGER_SUCCESS.name)
                            .buildNum(buildDetail?.buildNum.toString())
                    }
                } catch (ignore: Exception) {
                    logger.warn("$pipelineId|webhook trigger|(${element.name})|repo(${matcher.getRepoName()})", ignore)
                }
                return false
            } else {
                logger.info(
                    "$pipelineId|webhook trigger match unsuccess|(${element.name})|repo(${matcher.getRepoName()})"
                )
                if (!matchResult.reason.isNullOrBlank()) {
                    builder.eventSource(eventSource = repo.repoHashId!!)
                        .reasonDetail(JsonUtil.toJson(
                            PipelineTriggerReasonDetail(
                                elementId = element.id,
                                elementName = element.name,
                                elementAtomCode = element.getAtomCode(),
                                reasonMsg = matchResult.reason!!
                            )
                        ))
                }
            }
        }

        // 历史原因,webhook表没有记录eventType,所以查找出来的订阅者可能因为事件类型不匹配,事件不需要记录
        if (!builder.getEventSource().isNullOrBlank()) {
            builder.status(PipelineTriggerStatus.FAILED.name).reason(PipelineTriggerReason.TRIGGER_NOT_MATCH.name)
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
