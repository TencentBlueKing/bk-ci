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

package com.tencent.devops.common.webhook.service.code.handler.github.comment

import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.github.GithubBaseInfo
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewCommentEvent

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubReviewCommentTriggerHandler : GithubCommentTriggerHandler<GithubReviewCommentEvent> {

    override fun eventClass(): Class<GithubReviewCommentEvent> {
        return GithubReviewCommentEvent::class.java
    }

    override fun getCommentParam(event: GithubReviewCommentEvent): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        // review相关信息
        with(event) {
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_STATE] = pullRequest.state
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_ID] = comment.pullRequestReviewId
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_IID] = pullRequest.number
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH] = pullRequest.head.ref
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID] = pullRequest.head.repo.id
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_COMMIT] = pullRequest.head.sha
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_COMMIT] = pullRequest.base.sha
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH] = pullRequest.base.ref
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID] = pullRequest.base.repo.id
        }
        return startParams
    }

    override fun buildCommentUrl(event: GithubReviewCommentEvent): String {
        // https://github.com/TencentBlueKing/bk-ci/pull/{{pullNumber}}#issuecomment-{{commentId}}
        return with(event) {
            if (comment.htmlUrl.isNullOrBlank()) {
                "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}/pull/" +
                        "${pullRequest.number}#discussion_r${comment.id}"
            } else {
                comment.htmlUrl!!
            }
        }
    }
}
