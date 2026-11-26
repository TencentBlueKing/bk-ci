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

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubRepository
import com.tencent.devops.common.webhook.pojo.code.github.GithubUser
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.param.GithubWebhookElementParams
import com.tencent.devops.repository.pojo.Repository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GithubPushAndCreateTriggerHandlerTest {

    private val githubRepository = mockk<GithubRepository>(relaxed = true) {
        every { sshUrl } returns "git@github.com:blueking/bk-ci.git"
    }
    private val pipelineRepository = mockk<Repository> {
        every { url } returns "https://github.com/blueking/bk-ci.git"
    }
    private val sender = GithubUser(
        gravatarId = "",
        htmlUrl = "https://github.com/alice",
        id = 1L,
        login = "alice",
        nodeId = "user-1",
        siteAdmin = false,
        type = "User",
        url = "https://api.github.com/users/alice",
        createdAt = null,
        updatedAt = null
    )

    @Test
    fun shouldFilterPushOperatorsAfterParameterConversion() {
        val event = GithubPushEvent(
            after = "after-commit",
            baseRef = null,
            before = "before-commit",
            commits = listOf(mockk(relaxed = true)),
            compare = "https://github.com/blueking/bk-ci/compare/before...after",
            created = false,
            deleted = false,
            forced = false,
            headCommit = null,
            pusher = mockk(relaxed = true),
            ref = "refs/heads/main",
            repository = githubRepository,
            sender = sender
        )
        val handler = GithubPushTriggerHandler()

        assertTrue(handler.preMatch(event).isMatch)
        assertOperatorMatching(handler, event)
    }

    @Test
    fun shouldFilterBranchCreationOperatorsAfterParameterConversion() {
        val event = GithubCreateEvent(
            ref = "main",
            ref_type = "branch",
            repository = githubRepository,
            sender = sender
        )

        assertOperatorMatching(GithubCreateTriggerHandler(), event)
    }

    @Test
    fun shouldFilterTagCreationOperatorsAfterParameterConversion() {
        val event = GithubCreateEvent(
            ref = "v1.0.0",
            ref_type = "tag",
            repository = githubRepository,
            sender = sender
        )

        assertOperatorMatching(GithubCreateTriggerHandler(), event)
    }

    private fun <T : CodeWebhookEvent> assertOperatorMatching(handler: GitHookTriggerHandler<T>, event: T) {
        val cases = listOf<Triple<List<String>?, String?, String?>>(
            Triple(null, null, null),
            Triple(emptyList(), null, null),
            Triple(listOf("alice"), null, null),
            Triple(listOf("bob"), null, "bkRepoTriggerUserNotMatch"),
            Triple(listOf("bob", "alice"), null, null),
            Triple(listOf("alice"), "alice", "bkRepoTriggerUserIgnored"),
            Triple(null, "alice", "bkRepoTriggerUserIgnored"),
            Triple(listOf("alice"), "bob", null),
            Triple(listOf("\${operator}"), null, null)
        )
        for ((included, excluded, failedReason) in cases) {
            val params = GithubWebhookElementParams().getWebhookElementParams(
                element = CodeGithubWebHookTriggerElement(
                    repositoryHashId = "repo-hash-id",
                    repositoryType = TriggerRepositoryType.ID,
                    eventType = handler.getEventType(),
                    includeUsers = included?.joinToString(","),
                    excludeUsers = excluded
                ),
                variables = mapOf("operator" to "alice")
            )!!
            val result = handler.isMatch(
                event = event,
                projectId = "project",
                pipelineId = "pipeline",
                repository = pipelineRepository,
                webHookParams = params
            )

            assertEquals(failedReason == null, result.isMatch, "included=$included, excluded=$excluded")
            if (failedReason != null) {
                assertTrue(result.reason.orEmpty().contains(failedReason), result.reason)
            }
            params.excludeBranchName = handler.getBranchName(event)
            assertEquals(
                false,
                handler.isMatch(event, "project", "pipeline", pipelineRepository, params).isMatch,
                "Branch exclusion must still apply when the operator is included"
            )
        }
    }
}
