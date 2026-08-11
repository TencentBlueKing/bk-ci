/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.common.webhook.service.code.param

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GithubWebhookElementParamsTest {

    @Test
    fun `should convert issue assignee and label filters`() {
        val params = GithubWebhookElementParams().getWebhookElementParams(
            element = CodeGithubWebHookTriggerElement(
                repositoryHashId = "repo-hash-id",
                repositoryType = TriggerRepositoryType.ID,
                eventType = CodeEventType.ISSUES,
                includeIssueAction = listOf("assign", "unassign", "update", "label", "unlabel"),
                includeAssignees = "alice,bob",
                excludeAssignees = "bot",
                includeAssigneeChanges = "reviewer",
                excludeAssigneeChanges = "automation",
                includeLabels = "bug,feature-*",
                excludeLabels = "wontfix"
            ),
            variables = emptyMap()
        )!!

        assertEquals("assign,unassign,update,label,unlabel", params.includeIssueAction)
        assertEquals("alice,bob", params.includeAssignees)
        assertEquals("bot", params.excludeAssignees)
        assertEquals("reviewer", params.includeAssigneeChanges)
        assertEquals("automation", params.excludeAssigneeChanges)
        assertEquals("bug,feature-*", params.includeLabels)
        assertEquals("wontfix", params.excludeLabels)
    }

    @Test
    fun `should preserve excluded labels for pull request`() {
        val params = GithubWebhookElementParams().getWebhookElementParams(
            element = CodeGithubWebHookTriggerElement(
                repositoryHashId = "repo-hash-id",
                repositoryType = TriggerRepositoryType.ID,
                eventType = CodeEventType.PULL_REQUEST,
                includeLabels = "bug",
                excludeLabels = "wontfix"
            ),
            variables = emptyMap()
        )!!

        assertEquals("bug", params.includeLabels)
        assertEquals("wontfix", params.excludeLabels)
    }
}
