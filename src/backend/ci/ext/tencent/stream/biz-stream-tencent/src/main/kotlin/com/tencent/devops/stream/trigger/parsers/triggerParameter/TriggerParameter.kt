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

package com.tencent.devops.stream.trigger.parsers.triggerParameter

import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteBranch
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteTag
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.GitRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TriggerParameter @Autowired constructor(
    private val gitRequestEventHandle: GitRequestEventHandle
) {

    private val logger = LoggerFactory.getLogger(TriggerParameter::class.java)

    fun getGitRequestEvent(event: GitEvent, e: String): GitRequestEvent? {
        when (event) {
            is GitPushEvent -> {
                if (!pushEventFilter(event)) {
                    return null
                }
                return gitRequestEventHandle.createPushEvent(event, e)
            }
            is GitTagPushEvent -> {
                if (!tagPushEventFilter(event)) {
                    return null
                }
                return gitRequestEventHandle.createTagPushEvent(event, e)
            }
            is GitMergeRequestEvent -> {
                // 目前不支持Mr信息更新的触发
                if (event.object_attributes.action == "update" &&
                    event.object_attributes.extension_action != "push-update"
                ) {
                    logger.info("Git web hook is ${event.object_attributes.action} merge request")
                    return null
                }

                return gitRequestEventHandle.createMergeEvent(event, e)
            }
            is GitIssueEvent -> {
                return gitRequestEventHandle.createIssueEvent(event, e)
            }
            is GitReviewEvent -> {
                return gitRequestEventHandle.createReviewEvent(event, e)
            }
            is GitNoteEvent -> {
                return gitRequestEventHandle.createNoteEvent(event, e)
            }
        }
        logger.info("event invalid: $event")
        return null
    }

    @SuppressWarnings("ReturnCount")
    private fun pushEventFilter(event: GitPushEvent): Boolean {
        // 放开删除分支操作为了流水线删除功能
        if (event.isDeleteBranch()) {
            return true
        }
        if (event.total_commits_count <= 0) {
            logger.info("${event.checkout_sha} Git push web hook no commit(${event.total_commits_count})")
            return false
        }
        if (GitUtils.isPrePushBranch(event.ref)) {
            logger.info("Git web hook is pre-push event|branchName=${event.ref}")
            return false
        }
        return true
    }

    private fun tagPushEventFilter(event: GitTagPushEvent): Boolean {
        if (event.isDeleteTag()) {
            return true
        }
        if (event.total_commits_count <= 0) {
            logger.info("Git tag web hook no commit(${event.total_commits_count})")
            return false
        }
        return true
    }
}
