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

import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.github.GithubBaseInfo
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommitCommentEvent

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubCommitCommentTriggerHandler : GithubCommentTriggerHandler<GithubCommitCommentEvent> {
    companion object {
        const val SHORT_COMMIT_SHA_LENGTH = 8
    }

    override fun eventClass(): Class<GithubCommitCommentEvent> {
        return GithubCommitCommentEvent::class.java
    }

    override fun getCommentParam(event: GithubCommitCommentEvent): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(event.comment) {
            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = user.login
            startParams[PIPELINE_GIT_SHA] = id
            startParams[PIPELINE_GIT_SHA_SHORT] = commitId.substring(0, SHORT_COMMIT_SHA_LENGTH)
        }
        return startParams
    }

    override fun buildCommentUrl(event: GithubCommitCommentEvent): String {
        // https://github.com/TencentBlueKing/bk-ci/commit/{{commitSha}}#commitcomment-{{commentId}}
        return with(event) {
            if (comment.htmlUrl.isNullOrBlank()) {
                "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}/commit/" +
                        "${comment.commitId}#commitcomment-${comment.id}"
            } else {
                comment.htmlUrl!!
            }
        }
    }
}
