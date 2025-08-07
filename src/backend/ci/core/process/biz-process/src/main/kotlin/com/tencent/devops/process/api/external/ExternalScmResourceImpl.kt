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

package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.P4WebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.TGitWebhookEvent
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.stream.function.StreamBridge

@RestResource
class ExternalScmResourceImpl @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val simpleDispatcher: SampleEventDispatcher
) : ExternalScmResource {

    @Value("\${scm.external.tGit.hookSecret:}")
    private val tGitWebhookSecret: String = ""

    @Value("\${scm.external.tGit.enableHookSecret}")
    private val enableTGitWebhookSecret: Boolean = false

    override fun webHookCodeSvnCommit(event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(streamBridge, SvnWebhookEvent(requestContent = event)))

    override fun webHookCodeGitCommit(
        event: String,
        secret: String?,
        sourceType: String?,
        traceId: String,
        body: String
    ): Result<Boolean> {
        // 工蜂的测试请求,应该忽略
        if (sourceType == "Test") {
            return Result(true)
        }
        CodeWebhookEventDispatcher.dispatchEvent(
            streamBridge = streamBridge,
            event = GitWebhookEvent(
                requestContent = body,
                event = event,
                secret = secret,
                traceId = traceId
            )
        )
        return Result(true)
    }

    override fun webHookGitlabCommit(event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(streamBridge, GitlabWebhookEvent(requestContent = event)))

    override fun webHookCodeTGitCommit(
        event: String,
        secret: String?,
        traceId: String,
        body: String
    ): Result<Boolean> {
        logger.info("tgit webhook secret|$secret")
        if (enableTGitWebhookSecret) {
            if (secret.isNullOrBlank()) {
                logger.warn("the secret of tgit webhook can not be empty")
                throw ParamBlankException("secret empty")
            }
            if (secret != tGitWebhookSecret) {
                logger.warn("the secret of tgit webhook is illegal")
                throw InvalidParamException(message = "secret illegal", params = arrayOf("secret"))
            }
        }
        return Result(
            CodeWebhookEventDispatcher.dispatchEvent(
                streamBridge = streamBridge,
                event = TGitWebhookEvent(
                    requestContent = body,
                    event = event,
                    secret = secret
                )
            )
        )
    }

    override fun webHookCodeP4Commit(body: String): Result<Boolean> {
        logger.info("p4 webhook|$body")
        return Result(
            CodeWebhookEventDispatcher.dispatchEvent(
                streamBridge = streamBridge,
                event = P4WebhookEvent(
                    requestContent = body
                )
            )
        )
    }

    override fun webHookScmCommit(scmCode: String, request: HttpServletRequest, body: String): Result<Boolean> {
        logger.info("scm webhook|$body")
        simpleDispatcher.dispatch(
            ScmWebhookRequestEvent(
                scmCode = scmCode,
                request = WebhookRequest(
                    headers = scmWebhookHeader(request),
                    queryParams = parseQueryString(request.queryString ?: ""),
                    body = body
                )
            )
        )
        return Result(true)
    }

    private fun scmWebhookHeader(request: HttpServletRequest) = request.headerNames.toList()
            .associateWith { request.getHeader(it) }

    private fun parseQueryString(queryString: String): Map<String, String> {
        return queryString.split("&")
                .associate {
                    val pair = it.split("=", limit = 2)
                    val key = pair[0]
                    val value = if (pair.size > 1) pair[1] else ""
                    key to value
                }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalScmResourceImpl::class.java)
    }
}
