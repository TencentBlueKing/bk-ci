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
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import org.springframework.stereotype.Service

@Service
class GithubWebhookElementParams : ScmWebhookElementParams<CodeGithubWebHookTriggerElement> {

    override fun elementClass(): Class<CodeGithubWebHookTriggerElement> {
        return CodeGithubWebHookTriggerElement::class.java
    }

    override fun getWebhookElementParams(
        element: CodeGithubWebHookTriggerElement,
        variables: Map<String, String>
    ): WebHookParams? {
        val params = WebHookParams(
            repositoryConfig = RepositoryConfigUtils.buildWebhookConfig(
                element = element,
                variables = variables
            ).third
        )
        with(element) {
            params.excludeUsers = if (excludeUsers == null || excludeUsers!!.isEmpty()) {
                ""
            } else {
                EnvUtils.parseEnv(excludeUsers!!, variables)
            }
            params.branchName = if (element.branchName.isNullOrEmpty()) {
                ""
            } else {
                EnvUtils.parseEnv(element.branchName!!, variables)
            }
            params.eventType = element.eventType
            params.excludeBranchName = EnvUtils.parseEnv(element.excludeBranchName ?: "", variables)
            params.codeType = CodeType.GITHUB
            params.includeCrState = joinToString(includeCrState)
            params.includeNoteComment = includeNoteComment
            params.includeNoteTypes = joinToString(includeNoteTypes)
            params.includeIssueAction = joinToString(includeIssueAction)
            params.includeMrAction = joinToString(includeMrAction)
        }
        return params
    }

    fun joinToString(list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            ""
        } else {
            list.joinToString(",")
        }
    }
}
