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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_MANUAL_UNLOCK
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SOURCE_WEBHOOK
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.trigger.WebhookTriggerService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitPushHook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestHook
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue

/**
 * 历史的webhook与通过devops-scm的webhook数据对比
 */
@Service
class WebhookGrayCompareService @Autowired constructor(
    private val client: Client,
    private val webhookTriggerService: WebhookTriggerService,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineYamlService: PipelineYamlService,
    private val webhookTriggerMatcher: WebhookTriggerMatcher
) {

    private val executor = ThreadPoolUtil.getThreadPoolExecutor(
        corePoolSize = 1,
        maximumPoolSize = 1,
        keepAliveTime = 0L,
        queue = LinkedBlockingQueue(1),
        threadNamePrefix = "webhook-gray-compare-%d"
    )

    fun asyncCompareWebhook(
        scmType: ScmType,
        request: WebhookRequest,
        matcher: ScmWebhookMatcher
    ) {
        ThreadPoolUtil.submitAction(
            executor = executor,
            actionTitle = "async compare webhook|scmType: $scmType|repoName: ${matcher.getRepoName()}",
            action = { compareWebhook(scmType, request, matcher) }
        )
    }

    fun compareWebhook(scmType: ScmType, request: WebhookRequest, matcher: ScmWebhookMatcher) {
        try {
            val oldPipelineAndParams = getOldSuccessfulPipelines(scmType = scmType, matcher = matcher)
            val newPipelineAndParams = getNewSuccessfulPipelines(scmType = scmType, request = request)
            if (!comparePipeline(oldPipelineAndParams, newPipelineAndParams, scmType, matcher)) {
                return
            }
            compareParams(oldPipelineAndParams, newPipelineAndParams, scmType, matcher)
        } catch (ignored: Exception) {
            logger.warn("Failed to compare webhook|scmType: $scmType|repoName: ${matcher.getRepoName()}", ignored)
        }
    }

    private fun comparePipeline(
        oldPipelineAndParams: Map<String, Map<String, Any>>,
        newPipelineAndParams: Map<String, Map<String, Any>>,
        scmType: ScmType,
        matcher: ScmWebhookMatcher
    ): Boolean {
        if (oldPipelineAndParams.size != newPipelineAndParams.size) {
            // 新比旧少的流水线
            val miss = oldPipelineAndParams.keys.minus(newPipelineAndParams.keys)
            // 新比旧多的流水线
            val add = newPipelineAndParams.keys.minus(oldPipelineAndParams.keys)
            logger.warn(
                "compare webhook exception|the number of pipelines differs|" +
                        "scmType: $scmType|repoName: ${matcher.getRepoName()}|miss:$miss|add:$add"
            )
            return false
        }
        if (!oldPipelineAndParams.keys.containsAll(newPipelineAndParams.keys)) {
            // 新比旧少的流水线
            val miss = oldPipelineAndParams.keys.minus(newPipelineAndParams.keys)
            // 新比旧多的流水线
            val add = newPipelineAndParams.keys.minus(oldPipelineAndParams.keys)
            logger.warn(
                "compare webhook exception|old not contains all new|" +
                        "scmType: $scmType|repoName: ${matcher.getRepoName()}|miss:$miss|add:$add",
            )
            return false
        }
        if (!newPipelineAndParams.keys.containsAll(oldPipelineAndParams.keys)) {
            // 新比旧少的流水线
            val miss = oldPipelineAndParams.keys.minus(newPipelineAndParams.keys)
            // 新比旧多的流水线
            val add = newPipelineAndParams.keys.minus(oldPipelineAndParams.keys)
            logger.warn(
                "compare webhook exception|new not contains all old|" +
                        "scmType: $scmType|repoName: ${matcher.getRepoName()}|miss:$miss|add:$add"
            )
            return false
        }
        return true
    }

    @Suppress("NestedBlockDepth")
    private fun compareParams(
        oldPipelineAndParams: Map<String, Map<String, Any>>,
        newPipelineAndParams: Map<String, Map<String, Any>>,
        scmType: ScmType,
        matcher: ScmWebhookMatcher
    ) {
        val newMissVar = mutableSetOf<String>()
        val diffValueKeys = mutableSetOf<String>()
        val diffValues = mutableSetOf<String>()
        oldPipelineAndParams.forEach { (pipelineId, oldParams) ->
            val newParams = newPipelineAndParams[pipelineId] ?: return@forEach
            // 部分字段存在时效性，无需对比
            oldParams.filter { !IGNORED_PARAM_KEYS.contains(it.key) }.forEach eachParam@{ (key, value) ->
                if (IGNORED_PARAM_PREFIX.any { key.contains(it) }) {
                    return@eachParam
                }
                val oldValue = value.toString()
                // 旧值为空字符串, 新值不存在, 直接忽略
                if (oldValue.isBlank() && !newParams.containsKey(key)) {
                    return@eachParam
                }
                if (newParams.containsKey(key)) {
                    // 同步新旧参数类型为String
                    val newValue = newParams[key]?.toString() ?: ""
                    if (oldValue != newValue) {
                        diffValueKeys.add(key)
                        diffValues.add("$key:[$oldValue|$newValue]")
                    }
                } else {
                    newMissVar.add(key)
                }
            }
        }
        if (newMissVar.isNotEmpty()) {
            logger.warn(
                "compare webhook exception|new miss var|" +
                        "scmType: $scmType|repoName: ${matcher.getRepoName()}|newMissVar:$newMissVar",
            )
            return
        }
        if (diffValueKeys.isNotEmpty()) {
            logger.warn(
                "compare webhook exception|var value diff|scmType: $scmType|repoName:${matcher.getRepoName()}|" +
                        "diffValueKeys:$diffValueKeys|diffValues:$diffValues",
            )
            return
        }
    }

    private fun getOldSuccessfulPipelines(
        scmType: ScmType,
        matcher: ScmWebhookMatcher
    ): Map<String, Map<String, Any>> {
        val yamlPipelineIds = webhookTriggerService.getYamlPipelineIds(matcher, scmType)
        val triggerPipelines = pipelineWebhookService.getTriggerPipelines(
            name = matcher.getRepoName(),
            repositoryType = scmType,
            yamlPipelineIds = yamlPipelineIds
        )
        val pipelineAndParamsMap = mutableMapOf<String, Map<String, Any>>()
        triggerPipelines.forEach { (projectId, pipelineId) ->
            try {
                // 预匹配不过，忽略对比
                if (!matcher.preMatch().isMatch) {
                    return@forEach
                }
                oldWebhookTrigger(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    matcher = matcher,
                    pipelineAndParamsMap = pipelineAndParamsMap
                )
            } catch (e: Throwable) {
                logger.warn("Failed to get old successful pipeline|projectId: $projectId|pipelineId: $pipelineId")
            }
        }
        return pipelineAndParamsMap
    }

    private fun oldWebhookTrigger(
        projectId: String,
        pipelineId: String,
        matcher: ScmWebhookMatcher,
        pipelineAndParamsMap: MutableMap<String, Map<String, Any>>
    ) {
        pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val model = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)?.model ?: return
        val variables = mutableMapOf<String, String>()
        val container = model.getTriggerContainer()
        // 解析变量
        container.params.forEach { param ->
            variables[param.id] = param.defaultValue.toString()
        }
        container.elements.forEach elements@{ element ->
            if (!element.elementEnabled() || element !is WebHookTriggerElement) {
                return@elements
            }
            val webHookParams = WebhookElementParamsRegistrar.getService(element)
                .getWebhookElementParams(element, PipelineVarUtil.fillVariableMap(variables)) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                return@elements
            }

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
                return@elements
            }

            val matchResult = matcher.isMatch(projectId, pipelineId, repo, webHookParams)
            logger.info(
                "old webhook trigger|pipelineId:$pipelineId|element:${element.id}|matchResult:${matchResult.isMatch}"
            )
            if (matchResult.isMatch) {
                val params = WebhookStartParamsRegistrar.getService(element).getStartParams(
                    projectId = projectId,
                    element = element,
                    repo = repo,
                    matcher = matcher,
                    variables = variables,
                    params = webHookParams,
                    matchResult = matchResult
                )
                pipelineAndParamsMap[pipelineId] = params
            }
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun getNewSuccessfulPipelines(
        scmType: ScmType,
        request: WebhookRequest
    ): Map<String, Map<String, Any>> {
        val requestId = MDC.get(TraceTag.BIZID)
        val webhookData = client.get(ServiceRepositoryWebhookResource::class).webhookParse(
            scmCode = scmType.name,
            request = WebhookParseRequest(
                requestId = requestId,
                headers = request.headers,
                queryParams = request.queryParams,
                body = request.body
            )
        ).data ?: return emptyMap()
        // 填充原始事件体[第三方过滤器有用到]
        fillSourceWebhook(webhookData.webhook, request.body)
        logger.info(
            "webhook request body parsed|webhookData:${JsonUtil.toJson(webhookData.webhook, false)}"
        )
        val pipelineAndParamsMap = mutableMapOf<String, Map<String, Any>>()
        with(webhookData) {
            repositories.forEach { repository ->
                val triggerPipelines = pipelineWebhookService.listTriggerPipeline(
                    projectId = repository.projectId!!,
                    repositoryHashId = repository.repoHashId!!,
                    eventType = webhook.eventType
                )
                triggerPipelines.forEach { (projectId, pipelineId, version) ->
                    try {
                        newWebhookTrigger(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = version,
                            repository = repository,
                            webhook = webhook,
                            pipelineAndParamsMap = pipelineAndParamsMap
                        )
                    } catch (ignored: Exception) {
                        logger.warn(
                            "Failed to get new successful pipeline|projectId: $projectId|pipelineId: $pipelineId",
                            ignored
                        )
                    }
                }
            }
        }
        return pipelineAndParamsMap
    }

    private fun newWebhookTrigger(
        projectId: String,
        pipelineId: String,
        version: Int?,
        repository: Repository,
        webhook: Webhook,
        pipelineAndParamsMap: MutableMap<String, Map<String, Any>>
    ) {
        // 流水线开启PAC,并且代码库开启PAC,在pac监听器处理
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            if (it.repoHashId == repository.repoHashId) {
                return
            }
        }
        pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val resource =
            pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, version) ?: return
        val model = resource.model

        val variables = mutableMapOf<String, String>()
        val container = model.stages[0].containers[0] as TriggerContainer
        // 解析变量
        container.params.forEach { param ->
            variables[param.id] = param.defaultValue.toString()
        }
        // 填充[variables.]前缀
        variables.putAll(PipelineVarUtil.fillVariableMap(variables))
        if (repository.enablePac == true) {
            variables[PIPELINE_PAC_REPO_HASH_ID] = repository.repoHashId!!
        }
        container.elements.filterIsInstance<WebHookTriggerElement>().forEach elements@{ element ->
            if (!element.elementEnabled()) {
                return@elements
            }
            val atomResponse = webhookTriggerMatcher.matches(
                projectId = projectId,
                pipelineId = pipelineId,
                repository = repository,
                webhook = webhook,
                variables = variables,
                element = element
            )
            logger.info(
                "new webhook trigger|pipelineId:$pipelineId|element:${element.id}|" +
                        "matchResult:${atomResponse.matchStatus}"
            )
            if (atomResponse.matchStatus == MatchStatus.SUCCESS) {
                pipelineAndParamsMap[pipelineId] = atomResponse.outputVars
            }
        }
    }

    private fun fillSourceWebhook(
        webhook: Webhook,
        sourceWebhook: String
    ) {
        when (webhook) {
            is GitPushHook -> {
                webhook.extras[BK_REPO_SOURCE_WEBHOOK] = sourceWebhook
            }

            is PullRequestHook -> {
                webhook.extras[BK_REPO_SOURCE_WEBHOOK] = sourceWebhook
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookGrayCompareService::class.java)
        // 忽略的参数名
        private val IGNORED_PARAM_KEYS = listOf(
            BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME,
            BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP,
            BK_REPO_GIT_MANUAL_UNLOCK
        )
        // 忽略的前缀参数
        private val IGNORED_PARAM_PREFIX= listOf(
            BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
        )
    }
}
