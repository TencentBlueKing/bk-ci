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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GithubWebhookElementParamsTest {

    private val supportedEvents = listOf(CodeEventType.PUSH, CodeEventType.CREATE, CodeEventType.PULL_REQUEST)

    @Test
    fun shouldConvertIncludedUsersFromFormJson() {
        val element = JsonUtil.to(
            """{
                "@type": "codeGithubWebHookTrigger",
                "repositoryHashId": "repo-hash-id",
                "repositoryType": "ID",
                "eventType": "PULL_REQUEST",
                "includeUsers": "alice,bob"
            }""".trimIndent(),
            CodeGithubWebHookTriggerElement::class.java
        )
        val params = GithubWebhookElementParams().getWebhookElementParams(element, emptyMap())!!

        assertEquals("alice,bob", element.includeUsers)
        assertEquals("alice,bob", params.includeUsers)
        assertEquals("open,reopen,push-update", params.includeMrAction)
    }

    @Test
    fun shouldConvertIncludedUsersAndResolveVariablesForSupportedEvents() {
        for (eventType in supportedEvents) {
            val params = includedUserParams(
                users = "alice,\${operator}",
                variables = mapOf("operator" to "bob"),
                eventType = eventType
            )

            assertEquals("alice,bob", params.includeUsers, "eventType=$eventType")
            assertEquals("open,reopen,push-update", params.includeMrAction)
        }
    }

    @Test
    fun shouldKeepMissingAndEmptyIncludedUsersUnrestricted() {
        for (eventType in supportedEvents) {
            assertEquals("", includedUserParams(null, eventType = eventType).includeUsers)
            assertEquals("", includedUserParams("", eventType = eventType).includeUsers)
        }
    }

    @Test
    fun shouldPreserveExcludedUsersAlongsideIncludedUsers() {
        for (eventType in supportedEvents) {
            val params = GithubWebhookElementParams().getWebhookElementParams(
                element = CodeGithubWebHookTriggerElement(
                    repositoryHashId = "repo-hash-id",
                    repositoryType = TriggerRepositoryType.ID,
                    eventType = eventType,
                    includeUsers = "alice",
                    excludeUsers = "bot"
                ),
                variables = emptyMap()
            )!!

            assertEquals("alice", params.includeUsers, "eventType=$eventType")
            assertEquals("bot", params.excludeUsers)
        }
    }

    @Test
    fun shouldExposeIncludedUsersInSupportedTriggerConditions() {
        for (eventType in supportedEvents) {
            val element = CodeGithubWebHookTriggerElement(
                eventType = eventType,
                includeUsers = "alice,bob"
            )

            assertEquals(listOf("alice", "bob"), element.triggerCondition().single { it.name == "includeUsers" }.value)
            assertEquals(
                false,
                element.copy(includeUsers = "").triggerCondition().any { it.name == "includeUsers" }
            )
        }
    }

    private fun includedUserParams(
        users: String?,
        variables: Map<String, String> = emptyMap(),
        eventType: CodeEventType = CodeEventType.PULL_REQUEST
    ) = GithubWebhookElementParams().getWebhookElementParams(
        element = CodeGithubWebHookTriggerElement(
            repositoryHashId = "repo-hash-id",
            repositoryType = TriggerRepositoryType.ID,
            eventType = eventType,
            includeUsers = users
        ),
        variables = variables
    )!!

    @Test
    fun `should convert issue assignee and label filters`() {
        val params = GithubWebhookElementParams().getWebhookElementParams(
            element = CodeGithubWebHookTriggerElement(
                repositoryHashId = "repo-hash-id",
                repositoryType = TriggerRepositoryType.ID,
                eventType = CodeEventType.ISSUES,
                includeIssueAction = listOf("assigned", "unassigned", "update", "labeled", "unlabeled"),
                includeAssignees = "alice,bob",
                excludeAssignees = "bot",
                includeLabels = "bug,feature-*",
                excludeLabels = "wontfix"
            ),
            variables = emptyMap()
        )!!

        assertEquals("assigned,unassigned,update,labeled,unlabeled", params.includeIssueAction)
        assertEquals("alice,bob", params.includeAssignees)
        assertEquals("bot", params.excludeAssignees)
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
