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
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.github.GithubBaseInfo
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssueCommentEvent

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubIssueCommentTriggerHandler : GithubCommentTriggerHandler<GithubIssueCommentEvent> {

    override fun eventClass(): Class<GithubIssueCommentEvent> {
        return GithubIssueCommentEvent::class.java
    }

    override fun getCommentParam(event: GithubIssueCommentEvent): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(event.issue) {
            // 注：直接在Github PR下进行的评论，Github的Webhook消息头为[X-GitHub-Event=issue_comment]
            // pullRequest存在则代表直接在Github PR下进行的评论，填充Github PR相关参数，反之填充Issue相关参数
            if (pullRequest != null) {
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = pullRequest!!.htmlUrl
                startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = title
                startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = id
                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = number
                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = body ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = milestone?.title ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_ID] = milestone?.id ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = user.login
                // 同步Code_Git,基于MR直接评论，noteable_type=Review
                assignees?.run {
                    startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = this.joinToString(",") { it.login }
                }
            } else {
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_TITLE] = title
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ID] = id
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_IID] = number
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION] = body ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_STATE] = state
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_OWNER] = user.login
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_URL] = htmlUrl ?: ""
                startParams[BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID] = milestone?.id ?: ""
            }
        }
        return startParams
    }

    override fun buildCommentUrl(event: GithubIssueCommentEvent): String {
        // https://github.com/TencentBlueKing/bk-ci/issues/{{issuesNumber}}#issuecomment-{{commentId}}
        return with(event) {
            if (comment.htmlUrl.isNullOrBlank()) {
                "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}/commit/" +
                        "${issue.number}#issuecomment-${comment.id}"
            } else {
                comment.htmlUrl!!
            }
        }
    }
}
