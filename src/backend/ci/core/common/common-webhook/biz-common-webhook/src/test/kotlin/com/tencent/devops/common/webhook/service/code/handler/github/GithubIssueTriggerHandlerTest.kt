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

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABELS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_COLOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssue
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssuesEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubLabel
import com.tencent.devops.common.webhook.pojo.code.github.GithubRepository
import com.tencent.devops.common.webhook.pojo.code.github.GithubUser
import com.tencent.devops.repository.pojo.Repository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class GithubIssueTriggerHandlerTest {

    private val handler = GithubIssueTriggerHandler()
    private val repository = mockk<GithubRepository>(relaxed = true) {
        every { htmlUrl } returns "https://github.com/blueking/bk-ci"
        every { fullName } returns "blueking/bk-ci"
        every { defaultBranch } returns "master"
        every { getRepoUrl() } returns "https://github.com/blueking/bk-ci"
    }
    private val pipelineRepository = mockk<Repository> {
        every { url } returns "https://github.com/blueking/bk-ci"
    }
    private val author = user(id = 1L, login = "author")
    private val alice = user(id = 2L, login = "alice")
    private val bob = user(id = 3L, login = "bob")
    private val bug = label(id = 10L, name = "bug", description = null)
    private val urgent = label(id = 11L, name = "urgent", description = "needs attention")

    @Test
    fun `should convert assignee and label actions`() {
        val labeled = event("labeled", changedLabel = urgent)
        val unlabeled = event("unlabeled", changedLabel = urgent, currentLabels = listOf(bug))

        assertEquals("assigned", event("assigned").convertAction())
        assertEquals("unassigned", event("unassigned").convertAction())
        assertEquals("labeled", labeled.convertAction())
        assertEquals("unlabeled", unlabeled.convertAction())
    }

    @Test
    fun `should match configured assignee and label actions`() {
        mapOf(
            "assigned" to "assigned",
            "unassigned" to "unassigned",
            "labeled" to "labeled",
            "unlabeled" to "unlabeled"
        ).forEach { (githubAction, configuredAction) ->
            val result = handler.isMatch(
                event = event(githubAction),
                projectId = "project",
                pipelineId = "pipeline",
                repository = pipelineRepository,
                webHookParams = webHookParams(configuredAction)
            )

            assertTrue(result.isMatch, "$githubAction should match $configuredAction")
        }
    }

    @Test
    fun `should reject issue action that is not configured`() {
        val result = handler.isMatch(
            event = event("assigned"),
            projectId = "project",
            pipelineId = "pipeline",
            repository = pipelineRepository,
            webHookParams = webHookParams("update")
        )

        assertFalse(result.isMatch)
    }

    @Test
    fun `should include label events by the changed label`() {
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("labeled", includeLabels = "urgent")
            )
        )
        assertFalse(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("labeled", includeLabels = "missing")
            )
        )
    }

    @Test
    fun `should filter assignee events by the changed assignee`() {
        assertTrue(
            isMatch(
                event = event("assigned", changedAssignee = alice),
                params = webHookParams("assigned", includeAssignees = "alice")
            )
        )
        assertFalse(
            isMatch(
                event = event("assigned", changedAssignee = alice),
                params = webHookParams("assigned", includeAssignees = "bob")
            )
        )
        assertFalse(
            isMatch(
                event = event("assigned", changedAssignee = alice),
                params = webHookParams("assigned", excludeAssignees = "alice")
            )
        )
    }

    @Test
    fun `should filter unassigned events by the removed assignee only`() {
        // 移除 bob 后 issue 剩余 alice，但过滤只看本次变更的受理人
        assertTrue(
            isMatch(
                event = event("unassigned", changedAssignee = bob, currentAssignees = listOf(alice)),
                params = webHookParams("unassigned", includeAssignees = "bob")
            )
        )
        assertFalse(
            isMatch(
                event = event("unassigned", changedAssignee = bob, currentAssignees = listOf(alice)),
                params = webHookParams("unassigned", includeAssignees = "alice")
            )
        )
    }

    @Test
    fun `should filter non assignee actions by current assignees`() {
        assertTrue(
            isMatch(
                event = event("edited", currentAssignees = listOf(alice)),
                params = webHookParams("update", includeAssignees = "alice")
            )
        )
        assertFalse(
            isMatch(
                event = event("edited", currentAssignees = listOf(alice)),
                params = webHookParams("update", includeAssignees = "bob")
            )
        )
    }

    @Test
    fun `should match non assignee actions when any current assignee included`() {
        assertTrue(
            isMatch(
                event = event("edited", currentAssignees = listOf(alice, bob)),
                params = webHookParams("update", includeAssignees = "bob")
            )
        )
    }

    @Test
    fun `should reject non assignee actions when any current assignee excluded`() {
        assertFalse(
            isMatch(
                event = event("edited", currentAssignees = listOf(alice, bob)),
                params = webHookParams("update", excludeAssignees = "bob")
            )
        )
    }

    @Test
    fun `should let excluded assignee override included assignee for non assignee actions`() {
        assertFalse(
            isMatch(
                event = event("edited", currentAssignees = listOf(alice, bob)),
                params = webHookParams(
                    includeIssueAction = "update",
                    includeAssignees = "alice",
                    excludeAssignees = "bob"
                )
            )
        )
    }

    @Test
    fun `should reject non assignee actions when issue has no assignees`() {
        assertFalse(
            isMatch(
                event = event("edited", currentAssignees = emptyList()),
                params = webHookParams("update", includeAssignees = "alice")
            )
        )
    }

    @Test
    fun `should not reject non assignee actions without assignees when only exclude configured`() {
        assertTrue(
            isMatch(
                event = event("edited", currentAssignees = emptyList()),
                params = webHookParams("update", excludeAssignees = "alice")
            )
        )
    }

    @Test
    fun `should filter edited events by current assignees from github payload`() {
        val parsed = githubEvent("GithubIssueEditedEvent.json")

        assertEquals("edited", parsed.action)
        assertEquals(listOf("bob"), parsed.issue.assignees?.map { it.login })
        assertNull(parsed.assignee)
        // 受理人为 bob，配置 includeAssignees=alice 时不应触发（复现 issue 场景）
        assertFalse(
            isMatch(
                event = parsed,
                params = webHookParams("update", includeAssignees = "alice")
            )
        )
        assertTrue(
            isMatch(
                event = parsed,
                params = webHookParams("update", includeAssignees = "bob")
            )
        )
    }

    @Test
    fun `should exclude issue actions by webhook operator`() {
        assertFalse(
            isMatch(
                event = event("edited"),
                params = webHookParams("update", excludeUsers = "author")
            )
        )
    }

    @Test
    fun `should filter labeled and unlabeled events by the changed label`() {
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("labeled", includeLabels = "urgent")
            )
        )
        assertFalse(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("labeled", includeLabels = "bug")
            )
        )
        assertTrue(
            isMatch(
                event = event("unlabeled", changedLabel = urgent),
                params = webHookParams("unlabeled", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun `should let excluded label override included label`() {
        assertFalse(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams(
                    includeIssueAction = "labeled",
                    includeLabels = "urgent",
                    excludeLabels = "urgent"
                )
            )
        )
    }

    @Test
    fun `should not exclude label event when changed label is not excluded`() {
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams(
                    includeIssueAction = "labeled",
                    excludeLabels = "bug"
                )
            )
        )
    }

    @Test
    fun `should apply state filters to all issue actions`() {
        assertTrue(
            isMatch(
                event = event("assigned"),
                params = webHookParams(
                    includeIssueAction = "assigned,labeled",
                    includeAssignees = "alice",
                    includeLabels = "urgent"
                )
            )
        )
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams(
                    includeIssueAction = "assigned,labeled",
                    includeAssignees = "alice",
                    includeLabels = "urgent"
                )
            )
        )
    }

    @Test
    fun `should expose all current assignees after assignment`() {
        val params = handler.retrieveParams(
            event = event(action = "assigned"),
            projectId = null,
            repository = null
        )

        assertEquals("assigned", params[PIPELINE_GIT_ACTION])
        assertEquals("alice,bob", params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("alice"))
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("bob"))
    }

    @Test
    fun `should expose current assignees after unassignment`() {
        val params = handler.retrieveParams(
            event = event(action = "unassigned", currentAssignees = listOf(alice)),
            projectId = null,
            repository = null
        )

        assertEquals("unassigned", params[PIPELINE_GIT_ACTION])
        assertEquals("alice", params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("alice"))
        assertFalse(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("bob"))
    }

    @Test
    fun `should expose added label and all current labels`() {
        val params = handler.retrieveParams(
            event = event(action = "labeled", changedLabel = urgent),
            projectId = null,
            repository = null
        )

        assertEquals("labeled", params[PIPELINE_GIT_ACTION])
        assertEquals("urgent", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL])
        assertEquals(11L, params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID])
        assertEquals("ff0000", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_COLOR])
        assertEquals("needs attention", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_DESCRIPTION])
        assertEquals("bug,urgent", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("bug"))
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("urgent"))
    }

    @Test
    fun `should expose removed label but exclude it from current labels`() {
        val params = handler.retrieveParams(
            event = event(action = "unlabeled", changedLabel = urgent, currentLabels = listOf(bug)),
            projectId = null,
            repository = null
        )

        assertEquals("unlabeled", params[PIPELINE_GIT_ACTION])
        assertEquals("urgent", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL])
        assertEquals(11L, params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID])
        assertEquals("bug", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("bug"))
        assertFalse(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("urgent"))
    }

    @Test
    fun `should deserialize current assignees from assigned payload`() {
        val parsed = githubEvent("GithubIssueAssignedEvent.json")

        assertEquals("assigned", parsed.action)
        assertEquals(listOf("alice", "bob"), parsed.issue.assignees?.map { it.login })
        assertFalse(parsed.issue.locked.toBoolean())
    }

    @Test
    fun `should deserialize changed label from github payload`() {
        val parsed = githubEvent("GithubIssueLabeledEvent.json")

        assertEquals("labeled", parsed.action)
        assertEquals("urgent", parsed.label?.name)
        assertEquals(11L, parsed.label?.id)
        assertEquals(listOf("bug", "urgent"), parsed.issue.labels.map { it.name })
    }

    private fun event(
        action: String,
        changedLabel: GithubLabel? = null,
        changedAssignee: GithubUser? = null,
        currentAssignees: List<GithubUser> = listOf(alice, bob),
        currentLabels: List<GithubLabel> = listOf(bug, urgent)
    ) = GithubIssuesEvent(
        action = action,
        issue = GithubIssue(
            url = "https://api.github.com/repos/blueking/bk-ci/issues/1",
            htmlUrl = "https://github.com/blueking/bk-ci/issues/1",
            id = 100L,
            nodeId = "issue-node",
            createdAt = "2026-08-03T00:00:00Z",
            updatedAt = "2026-08-03T00:00:00Z",
            number = 1L,
            title = "issue title",
            user = author,
            labels = currentLabels,
            state = "open",
            locked = "false",
            assignees = currentAssignees,
            closedAt = null,
            body = "issue body",
            pullRequest = null,
            milestone = null
        ),
        repository = repository,
        sender = author,
        label = changedLabel,
        assignee = changedAssignee
    )

    private fun githubEvent(fileName: String): GithubIssuesEvent {
        val payload = ClassPathResource("com/tencent/devops/common/webhook/service/code/github/$fileName")
            .inputStream.bufferedReader().use { it.readText() }
        return JsonUtil.to(payload, GithubIssuesEvent::class.java)
    }

    private fun isMatch(event: GithubIssuesEvent, params: WebHookParams): Boolean {
        return handler.isMatch(
            event = event,
            projectId = "project",
            pipelineId = "pipeline",
            repository = pipelineRepository,
            webHookParams = params
        ).isMatch
    }

    private fun webHookParams(
        includeIssueAction: String,
        includeAssignees: String? = null,
        excludeAssignees: String? = null,
        excludeUsers: String? = null,
        includeLabels: String? = null,
        excludeLabels: String? = null
    ) = WebHookParams(
        repositoryConfig = RepositoryConfig(
            repositoryHashId = "repo-hash-id",
            repositoryName = null,
            repositoryType = RepositoryType.ID
        ),
        eventType = CodeEventType.ISSUES,
        excludeUsers = excludeUsers,
        includeIssueAction = includeIssueAction,
        includeAssignees = includeAssignees,
        excludeAssignees = excludeAssignees,
        includeLabels = includeLabels,
        excludeLabels = excludeLabels
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

    private fun label(id: Long, name: String, description: String?) = GithubLabel(
        color = "ff0000",
        default = false,
        description = description,
        id = id,
        name = name,
        nodeId = "label-$id",
        url = "https://api.github.com/repos/blueking/bk-ci/labels/$name"
    )
}
