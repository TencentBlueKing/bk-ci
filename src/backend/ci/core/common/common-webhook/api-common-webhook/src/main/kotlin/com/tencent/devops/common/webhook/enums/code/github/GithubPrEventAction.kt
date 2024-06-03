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

package com.tencent.devops.common.webhook.enums.code.github

import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent

/*
 * Stream Mr 事件支持动作
 * 根据Webhook事件抽象Stream action
 */

enum class GithubPrEventAction(val value: String) {
    OPEN("opened"),
    CLOSE("closed"),
    REOPEN("reopened"),
    PUSH_UPDATE("synchronize"),
    MERGE("also closed"),
    EDITED("edited"), // 修改mr信息
    /**
     * assigned/auto_merge_disabled/auto_merge_enabled/
     * converted_to_draft/edited/labeled/locked/ready_for_review/
     * review_request_removed/review_requested/unassigned/unlabeled/unlocked
     */
    STREAM_NOT_SUPPORT("other action");

    companion object {
        fun get(eventAction: GithubPullRequestEvent): GithubPrEventAction {
            if (eventAction.pullRequest.merged && eventAction.action == CLOSE.value) return MERGE
            values().forEach {
                if (eventAction.action == it.value) return it
            }
            return STREAM_NOT_SUPPORT
        }
    }
}
