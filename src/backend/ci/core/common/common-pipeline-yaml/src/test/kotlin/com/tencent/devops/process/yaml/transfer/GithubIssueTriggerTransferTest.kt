/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.v2.models.on.IssueRule as IssueRuleV2
import com.tencent.devops.process.yaml.v3.models.on.IssueRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GithubIssueTriggerTransferTest {

    private val transfer = TriggerTransfer(
        client = mockk(relaxed = true),
        creator = mockk(relaxed = true),
        transferCache = mockk(relaxed = true)
    )

    @Test
    fun `should deserialize issue filter field names in v2 and v3`() {
        val json = """
            {
              "action": ["assign", "unassign", "update", "label", "unlabel"],
              "assignees": ["alice", "bob"],
              "assignees-ignore": ["bot"],
              "labels": ["bug", "feature-*"]
            }
        """.trimIndent()

        val v2 = JsonUtil.to(json, IssueRuleV2::class.java)
        val v3 = JsonUtil.to(json, IssueRule::class.java)

        assertEquals(listOf("alice", "bob"), v2.assignees)
        assertEquals(listOf("bot"), v2.assigneesIgnore)
        assertEquals(listOf("bug", "feature-*"), v3.labels)
    }

    @Test
    fun `should convert yaml issue filters to github trigger element`() {
        val elements = mutableListOf<Element>()

        transfer.yaml2TriggerGithub(
            triggerOn = TriggerOn(
                repoName = "bk-ci",
                issue = IssueRule(
                    action = listOf("assign", "unassign", "update", "label", "unlabel"),
                    assignees = listOf("alice", "bob"),
                    assigneesIgnore = listOf("bot"),
                    labels = listOf("bug", "feature-*")
                )
            ),
            elementQueue = elements
        )

        val element = elements.single() as CodeGithubWebHookTriggerElement
        assertEquals("alice,bob", element.includeAssignees)
        assertEquals("bot", element.excludeAssignees)
        assertEquals("bug,feature-*", element.includeLabels)
        assertEquals(null, element.excludeLabels)
    }

    @Test
    fun `should convert github trigger element issue filters to yaml`() {
        val element = CodeGithubWebHookTriggerElement(
            repositoryType = TriggerRepositoryType.NAME,
            repositoryName = "bk-ci",
            eventType = CodeEventType.ISSUES,
            includeIssueAction = listOf("assign", "unassign", "update", "label", "unlabel"),
            includeAssignees = "alice,bob",
            excludeAssignees = "bot",
            includeLabels = "bug,feature-*",
            excludeLabels = "wontfix"
        )

        val issue = transfer.git2YamlTriggerOn(
            elements = listOf(WebHookTriggerElementChanger(element)),
            projectId = "project",
            aspectWrapper = mockk<PipelineTransferAspectWrapper>(relaxed = true),
            defaultName = "GitHub事件触发"
        ).single().issue!!

        assertEquals(listOf("alice", "bob"), issue.assignees)
        assertEquals(listOf("bot"), issue.assigneesIgnore)
        assertEquals(listOf("bug", "feature-*"), issue.labels)
    }
}
