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
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_ID
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
import com.tencent.devops.common.webhook.pojo.code.github.GithubLabelAction
import com.tencent.devops.common.webhook.pojo.code.github.GithubRepository
import com.tencent.devops.common.webhook.pojo.code.github.GithubUser
import com.tencent.devops.common.webhook.pojo.code.github.getLabelChange
import com.tencent.devops.repository.pojo.Repository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `should convert assign and label actions`() {
        val labeled = event("labeled", changedLabel = urgent)
        val unlabeled = event("unlabeled", changedLabel = urgent, currentLabels = listOf(bug))

        assertEquals("assign", event("assigned").convertAction())
        assertEquals("unassign", event("unassigned").convertAction())
        assertEquals("label", labeled.convertAction())
        assertEquals("unlabel", unlabeled.convertAction())
        assertEquals(GithubLabelAction.LABEL, labeled.getLabelChange()?.action)
        assertEquals(GithubLabelAction.UNLABEL, unlabeled.getLabelChange()?.action)
        assertEquals(urgent, unlabeled.getLabelChange()?.changedLabel)
        assertEquals(listOf(bug), unlabeled.getLabelChange()?.currentLabels)
    }

    @Test
    fun `should match configured assign and label actions`() {
        mapOf(
            "assigned" to "assign",
            "unassigned" to "unassign",
            "labeled" to "label",
            "unlabeled" to "unlabel"
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
            webHookParams = webHookParams("label,unlabel")
        )

        assertFalse(result.isMatch)
    }

    @Test
    fun `should filter assigned and unassigned events by changed assignee`() {
        assertTrue(
            isMatch(
                event = event("assigned", assignee = bob),
                params = webHookParams("assign", includeAssignees = "bob")
            )
        )
        assertFalse(
            isMatch(
                event = event("assigned", assignee = bob),
                params = webHookParams("assign", includeAssignees = "alice")
            )
        )
        assertTrue(
            isMatch(
                event = event("unassigned", assignee = bob, currentAssignees = listOf(alice)),
                params = webHookParams("unassign", includeAssignees = "bob")
            )
        )
    }

    @Test
    fun `should let excluded assignee override included assignee`() {
        assertFalse(
            isMatch(
                event = event("assigned", assignee = bob),
                params = webHookParams(
                    includeIssueAction = "assign",
                    includeAssignees = "bob",
                    excludeAssignees = "bob"
                )
            )
        )
    }

    @Test
    fun `should filter labeled and unlabeled events by changed label`() {
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("label", includeLabels = "urg*")
            )
        )
        assertFalse(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams("label", includeLabels = "bug")
            )
        )
        assertTrue(
            isMatch(
                event = event("unlabeled", changedLabel = urgent, currentLabels = listOf(bug)),
                params = webHookParams("unlabel", includeLabels = "urgent")
            )
        )
    }

    @Test
    fun `should reject excluded changed label`() {
        assertFalse(
            isMatch(
                event = event("unlabeled", changedLabel = urgent, currentLabels = listOf(bug)),
                params = webHookParams(
                    includeIssueAction = "unlabel",
                    includeLabels = "urgent",
                    excludeLabels = "urg*"
                )
            )
        )
    }

    @Test
    fun `should only apply filters related to current action`() {
        assertTrue(
            isMatch(
                event = event("assigned", assignee = bob),
                params = webHookParams(
                    includeIssueAction = "assign,label",
                    includeAssignees = "bob",
                    includeLabels = "missing"
                )
            )
        )
        assertTrue(
            isMatch(
                event = event("labeled", changedLabel = urgent),
                params = webHookParams(
                    includeIssueAction = "assign,label",
                    includeAssignees = "missing",
                    includeLabels = "urgent"
                )
            )
        )
    }

    @Test
    fun `should expose assigned user and all current assignees`() {
        val params = handler.retrieveParams(
            event = event(action = "assigned", assignee = bob),
            projectId = null,
            repository = null
        )

        assertEquals("assign", params[PIPELINE_GIT_ACTION])
        assertEquals("bob", params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE])
        assertEquals(3L, params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_ID])
        assertEquals("alice,bob", params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("alice"))
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES].toString().contains("bob"))
    }

    @Test
    fun `should expose unassigned user but exclude it from current assignees`() {
        val params = handler.retrieveParams(
            event = event(action = "unassigned", assignee = bob, currentAssignees = listOf(alice)),
            projectId = null,
            repository = null
        )

        assertEquals("unassign", params[PIPELINE_GIT_ACTION])
        assertEquals("bob", params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE])
        assertEquals(3L, params[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_ID])
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

        assertEquals("label", params[PIPELINE_GIT_ACTION])
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

        assertEquals("unlabel", params[PIPELINE_GIT_ACTION])
        assertEquals("urgent", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL])
        assertEquals(11L, params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID])
        assertEquals("bug", params[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES])
        assertTrue(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("bug"))
        assertFalse(params[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS].toString().contains("urgent"))
    }

    @Test
    fun `should deserialize changed assignee from github payload`() {
        val parsed = githubEvent("GithubIssueAssignedEvent.json")

        assertEquals("assigned", parsed.action)
        assertEquals("bob", parsed.assignee?.login)
        assertEquals(3L, parsed.assignee?.id)
        assertEquals(listOf("alice", "bob"), parsed.issue.assignees?.map { it.login })
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
        assignee: GithubUser? = null,
        changedLabel: GithubLabel? = null,
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
        assignee = assignee,
        label = changedLabel
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
        includeLabels: String? = null,
        excludeLabels: String? = null
    ) = WebHookParams(
        repositoryConfig = RepositoryConfig(
            repositoryHashId = "repo-hash-id",
            repositoryName = null,
            repositoryType = RepositoryType.ID
        ),
        eventType = CodeEventType.ISSUES,
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
