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
 *
 */

package com.tencent.devops.process.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.common.webhook.pojo.code.p4.P4Event
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.engine.service.code.ScmWebhookMatcherBuilder
import com.tencent.devops.process.webhook.parser.GithubWebhookEventParser
import com.tencent.devops.process.webhook.parser.GitlabWebhookEventParser
import com.tencent.devops.process.webhook.parser.P4WebhookEventParser
import com.tencent.devops.process.webhook.parser.SvnWebhookEventParser
import com.tencent.devops.process.webhook.parser.TGitWebhookEventParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebhookEventFactory @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val scmWebhookMatcherBuilder: ScmWebhookMatcherBuilder
) {

    fun parseEvent(scmType: ScmType, request: WebhookRequest): CodeWebhookEvent? {
        val webhookEventParser = when (scmType) {
            ScmType.CODE_GIT, ScmType.CODE_TGIT ->
                TGitWebhookEventParser(objectMapper = objectMapper)

            ScmType.CODE_SVN ->
                SvnWebhookEventParser(objectMapper = objectMapper)

            ScmType.CODE_P4 ->
                P4WebhookEventParser(objectMapper = objectMapper)

            ScmType.CODE_GITLAB ->
                GitlabWebhookEventParser(objectMapper = objectMapper)

            ScmType.GITHUB ->
                GithubWebhookEventParser(objectMapper = objectMapper)

            else ->
                throw InvalidParamException("Unknown scm type($scmType)")
        }
        return webhookEventParser.parseEvent(request = request)
    }

    fun createScmWebHookMatcher(scmType: ScmType, event: CodeWebhookEvent): ScmWebhookMatcher {
        return when (scmType) {
            ScmType.CODE_GIT, ScmType.CODE_TGIT ->
                scmWebhookMatcherBuilder.createGitWebHookMatcher(event = event as GitEvent)

            ScmType.CODE_SVN ->
                scmWebhookMatcherBuilder.createSvnWebHookMatcher(event = event as SvnCommitEvent)

            ScmType.CODE_P4 ->
                scmWebhookMatcherBuilder.createP4WebHookMatcher(event = event as P4Event)

            ScmType.CODE_GITLAB ->
                scmWebhookMatcherBuilder.createGitlabWebHookMatcher(event = event as GitEvent)

            ScmType.GITHUB ->
                scmWebhookMatcherBuilder.createGithubWebHookMatcher(event = event as GithubEvent)

            else ->
                throw InvalidParamException("Unknown scm type($scmType)")
        }
    }
}
