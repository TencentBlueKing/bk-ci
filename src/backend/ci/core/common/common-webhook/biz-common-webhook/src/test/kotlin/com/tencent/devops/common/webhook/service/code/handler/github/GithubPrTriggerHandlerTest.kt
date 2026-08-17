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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ACTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEES
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE_LOGINS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LABELS
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class GithubPrTriggerHandlerTest {

    private val handler = GithubPrTriggerHandler(mockk<EventCacheService>(relaxed = true))
    private val githubRepository = mockk<GithubRepository>(relaxed = true) {
        every { cloneUrl } returns REPOSITORY_URL
        every { sshUrl } returns "git@github.com:blueking/bk-ci.git"
    }
    private val pipelineRepository = mockk<Repository> {
        every { url } returns REPOSITORY_URL
    }
    private val sender = user(id = 1L, login = "author")
    private val alice = user(id = 2L, login = "alice")
    private val bug = label(id = 1L, name = "bug")
    private val urgent = label(id = 2L, name = "urgent")

    @Test
    fun shouldConvertPullRequestLabelEventsToLabelActions() {
        val labeled = event(action = "labeled")
        val unlabeled = event(action = "unlabeled")

        assertEquals("labeled", labeled.getRealAction())
        assertEquals("unlabeled", unlabeled.getRealAction())
        assertTrue(handler.preMatch(labeled).isMatch)
        assertTrue(handler.preMatch(unlabeled).isMatch)
    }

    @Test
    fun shouldConvertPullRequestAssigneeEvents() {
        assertEquals("assigned", event(action = "assigned").getRealAction())
        assertEquals("unassigned", event(action = "unassigned").getRealAction())
    }

    @Test
    fun shouldDeserializePullRequestLabelEventPayload() {
        val parsed = githubPullRequestEvent()

        assertEquals("labeled", parsed.action)
        assertEquals("labeled", parsed.getRealAction())
        assertEquals(listOf("bug", "urgent"), parsed.pullRequest.labels.map { it.name })
        assertNull(parsed.pullRequest.labels.first().description)
    }

    @Test
    fun shouldRequirePullRequestEditActionToBeConfigured() {
        assertFalse(
            isMatch(
                event = event(action = "labeled"),
                params = webHookParams(action = "open", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun shouldFilterPullRequestLabelEventsByChangedLabel() {
        assertTrue(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent),
                params = webHookParams(action = "labeled", includeLabels = "urgent")
            )
        )
        assertFalse(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent),
                params = webHookParams(action = "labeled", includeLabels = "bug")
            )
        )
        assertTrue(
            isMatch(
                event = event(action = "unlabeled", changedLabel = urgent),
                params = webHookParams(action = "unlabeled", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun shouldLetExcludedLabelOverrideIncludedLabel() {
        assertFalse(
            isMatch(
                event = event(action = "labeled", changedLabel = urgent),
                params = webHookParams(
                    action = "labeled",
                    includeLabels = "urgent",
                    excludeLabels = "urgent"
                )
            )
        )
    }

    @Test
    fun shouldNotFilterRegularPullRequestActionsByLabels() {
        assertTrue(
            isMatch(
                event = event(action = "opened", currentLabels = listOf(bug)),
                params = webHookParams(action = "open", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun shouldFilterPullRequestAssigneeEventsByChangedAssignee() {
        assertTrue(
            isMatch(
                event = event(action = "assigned", changedAssignee = alice),
                params = webHookParams(action = "assigned", includeAssignees = "alice")
            )
        )
        assertFalse(
            isMatch(
                event = event(action = "assigned", changedAssignee = alice),
                params = webHookParams(action = "assigned", excludeAssignees = "alice")
            )
        )
    }

    @Test
    fun shouldExposePullRequestCurrentAssignees() {
        val params = handler.retrieveParams(
            event = event(action = "unassigned", currentAssignees = listOf(alice)),
            projectId = null,
            repository = null
        )

        assertEquals("unassigned", params[BK_REPO_GIT_WEBHOOK_MR_ACTION])
        assertEquals("unassigned", params[PIPELINE_GIT_MR_ACTION])
        assertEquals("alice", params[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE_LOGINS])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEES].toString().contains("alice"))
        assertFalse(params[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEES].toString().contains("bob"))
    }

    @Test
    fun shouldNormalizePullRequestUnlabeledActionAndExposeCurrentLabels() {
        val params = handler.retrieveParams(
            event = event(action = "unlabeled", currentLabels = listOf(bug)),
            projectId = null,
            repository = null
        )

        assertEquals("unlabeled", params[BK_REPO_GIT_WEBHOOK_MR_ACTION])
        assertEquals("unlabeled", params[PIPELINE_GIT_MR_ACTION])
        assertEquals("unlabeled", params[PIPELINE_GIT_ACTION])
        assertEquals("bug", params[BK_REPO_GIT_WEBHOOK_MR_LABELS])
    }

    @Test
    fun shouldKeepExistingPullRequestActionVariablesUnchanged() {
        val params = handler.retrieveParams(
            event = event(action = "opened", currentLabels = listOf(bug)),
            projectId = null,
            repository = null
        )

        assertEquals("open", params[BK_REPO_GIT_WEBHOOK_MR_ACTION])
        assertEquals("opened", params[PIPELINE_GIT_MR_ACTION])
        assertEquals("opened", params[PIPELINE_GIT_ACTION])
        assertEquals("bug", params[BK_REPO_GIT_WEBHOOK_MR_LABELS])
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
        excludeLabels: String? = null,
        excludeUsers: String? = null,
        includeAssignees: String? = null,
        excludeAssignees: String? = null
    ) = WebHookParams(
        repositoryConfig = RepositoryConfig(
            repositoryHashId = "repo-hash-id",
            repositoryName = null,
            repositoryType = RepositoryType.ID
        ),
        eventType = CodeEventType.PULL_REQUEST,
        includeMrAction = action,
        excludeUsers = excludeUsers,
        includeAssignees = includeAssignees,
        excludeAssignees = excludeAssignees,
        includeLabels = includeLabels,
        excludeLabels = excludeLabels
    )

    private fun event(
        action: String,
        currentLabels: List<GithubLabel> = emptyList(),
        currentAssignees: List<GithubUser> = emptyList(),
        changedLabel: GithubLabel? = null,
        changedAssignee: GithubUser? = null
    ): GithubPullRequestEvent {
        val baseBranch = branch(refName = "master")
        val headBranch = branch(refName = "feature")
        val pullRequest = mockk<GithubPullRequest>(relaxed = true) {
            every { merged } returns false
            every { base } returns baseBranch
            every { head } returns headBranch
            every { labels } returns currentLabels
            every { assignees } returns currentAssignees
            every { title } returns "PR title"
        }
        return GithubPullRequestEvent(
            action = action,
            number = 1,
            pullRequest = pullRequest,
            repository = githubRepository,
            sender = sender,
            label = changedLabel,
            assignee = changedAssignee
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun githubPullRequestEvent(): GithubPullRequestEvent {
        val payload = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/github/GithubIssueLabeledEvent.json"
        ).inputStream.bufferedReader().use { it.readText() }
        val event = JsonUtil.to<MutableMap<String, Any>>(payload)
        val issue = event.remove("issue") as Map<String, Any>
        val repository = event["repository"] as Map<String, Any>
        val sender = event["sender"] as Map<String, Any>
        event["action"] = "labeled"
        val branch = { ref: String ->
            mapOf(
                "label" to "blueking:$ref",
                "ref" to ref,
                "repo" to repository,
                "sha" to "commit-$ref",
                "user" to sender
            )
        }
        event["number"] = 1
        event["pull_request"] = mapOf(
            "additions" to 1,
            "assignees" to emptyList<Any>(),
            "author_association" to "MEMBER",
            "base" to branch("master"),
            "changed_files" to 1,
            "comments" to 0,
            "comments_url" to "https://api.github.com/repos/blueking/bk-ci/issues/1/comments",
            "commits" to 1,
            "commits_url" to "https://api.github.com/repos/blueking/bk-ci/pulls/1/commits",
            "created_at" to "2026-08-03T00:00:00Z",
            "deletions" to 0,
            "diff_url" to "https://github.com/blueking/bk-ci/pull/1.diff",
            "draft" to false,
            "head" to branch("feature"),
            "html_url" to "https://github.com/blueking/bk-ci/pull/1",
            "id" to 100L,
            "issue_url" to "https://api.github.com/repos/blueking/bk-ci/issues/1",
            "labels" to issue.getValue("labels"),
            "locked" to false,
            "merged" to false,
            "node_id" to "pull-request-node",
            "number" to 1,
            "patch_url" to "https://github.com/blueking/bk-ci/pull/1.patch",
            "requested_reviewers" to emptyList<Any>(),
            "requested_teams" to emptyList<Any>(),
            "review_comment_url" to "https://api.github.com/repos/blueking/bk-ci/pulls/comments{/number}",
            "review_comments" to 0,
            "review_comments_url" to "https://api.github.com/repos/blueking/bk-ci/pulls/1/comments",
            "state" to "open",
            "statuses_url" to "https://api.github.com/repos/blueking/bk-ci/statuses/commit-feature",
            "title" to "PR title",
            "url" to "https://api.github.com/repos/blueking/bk-ci/pulls/1",
            "user" to sender
        )
        return JsonUtil.to(JsonUtil.toJson(event), GithubPullRequestEvent::class.java)
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

    private fun user(id: Long, login: String) = GithubUser(
        gravatarId = "",
        htmlUrl = "https://github.com/$login",
        id = id,
        login = login,
        nodeId = "user-$id",
        siteAdmin = false,
        type = "User",
        url = "https://api.github.com/users/$login",
        createdAt = null,
        updatedAt = null
    )

    companion object {
        private const val REPOSITORY_URL = "https://github.com/blueking/bk-ci.git"
    }
}
