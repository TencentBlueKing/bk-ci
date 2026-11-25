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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.pojo.code.github.GithubAuthor
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommit
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommitter
import com.tencent.devops.common.webhook.pojo.code.github.GithubHeadCommit
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPusher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class GithubPushVariableParamsTest {

    private val handler = GithubPushTriggerHandler()
    private val pullRequestEvent = ClassPathResource(
        "com/tencent/devops/common/webhook/service/code/github/GithubPullRequestOpenedEvent.json"
    ).inputStream.bufferedReader().use { JsonUtil.to(it.readText(), GithubPullRequestEvent::class.java) }
    private val firstCommit = commit("1".repeat(40), "first message", "first-author")
    private val lastCommit = commit("2".repeat(40), "last message", "last-author")

    @Test
    fun shouldKeepPushMetadataConsistentWithAfterCommit() {
        assertHeadCommitParams(event())
    }

    @Test
    fun shouldFindAfterCommitWhenHeadCommitIsMissing() {
        assertHeadCommitParams(event(headCommit = null))
    }

    @Test
    fun shouldUseHeadCommitWhenCommitsListIsTruncated() {
        assertHeadCommitParams(event(commits = listOf(firstCommit)))
    }

    @Test
    fun shouldIgnoreHeadCommitThatDoesNotMatchAfter() {
        val head = JsonUtil.to(JsonUtil.toJson(firstCommit), GithubHeadCommit::class.java)

        assertHeadCommitParams(event(headCommit = head))
    }

    @Test
    fun shouldBuildEventWebUrlWhenRepositoryHtmlUrlIsMissing() {
        val original = event()
        val event = original.copy(repository = original.repository.copy(htmlUrl = null))
        val params = handler.retrieveParams(event, null, null)

        assertEquals(
            "https://github.com/${event.repository.fullName}/commit/${event.after}",
            params[PIPELINE_GIT_EVENT_URL]
        )
    }

    private fun assertHeadCommitParams(event: GithubPushEvent) {
        val params = handler.retrieveParams(event, null, null)

        assertEquals(lastCommit.author.name, params[PIPELINE_GIT_COMMIT_AUTHOR])
        assertEquals(lastCommit.message, params[PIPELINE_GIT_COMMIT_MESSAGE])
        assertEquals(lastCommit.id.take(8), params[PIPELINE_GIT_SHA_SHORT])
        assertEquals("${event.repository.htmlUrl}/commit/${lastCommit.id}", params[PIPELINE_GIT_EVENT_URL])
    }

    private fun event(
        commits: List<GithubCommit> = listOf(firstCommit, lastCommit),
        headCommit: GithubHeadCommit? = JsonUtil.to(JsonUtil.toJson(lastCommit), GithubHeadCommit::class.java)
    ) = GithubPushEvent(
        after = lastCommit.id,
        baseRef = null,
        before = "0".repeat(40),
        commits = commits,
        compare = "https://github.com/blueking/bk-ci/compare/first...last",
        created = false,
        deleted = false,
        forced = false,
        headCommit = headCommit,
        pusher = GithubPusher(email = "sender@example.test", name = "sender"),
        ref = "refs/heads/main",
        repository = pullRequestEvent.repository,
        sender = pullRequestEvent.sender
    )

    private fun commit(sha: String, message: String, author: String) = GithubCommit(
        added = emptyList(),
        author = GithubAuthor(email = "$author@example.test", name = author, username = author),
        committer = GithubCommitter(email = "$author@example.test", name = author, username = author),
        distinct = true,
        id = sha,
        message = message,
        modified = emptyList(),
        removed = emptyList(),
        timestamp = "2026-11-24T00:00:00Z",
        treeId = sha,
        url = "https://github.com/blueking/bk-ci/commit/$sha"
    )
}
