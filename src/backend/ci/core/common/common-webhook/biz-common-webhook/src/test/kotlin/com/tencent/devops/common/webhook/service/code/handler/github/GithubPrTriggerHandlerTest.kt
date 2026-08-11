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

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubLabel
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequest
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestBranch
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubRepository
import com.tencent.devops.common.webhook.pojo.code.github.GithubUser
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.repository.pojo.Repository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GithubPrTriggerHandlerTest {

    private val handler = GithubPrTriggerHandler(mockk<EventCacheService>(relaxed = true))
    private val githubRepository = mockk<GithubRepository>(relaxed = true) {
        every { cloneUrl } returns REPOSITORY_URL
        every { sshUrl } returns "git@github.com:blueking/bk-ci.git"
    }
    private val pipelineRepository = mockk<Repository> {
        every { url } returns REPOSITORY_URL
    }
    private val sender = mockk<GithubUser>(relaxed = true) {
        every { login } returns "author"
    }
    private val bug = label(id = 1L, name = "bug")
    private val urgent = label(id = 2L, name = "urgent")

    @Test
    fun shouldConvertAndAcceptPullRequestLabelActions() {
        val labeled = event(action = "labeled", changedLabel = urgent)
        val unlabeled = event(action = "unlabeled", changedLabel = urgent)

        assertEquals("label", labeled.getRealAction())
        assertEquals("unlabel", unlabeled.getRealAction())
        assertTrue(handler.preMatch(labeled).isMatch)
        assertTrue(handler.preMatch(unlabeled).isMatch)
    }

    @Test
    fun shouldRequirePullRequestLabelActionToBeConfigured() {
        assertFalse(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent),
                params = webHookParams(action = "open", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun shouldFilterPullRequestLabelActionsByChangedLabel() {
        assertTrue(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent, currentLabels = listOf(bug, urgent)),
                params = webHookParams(action = "label", includeLabels = "urg*")
            )
        )
        assertTrue(
            isMatch(
                event = event(action = "unlabeled", changedLabel = urgent, currentLabels = listOf(bug)),
                params = webHookParams(action = "unlabel", includeLabels = "urgent")
            )
        )
        assertFalse(
            isMatch(
                event = event(action = "unlabeled", changedLabel = urgent, currentLabels = listOf(bug)),
                params = webHookParams(action = "unlabel", includeLabels = "bug")
            )
        )
    }

    @Test
    fun shouldLetExcludedChangedLabelOverrideIncludedLabel() {
        assertFalse(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent),
                params = webHookParams(
                    action = "label",
                    includeLabels = "urgent",
                    excludeLabels = "urg*"
                )
            )
        )
    }

    @Test
    fun shouldKeepFilteringRegularPullRequestActionsByCurrentLabels() {
        assertTrue(
            isMatch(
                event = event(action = "opened", currentLabels = listOf(bug)),
                params = webHookParams(action = "open", includeLabels = "bug")
            )
        )
        assertFalse(
            isMatch(
                event = event(action = "opened", currentLabels = listOf(bug)),
                params = webHookParams(action = "open", includeLabels = "urgent")
            )
        )
    }

    private fun isMatch(event: GithubPullRequestEvent, params: WebHookParams): Boolean {
        return handler.isMatch(
            event = event,
            projectId = "project",
            pipelineId = "pipeline",
            repository = pipelineRepository,
            webHookParams = params
        ).isMatch
    }

    private fun webHookParams(
        action: String,
        includeLabels: String? = null,
        excludeLabels: String? = null
    ) = WebHookParams(
        repositoryConfig = RepositoryConfig(
            repositoryHashId = "repo-hash-id",
            repositoryName = null,
            repositoryType = RepositoryType.ID
        ),
        eventType = CodeEventType.PULL_REQUEST,
        includeMrAction = action,
        includeLabels = includeLabels,
        excludeLabels = excludeLabels
    )

    private fun event(
        action: String,
        changedLabel: GithubLabel? = null,
        currentLabels: List<GithubLabel> = emptyList()
    ): GithubPullRequestEvent {
        val baseBranch = branch(refName = "master")
        val headBranch = branch(refName = "feature")
        val pullRequest = mockk<GithubPullRequest>(relaxed = true) {
            every { merged } returns false
            every { base } returns baseBranch
            every { head } returns headBranch
            every { labels } returns currentLabels
            every { title } returns "PR title"
        }
        return GithubPullRequestEvent(
            action = action,
            number = 1,
            pullRequest = pullRequest,
            repository = githubRepository,
            sender = sender,
            label = changedLabel
        )
    }

    private fun branch(refName: String) = mockk<GithubPullRequestBranch>(relaxed = true) {
        every { ref } returns refName
        every { repo } returns githubRepository
    }

    private fun label(id: Long, name: String) = GithubLabel(
        color = "ffffff",
        default = false,
        description = null,
        id = id,
        name = name,
        nodeId = "label-$id",
        url = "https://api.github.com/repos/blueking/bk-ci/labels/$name"
    )

    companion object {
        private const val REPOSITORY_URL = "https://github.com/blueking/bk-ci.git"
    }
}
