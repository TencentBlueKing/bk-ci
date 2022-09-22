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
 *
 */

package com.tencent.devops.stream.resources.external

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.external.ExternalGithubResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.service.StreamLoginService
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestDispatcher
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@RestResource
class ExternalGithubResourceImpl @Autowired constructor(
    private val streamLoginService: StreamLoginService,
    private val streamGitConfig: StreamGitConfig,
    private val rabbitTemplate: RabbitTemplate
) : ExternalGithubResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalGithubResourceImpl::class.java)
    }

    override fun webhookCommit(event: String, guid: String, signature: String, body: String): Result<Boolean> {
        logger.info("Github webhook [event=$event, guid=$guid, signature=$signature, body=$body]")
        try {
            val removePrefixSignature = signature.removePrefix("sha1=")
            val genSignature = ShaUtils.hmacSha1(streamGitConfig.signSecret.toByteArray(), body.toByteArray())
            logger.info("signature($removePrefixSignature) and generate signature ($genSignature)")
            if (!ShaUtils.isEqual(removePrefixSignature, genSignature)) {
                logger.warn("signature($removePrefixSignature) and generate signature ($genSignature) not match")
                return Result(false)
            }

            StreamRequestDispatcher.dispatch(
                rabbitTemplate = rabbitTemplate,
                event = StreamRequestEvent(
                    eventType = event,
                    webHookType = ScmType.GITHUB.name,
                    event = body
                )
            )
        } catch (t: Throwable) {
            logger.info("Github webhook exception", t)
        }
        return Result(true)
    }

    override fun oauthCallback(code: String, state: String?): Response {
        val redirectUrl = streamLoginService.githubCallback(code, state)
        logger.info("github oauth callback redirectUrl: $redirectUrl")
        return Response.temporaryRedirect(UriBuilder.fromUri(redirectUrl).build())
            .status(Response.Status.TEMPORARY_REDIRECT).build()
    }
}
