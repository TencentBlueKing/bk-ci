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

package com.tencent.devops.stream.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitTagPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.pojo.GitRequestEvent

object StreamTriggerMessageUtils {
    @SuppressWarnings("ComplexMethod")
    fun getEventMessageTitle(event: GitRequestEvent, checkRepoHookTrigger: Boolean = false): String {
        val messageTitle = when (event.objectKind) {
            StreamObjectKind.MERGE_REQUEST.value -> {
                "Merge requests [!${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamObjectKind.PULL_REQUEST.value -> {
                "Pull requests [!${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamObjectKind.MANUAL.value -> {
                "Manually run by ${event.userId}"
            }
            StreamObjectKind.OPENAPI.value -> {
                "Run by OPENAPI(${event.userId})"
            }
            StreamObjectKind.TAG_PUSH.value -> {
                if (event.operationKind == TGitTagPushOperationKind.DELETE.value) {
                    "Tag [${event.branch}] deleted by ${event.userId}"
                } else {
                    "Tag [${event.branch}] pushed by ${event.userId}"
                }
            }
            StreamObjectKind.SCHEDULE.value -> {
                "Scheduled"
            }
            StreamObjectKind.PUSH.value -> {
                if (event.operationKind == TGitPushOperationKind.DELETE.value) {
                    "Branch [${event.branch}] deleted by ${event.userId}"
                } else {
                    val eventMap = try {
                        JsonUtil.to(event.event, object : TypeReference<GitPushEvent>() {})
                    } catch (e: Exception) {
                        null
                    }
                    if (eventMap?.create_and_update != null) {
                        "Branch [${event.branch}] added by ${event.userId}"
                    } else {
                        "Commit [${event.commitId.subSequence(0, 8)}] pushed by ${event.userId}"
                    }
                }
            }
            StreamObjectKind.ISSUE.value -> {
                "Issue [${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamObjectKind.REVIEW.value -> {
                "Review [${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamObjectKind.NOTE.value -> {
                val noteEvent = try {
                    JsonUtil.to(event.event, GitNoteEvent::class.java)
                } catch (e: Exception) {
                    null
                }
                "Note [${noteEvent?.objectAttributes?.id}] submitted by ${event.userId}"
            }
            else -> {
                "Commit [${event.commitId.subSequence(0, 8)}] pushed by ${event.userId}"
            }
        }
        return if (checkRepoHookTrigger) "[${event.branch}] " + messageTitle else messageTitle
    }
}
