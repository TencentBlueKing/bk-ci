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

package com.tencent.devops.common.webhook.service.code.param

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.util.WebhookUtils
import org.springframework.stereotype.Service

@Service
class TGitWebhookElementParams : ScmWebhookElementParams<CodeTGitWebHookTriggerElement> {

    override fun elementClass(): Class<CodeTGitWebHookTriggerElement> {
        return CodeTGitWebHookTriggerElement::class.java
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    override fun getWebhookElementParams(
        element: CodeTGitWebHookTriggerElement,
        variables: Map<String, String>
    ): WebHookParams? {
        val params = WebHookParams(
            repositoryConfig = RepositoryConfigUtils.buildWebhookConfig(
                element = element,
                variables = variables
            ).third
        )
        with(element.data.input) {
            params.excludeUsers = if (excludeUsers == null || excludeUsers!!.isEmpty()) {
                ""
            } else {
                EnvUtils.parseEnv(excludeUsers!!.joinToString(","), variables)
            }
            params.includeUsers = if (includeUsers == null || includeUsers!!.isEmpty()) {
                ""
            } else {
                EnvUtils.parseEnv(includeUsers!!.joinToString(","), variables)
            }
            params.block = isBlock(element)
            params.branchName = if (!branchName.isNullOrBlank()) {
                EnvUtils.parseEnv(branchName!!, variables)
            } else {
                ""
            }
            params.version = element.version
            when {
                // action上线后【流水线配置层面】兼容存量merge_request_accept和push事件
                eventType == CodeEventType.MERGE_REQUEST_ACCEPT -> {
                    params.includeMrAction = CodeGitWebHookTriggerElement.MERGE_ACTION_MERGE
                }

                eventType == CodeEventType.MERGE_REQUEST &&
                        !WebhookUtils.isActionGitTriggerVersion(element.version) &&
                        includeMrAction == null -> {
                    params.includeMrAction = joinToString(
                        listOf(
                            CodeGitWebHookTriggerElement.MERGE_ACTION_OPEN,
                            CodeGitWebHookTriggerElement.MERGE_ACTION_REOPEN,
                            CodeGitWebHookTriggerElement.MERGE_ACTION_PUSH_UPDATE
                        )
                    )
                }

                eventType == CodeEventType.PUSH &&
                        !WebhookUtils.isActionGitTriggerVersion(element.version) &&
                        includePushAction == null -> {
                    params.includePushAction = joinToString(
                        listOf(
                            CodeGitWebHookTriggerElement.PUSH_ACTION_CREATE_BRANCH,
                            CodeGitWebHookTriggerElement.PUSH_ACTION_PUSH_FILE
                        )
                    )
                }

                else -> {
                    params.includeMrAction = joinToString(includeMrAction)
                    params.includePushAction = joinToString(includePushAction)
                }
            }
            params.eventType = eventType
            params.excludeBranchName = EnvUtils.parseEnv(excludeBranchName ?: "", variables)
            params.pathFilterType = pathFilterType
            params.includePaths = EnvUtils.parseEnv(includePaths ?: "", variables)
            params.excludePaths = EnvUtils.parseEnv(excludePaths ?: "", variables)
            params.codeType = CodeType.GIT
            params.tagName = EnvUtils.parseEnv(tagName ?: "", variables)
            params.excludeTagName = EnvUtils.parseEnv(excludeTagName ?: "", variables)
            params.excludeSourceBranchName = EnvUtils.parseEnv(excludeSourceBranchName ?: "", variables)
            params.includeSourceBranchName = EnvUtils.parseEnv(includeSourceBranchName ?: "", variables)
            params.includeCrState = if (includeCrState.isNullOrEmpty()) {
                ""
            } else {
                includeCrState!!.joinToString(",")
            }
            params.fromBranches = EnvUtils.parseEnv(fromBranches ?: "", variables)
            params.webhookQueue = webhookQueue ?: false
            params.includeIssueAction = joinToString(includeIssueAction)
            params.includeNoteComment = includeNoteComment
            params.includeNoteTypes = joinToString(includeNoteTypes)
            return params
        }
    }

    private fun joinToString(list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            ""
        } else {
            list.joinToString(",")
        }
    }

    private fun isBlock(element: CodeTGitWebHookTriggerElement): Boolean {
        return with(element.data.input) {
            when {
                enableCheck == false || eventType != CodeEventType.MERGE_REQUEST -> false
                else -> block ?: false
            }
        }
    }
}
