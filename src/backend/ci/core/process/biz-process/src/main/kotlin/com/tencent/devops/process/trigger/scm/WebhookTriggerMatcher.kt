/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SOURCE_WEBHOOK
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.trigger.scm.rule.WebhookRuleManager
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * webhook触发规则
 */
@Service
class WebhookTriggerMatcher @Autowired constructor(
    private val webhookRuleManager: WebhookRuleManager
) {

    fun matches(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webhook: Webhook,
        variables: Map<String, String>,
        element: WebHookTriggerElement
    ): WebhookAtomResponse {
        // 获取流水线webhook触发配置参数
        val webHookParams = WebhookElementParamsRegistrar.getService(element).getWebhookElementParams(
            element = element,
            variables = variables
        ) ?: return WebhookAtomResponse(
            matchStatus = MatchStatus.ELEMENT_NOT_MATCH
        )
        val matchResult = with(webHookParams) {
            val repositoryMatch = when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> repositoryConfig.getRepositoryId() == repository.repoHashId!!
                RepositoryType.NAME -> repositoryConfig.getRepositoryId() == repository.aliasName
            }
            if (!repositoryMatch) {
                return WebhookAtomResponse(
                    matchStatus = MatchStatus.REPOSITORY_NOT_MATCH
                )
            }
            // 兼容V1版本触发器
            val eventType = eventType?.let {
                if (it == CodeEventType.MERGE_REQUEST_ACCEPT) CodeEventType.MERGE_REQUEST else it
            }?.name
            if (eventType != webhook.eventType) {
                return WebhookAtomResponse(
                    matchStatus = MatchStatus.EVENT_TYPE_NOT_MATCH
                )
            }
            // 保存代码库关联时的url
            sourceRepoUrl = repository.url
            webhookRuleManager.evaluate(
                projectId = projectId,
                pipelineId = pipelineId,
                webHookParams = webHookParams,
                webhook = webhook
            )
        }
        return if (matchResult.isMatch) {
            val startParams = mutableMapOf<String, Any>()
            startParams.putAll(variables)
            startParams.putAll(matchResult.extra)
            webhookOutputs(
                startParams = startParams,
                webhook = webhook
            )
            elementOutputs(
                startParams = startParams,
                element = element,
                repository = repository,
                variables = variables,
                webhookParams = webHookParams,
                matchResult = matchResult
            )
            WebhookAtomResponse(
                matchStatus = MatchStatus.SUCCESS,
                outputVars = startParams
            )
        } else {
            WebhookAtomResponse(
                matchStatus = MatchStatus.CONDITION_NOT_MATCH,
                failedReason = matchResult.reason
            )
        }
    }

    private fun elementOutputs(
        startParams: MutableMap<String, Any>,
        element: WebHookTriggerElement,
        repository: Repository,
        variables: Map<String, String>,
        webhookParams: WebHookParams,
        matchResult: WebhookMatchResult
    ) {
        // 公共触发参数
        startParams[PIPELINE_START_TASK_ID] = element.id!! // 当前触发节点为启动节点
        startParams[PIPELINE_WEBHOOK_REPO] = webhookParams.repositoryConfig.getRepositoryId()
        startParams[PIPELINE_WEBHOOK_REPO_TYPE] = webhookParams.repositoryConfig.repositoryType.name
        startParams[PIPELINE_WEBHOOK_BLOCK] = webhookParams.block
        startParams[PIPELINE_WEBHOOK_QUEUE] = webhookParams.webhookQueue
        startParams[BK_REPO_WEBHOOK_REPO_TYPE] = webhookParams.codeType.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = repository.url
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = repository.projectName
        startParams[BK_REPO_WEBHOOK_REPO_ALIAS_NAME] = repository.aliasName
        startParams[BK_REPO_WEBHOOK_HASH_ID] = repository.repoHashId ?: ""
        startParams[PIPELINE_WEBHOOK_QUEUE] = webhookParams.webhookQueue
        startParams[PIPELINE_WEBHOOK_COMMIT_MESSAGE]?.let {
            it as String
            if (it.length >= PIPELINE_WEBHOOK_COMMIT_MESSAGE_LENGTH_MAX) {
                startParams[PIPELINE_WEBHOOK_COMMIT_MESSAGE] = it.substring(
                    0,
                    PIPELINE_WEBHOOK_COMMIT_MESSAGE_LENGTH_MAX
                )
            }
        }
        startParams[PIPELINE_BUILD_MSG] = startParams[PIPELINE_WEBHOOK_COMMIT_MESSAGE] as String?
            ?: I18nUtil.getCodeLanMessage(CommonMessageCode.BK_CODE_BASE_TRIGGERING)
        // 构建历史界面——触发logo展示
        startParams[PIPELINE_WEBHOOK_TYPE] = webhookParams.codeType.name
        // 子类代码库触发参数
        val elementStartParams = WebhookStartParamsRegistrar.getService(element).getElementStartParams(
            element = element,
            variables = variables,
            repo = repository,
            matcher = null,
            matchResult = matchResult,
            params = webhookParams,
            projectId = repository.projectId ?: ""
        ).mapValues { it.value.toString() }
        startParams.putAll(elementStartParams)
    }

    private fun webhookOutputs(
        startParams: MutableMap<String, Any>,
        webhook: Webhook
    ) {
        val webhookOutputs = webhook.outputs().filter { it.key != BK_REPO_SOURCE_WEBHOOK }
        startParams.putAll(webhookOutputs)
    }

    companion object {
        const val PIPELINE_WEBHOOK_COMMIT_MESSAGE_LENGTH_MAX = 128
    }
}

data class WebhookAtomResponse(
    val matchStatus: MatchStatus,
    val outputVars: Map<String, Any> = emptyMap(),
    val failedReason: String? = null
)

enum class MatchStatus {
    // 匹配成功
    SUCCESS,

    // 插件不匹配
    ELEMENT_NOT_MATCH,

    // 代码库不匹配
    REPOSITORY_NOT_MATCH,

    // 事件类型不匹配
    EVENT_TYPE_NOT_MATCH,

    // 条件不匹配
    CONDITION_NOT_MATCH;
}
