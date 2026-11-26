/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommitCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssue
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssueCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestUrl
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewCommentEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.handler.github.comment.GithubCommentTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.github.comment.GithubCommitCommentTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.github.comment.GithubIssueCommentTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.github.comment.GithubReviewCommentTriggerHandler
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class GithubWebhookVariableParamsTest {

    private val pullRequestEvent = ClassPathResource(
        "com/tencent/devops/common/webhook/service/code/github/GithubPullRequestOpenedEvent.json"
    ).inputStream.bufferedReader().use { JsonUtil.to(it.readText(), GithubPullRequestEvent::class.java) }

    @Test
    fun shouldUseRepositoryIdForAllCommentTypes() {
        val payload = commentPayload()
        val issue = JsonUtil.to(JsonUtil.toJson(pullRequestEvent.pullRequest), GithubIssue::class.java)
        payload["issue"] = issue
        val issueEvent = JsonUtil.to(JsonUtil.toJson(payload), GithubIssueCommentEvent::class.java)
        val pr = pullRequestEvent.pullRequest
        val prComment = issueEvent.copy(
            issue = issue.copy(
                pullRequest = GithubPullRequestUrl(
                    url = pr.url,
                    htmlUrl = pr.htmlUrl!!,
                    diffUrl = pr.diffUrl,
                    patchUrl = pr.patchUrl
                )
            )
        )

        assertCommentProjectId(GithubIssueCommentTriggerHandler(), issueEvent)
        assertCommentProjectId(GithubIssueCommentTriggerHandler(), prComment)
        assertCommentProjectId(
            GithubCommitCommentTriggerHandler(),
            JsonUtil.to(JsonUtil.toJson(payload), GithubCommitCommentEvent::class.java)
        )
        assertCommentProjectId(
            GithubReviewCommentTriggerHandler(),
            JsonUtil.to(JsonUtil.toJson(payload), GithubReviewCommentEvent::class.java)
        )
    }

    @Test
    fun shouldUseCommitShaInsteadOfCommentId() {
        val event = JsonUtil.to(JsonUtil.toJson(commentPayload()), GithubCommitCommentEvent::class.java)
        val params = GithubCommitCommentTriggerHandler().retrieveParams(event, "game_ci", null)

        assertEquals(event.comment.commitId, params[PIPELINE_GIT_SHA])
        assertEquals(event.comment.commitId.take(8), params[PIPELINE_GIT_SHA_SHORT])
        assertEquals(700L, params[BK_REPO_GIT_WEBHOOK_NOTE_ID])
    }

    @Test
    fun shouldUsePullRequestWebUrlInFinalParams() {
        val handler = GithubPrTriggerHandler(mockk<EventCacheService>())
        val params = handler.retrieveParams(pullRequestEvent, null, null)

        assertEquals(pullRequestEvent.pullRequest.htmlUrl, params[PIPELINE_GIT_MR_URL])
    }

    @Test
    fun shouldBuildPullRequestWebUrlWhenHtmlUrlIsBlank() {
        val pr = pullRequestEvent.pullRequest
        val event = pullRequestEvent.copy(pullRequest = pr.copy(htmlUrl = ""))
        val params = GithubPrTriggerHandler(mockk<EventCacheService>()).retrieveParams(event, null, null)

        assertEquals("${pr.base.repo.htmlUrl}/pull/${pr.number}", params[PIPELINE_GIT_MR_URL])
    }

    @Test
    fun shouldUsePullRequestWebUrlForReviewParams() {
        val payload = commentPayload()
        payload["review"] = mapOf(
            "id" to 800L,
            "node_id" to "review-800",
            "user" to pullRequestEvent.sender,
            "body" to "review body",
            "commit_id" to pullRequestEvent.pullRequest.head.sha,
            "submitted_at" to "2026-11-24T00:00:00Z",
            "state" to "approved",
            "pull_request_url" to pullRequestEvent.pullRequest.url,
            "author_association" to "MEMBER"
        )
        val event = JsonUtil.to(JsonUtil.toJson(payload), GithubReviewEvent::class.java)
        val params = GithubReviewTriggerHandler(mockk<EventCacheService>()).retrieveParams(event, null, null)

        assertEquals(pullRequestEvent.pullRequest.htmlUrl, params[PIPELINE_GIT_MR_URL])
    }

    private fun <T : GithubCommentEvent> assertCommentProjectId(handler: GithubCommentTriggerHandler<T>, event: T) {
        for (bkCiProjectId in listOf("game_ci", null)) {
            val params = handler.retrieveParams(event, bkCiProjectId, null)

            assertEquals(event.repository.id.toString(), params[BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID])
            assertEquals(700L, params[BK_REPO_GIT_WEBHOOK_NOTE_ID])
        }
    }

    private fun commentPayload(): MutableMap<String, Any> = mutableMapOf(
        "action" to "created",
        "repository" to pullRequestEvent.repository,
        "sender" to pullRequestEvent.sender,
        "pull_request" to pullRequestEvent.pullRequest,
        "comment" to mapOf(
            "id" to 700L,
            "node_id" to "comment-700",
            "body" to "test comment",
            "user" to pullRequestEvent.sender,
            "url" to "https://api.github.com/repos/blueking/bk-ci/comments/700",
            "html_url" to "https://github.com/blueking/bk-ci/commit/01234567#commitcomment-700",
            "created_at" to "2026-11-24T00:00:00Z",
            "updated_at" to "2026-11-24T00:00:00Z",
            "commit_id" to "0123456789abcdef0123456789abcdef01234567",
            "issue_url" to "https://api.github.com/repos/blueking/bk-ci/issues/1",
            "pull_request_review_id" to 800L,
            "path" to "README.md"
        )
    )
}
