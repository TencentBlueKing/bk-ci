package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.ImateAvatar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoffeeAIProjectScopeTest {

    @Test
    fun `personal project id starts with underscore`() {
        assertTrue(CoffeeAIProjectScope.isPersonalProject("_alice"))
        assertTrue(CoffeeAIProjectScope.isPersonalProject("_alice_personal"))
        assertFalse(CoffeeAIProjectScope.isPersonalProject("team-proj"))
        assertFalse(CoffeeAIProjectScope.isPersonalProject(""))
        assertFalse(CoffeeAIProjectScope.isPersonalProject(null))
    }

    @Test
    fun `cds shared flag or other owner counts as shared avatar`() {
        assertTrue(CoffeeAIProjectScope.isSharedAvatar(avatar(owner = "alice", shared = true), "alice"))
        assertTrue(CoffeeAIProjectScope.isSharedAvatar(avatar(owner = "bob", shared = false), "alice"))
        assertFalse(CoffeeAIProjectScope.isSharedAvatar(avatar(owner = "alice", shared = false), "alice"))
    }

    @Test
    fun `personal pipeline only keeps own unshared avatars`() {
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "alice", shared = false),
                userId = "alice",
                pipelineProjectId = "_alice"
            )
        )
        assertFalse(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "bob", shared = true),
                userId = "alice",
                pipelineProjectId = "_alice"
            )
        )
        assertFalse(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "alice", shared = true),
                userId = "alice",
                pipelineProjectId = "_alice"
            )
        )
    }

    @Test
    fun `team pipeline keeps shared avatars regardless of bound project`() {
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "bob", shared = true, bound = "other-proj"),
                userId = "alice",
                pipelineProjectId = "team-proj"
            )
        )
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "alice", shared = true),
                userId = "alice",
                pipelineProjectId = "team-proj"
            )
        )
        assertEquals(
            CoffeeAIProjectScope.REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT,
            CoffeeAIProjectScope.mismatchReason(
                avatar = avatar(owner = "alice", shared = false),
                userId = "alice",
                pipelineProjectId = "team-proj"
            )
        )
        assertNull(
            CoffeeAIProjectScope.mismatchReason(
                avatar = avatar(owner = "bob", shared = true),
                userId = "alice",
                pipelineProjectId = "team-proj"
            )
        )
    }

    private fun avatar(
        owner: String,
        shared: Boolean = false,
        bound: String? = null
    ) = ImateAvatar(
        digitalAvatarId = "av-$owner",
        agentId = "agent-$owner",
        workspaceName = "ins-$owner",
        owner = owner,
        shared = shared,
        selectedBoundProjectId = bound
    )
}
