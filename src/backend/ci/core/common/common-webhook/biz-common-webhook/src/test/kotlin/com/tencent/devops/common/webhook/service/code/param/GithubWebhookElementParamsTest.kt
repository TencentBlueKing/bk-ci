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
                includeIssueAction = listOf("assign", "unassign", "label", "unlabel"),
                includeIssueAssignees = "alice,bob",
                excludeIssueAssignees = "bot",
                includeIssueLabels = "bug,feature-*",
                excludeIssueLabels = "wontfix"
            ),
            variables = emptyMap()
        )!!

        assertEquals("assign,unassign,label,unlabel", params.includeIssueAction)
        assertEquals("alice,bob", params.includeIssueAssignees)
        assertEquals("bot", params.excludeIssueAssignees)
        assertEquals("bug,feature-*", params.includeIssueLabels)
        assertEquals("wontfix", params.excludeIssueLabels)
    }
}
