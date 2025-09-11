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
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.util.WebhookUtils
import org.springframework.stereotype.Service

@Service
class ScmGitWebhookElementParams : ScmWebhookElementParams<CodeScmGitWebHookTriggerElement> {

    override fun elementClass(): Class<CodeScmGitWebHookTriggerElement> {
        return CodeScmGitWebHookTriggerElement::class.java
    }

    @SuppressWarnings("ComplexMethod")
    override fun getWebhookElementParams(
        element: CodeScmGitWebHookTriggerElement,
        variables: Map<String, String>
    ): WebHookParams? {
        val params = WebHookParams(
            repositoryConfig = RepositoryConfigUtils.buildWebhookConfig(
                element = element,
                variables = variables
            ).third
        )
        with(element.data.input) {
            params.excludeUsers = if (excludeUsers.isNullOrBlank()) {
                ""
            } else {
                EnvUtils.parseEnv(excludeUsers, variables)
            }
            params.includeUsers = if (excludeUsers.isNullOrBlank()) {
                ""
            } else {
                EnvUtils.parseEnv(includeUsers, variables)
            }
            params.branchName = EnvUtils.parseEnv(branchName ?: "", variables)
            params.version = element.version
            when {
                eventType == CodeEventType.MERGE_REQUEST -> {
                    params.includeMrAction = if (actions == null) {
                        WebhookUtils.joinToString(
                            listOf(
                                CodeScmGitWebHookTriggerElement.MERGE_ACTION_OPEN,
                                CodeScmGitWebHookTriggerElement.MERGE_ACTION_REOPEN,
                                CodeScmGitWebHookTriggerElement.MERGE_ACTION_PUSH_UPDATE
                            )
                        )
                    } else {
                        WebhookUtils.joinToString(actions)
                    }
                }

                eventType == CodeEventType.PUSH -> {
                    params.includePushAction = if (actions == null) {
                        WebhookUtils.joinToString(
                            listOf(
                                CodeScmGitWebHookTriggerElement.PUSH_ACTION_CREATE_BRANCH,
                                CodeScmGitWebHookTriggerElement.PUSH_ACTION_PUSH_FILE
                            )
                        )
                    } else {
                        WebhookUtils.joinToString(actions)
                    }
                }
            }
            params.eventType = eventType
            params.excludeBranchName = EnvUtils.parseEnv(excludeBranchName ?: "", variables)
            params.pathFilterType = pathFilterType
            params.includePaths = EnvUtils.parseEnv(includePaths ?: "", variables)
            params.excludePaths = EnvUtils.parseEnv(excludePaths ?: "", variables)
            params.codeType = CodeType.SCM_GIT
            params.tagName = EnvUtils.parseEnv(tagName ?: "", variables)
            params.excludeTagName = EnvUtils.parseEnv(excludeTagName ?: "", variables)
            params.excludeSourceBranchName = EnvUtils.parseEnv(excludeSourceBranchName ?: "", variables)
            params.includeSourceBranchName = EnvUtils.parseEnv(includeSourceBranchName ?: "", variables)
        }
        return params
    }
}
