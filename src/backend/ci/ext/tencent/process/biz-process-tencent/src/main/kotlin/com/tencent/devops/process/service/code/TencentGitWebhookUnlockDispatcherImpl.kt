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

package com.tencent.devops.process.service.code

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_MANUAL_UNLOCK
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.plugin.api.pojo.GitWebhookUnlockEvent
import com.tencent.devops.process.engine.service.code.GitWebhookUnlockDispatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentGitWebhookUnlockDispatcherImpl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : GitWebhookUnlockDispatcher {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentGitWebhookUnlockDispatcherImpl::class.java)
    }

    override fun dispatchUnlockHookLockEvent(matcher: ScmWebhookMatcher) {
        val manualUnlock = (matcher.getEnv()[BK_REPO_GIT_MANUAL_UNLOCK] as Boolean?) ?: false
        val canDispatch = canDispatch(matcher)
        logger.info(
            "dispatch unlock hooklock event, " +
                "repoName:${matcher.getRepoName()}, mrId:${matcher.getMergeRequestId()}, " +
                "manualUnlock:$manualUnlock, canDispatch:$canDispatch"
        )
        if (matcher.getMergeRequestId() != null && manualUnlock && canDispatch) {
            pipelineEventDispatcher.dispatch(
                GitWebhookUnlockEvent(
                    repoName = matcher.getRepoName(),
                    mrId = matcher.getMergeRequestId()!!
                )
            )
        }
    }

    /**
     * 1. 没有流水线触发,需要解锁
     * 2. 有流水线触发,但是所有的流水线都不需要锁住mr,需要解锁
     */
    private fun canDispatch(matcher: ScmWebhookMatcher): Boolean {
        val webHookParamsMap = matcher.getWebHookParamsMap()
        if (webHookParamsMap.isEmpty()) {
            return true
        }
        webHookParamsMap.values.forEach { params ->
            if (params.block) {
                return false
            }
        }
        return true
    }
}
