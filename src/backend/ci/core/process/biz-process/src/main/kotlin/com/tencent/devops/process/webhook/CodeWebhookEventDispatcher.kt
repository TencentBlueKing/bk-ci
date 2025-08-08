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

package com.tencent.devops.process.webhook

import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ICodeWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge

/**
 * @ Date       ：Created in 10:32 2019-08-08
 */

object CodeWebhookEventDispatcher {

    fun dispatchEvent(streamBridge: StreamBridge, event: ICodeWebhookEvent): Boolean {
        logger.debug("Webhook comming [${event.commitEventType}|${event.requestContent}]")
        var result = false
        try {
            logger.info("${event.traceId}|Dispatch the ${event.commitEventType} webhook event by MQ")
            event.sendTo(streamBridge)
            result = true
        } catch (ignore: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", ignore)
        }
        return result
    }

    fun dispatchGithubEvent(streamBridge: StreamBridge, event: GithubWebhookEvent): Boolean {
        logger.debug("Webhook comming [GITHUB|${event.githubWebhook.event}]")
        var result = false
        try {
            logger.info("Dispatch the GITHUB webhook event by MQ")
            event.sendTo(streamBridge)
            result = true
        } catch (ignore: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", ignore)
        }
        return result
    }

    fun dispatchReplayEvent(streamBridge: StreamBridge, event: ReplayWebhookEvent): Boolean {
        logger.debug("Webhook comming [replay|$event]")
        var result = false
        try {
            logger.info("Dispatch the replay webhook event by MQ")
            event.sendTo(streamBridge)
            result = true
        } catch (e: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", e)
        }
        return result
    }

    private val logger = LoggerFactory.getLogger(CodeWebhookEventDispatcher::class.java)
}
