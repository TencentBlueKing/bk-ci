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

package com.tencent.devops.process.webhook.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCheckRunEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommitCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssueCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssuesEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewEvent
import org.slf4j.LoggerFactory

/**
 * github webhook事件解析
 */
class GithubWebhookEventParser(
    private val objectMapper: ObjectMapper
) : IWebhookEventParser {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubWebhookEventParser::class.java)
    }

    override fun parseEvent(request: WebhookRequest): CodeWebhookEvent? {
        val eventType = request.headers?.get("X-GitHub-Event")
        val guid = request.headers?.get("X-Github-Delivery")
        val signature = request.headers?.get("X-Hub-Signature")
        val body = request.body
        logger.info("Trigger code github build (event=$eventType, guid=$guid, signature=$signature, body=$body)")

        return when (eventType) {
            GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(body)
            GithubCreateEvent.classType -> objectMapper.readValue<GithubCreateEvent>(body)
            GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(body)
            GithubCheckRunEvent.classType -> objectMapper.readValue<GithubCheckRunEvent>(body)
            GithubCommitCommentEvent.classType -> objectMapper.readValue<GithubCommitCommentEvent>(body)
            GithubIssueCommentEvent.classType -> objectMapper.readValue<GithubIssueCommentEvent>(body)
            GithubReviewCommentEvent.classType -> objectMapper.readValue<GithubReviewCommentEvent>(body)
            GithubIssuesEvent.classType -> objectMapper.readValue<GithubIssuesEvent>(body)
            GithubReviewEvent.classType -> objectMapper.readValue<GithubReviewEvent>(body)
            else -> {
                logger.info("Github event($eventType) is ignored")
                return null
            }
        }
    }
}
