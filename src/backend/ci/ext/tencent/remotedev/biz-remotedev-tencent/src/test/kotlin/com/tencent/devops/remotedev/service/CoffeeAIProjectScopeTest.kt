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
        assertFalse(CoffeeAIProjectScope.isPersonalProject("landunplugins"))
        assertFalse(CoffeeAIProjectScope.isPersonalProject(""))
        assertFalse(CoffeeAIProjectScope.isPersonalProject(null))
    }

    @Test
    fun `shared flag or other owner counts as shared with viewer`() {
        assertTrue(CoffeeAIProjectScope.isSharedWithViewer(avatar(owner = "bob", shared = true), "alice"))
        assertTrue(CoffeeAIProjectScope.isSharedWithViewer(avatar(owner = "bob", shared = false), "alice"))
        assertFalse(CoffeeAIProjectScope.isSharedWithViewer(avatar(owner = "alice", shared = false), "alice"))
    }

    @Test
    fun `personal pipeline only keeps own unbound avatars`() {
        val own = avatar(owner = "alice", shared = false)
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = own,
                userId = "alice",
                pipelineProjectId = "_alice",
                workspaceOwnerType = "PERSONAL"
            )
        )
        assertFalse(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "bob", shared = true),
                userId = "alice",
                pipelineProjectId = "_alice",
                workspaceOwnerType = "PERSONAL"
            )
        )
        assertFalse(
            CoffeeAIProjectScope.matchesProject(
                avatar = avatar(owner = "alice", bound = "team-proj"),
                userId = "alice",
                pipelineProjectId = "_alice",
                workspaceOwnerType = "PROJECT"
            )
        )
    }

    @Test
    fun `team pipeline only keeps avatars bound to that project`() {
        val boundOwn = avatar(owner = "alice", bound = "team-proj")
        val boundShared = avatar(owner = "bob", shared = true, bound = "team-proj")
        val otherBound = avatar(owner = "alice", bound = "other-proj")
        val personal = avatar(owner = "alice")
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = boundOwn,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "team-proj",
                workspaceOwnerType = "PROJECT"
            )
        )
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = boundShared,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "team-proj",
                workspaceOwnerType = "PROJECT"
            )
        )
        assertFalse(
            CoffeeAIProjectScope.matchesProject(
                avatar = otherBound,
                userId = "alice",
                pipelineProjectId = "team-proj"
            )
        )
        assertEquals(
            CoffeeAIProjectScope.REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT,
            CoffeeAIProjectScope.mismatchReason(
                avatar = personal,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "_alice",
                workspaceOwnerType = "PERSONAL"
            )
        )
        assertEquals(
            CoffeeAIProjectScope.REASON_AVATAR_BOUND_TO_OTHER_PROJECT,
            CoffeeAIProjectScope.mismatchReason(
                avatar = otherBound,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "other-proj",
                workspaceOwnerType = "PROJECT"
            )
        )
        assertNull(
            CoffeeAIProjectScope.mismatchReason(
                avatar = boundOwn,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "team-proj",
                workspaceOwnerType = "PROJECT"
            )
        )
    }

    @Test
    fun `workspace projectId fills missing selectedBoundProjectId`() {
        val shared = avatar(owner = "bob", shared = true)
        assertTrue(
            CoffeeAIProjectScope.matchesProject(
                avatar = shared,
                userId = "alice",
                pipelineProjectId = "team-proj",
                workspaceProjectId = "team-proj",
                workspaceOwnerType = "PROJECT"
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
