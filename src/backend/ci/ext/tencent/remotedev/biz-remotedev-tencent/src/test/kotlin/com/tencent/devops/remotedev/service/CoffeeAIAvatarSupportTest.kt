package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CoffeeAIAvatarSupportTest {

    @Test
    fun `displayName falls back to agentName then workspaceName`() {
        assertEquals(
            "展示名",
            CoffeeAIAvatarSupport.displayNameOf(avatar(displayName = "展示名", agentName = "agent"))
        )
        assertEquals(
            "agent-a",
            CoffeeAIAvatarSupport.displayNameOf(avatar(displayName = "  ", agentName = "agent-a"))
        )
        assertEquals(
            "ins-1",
            CoffeeAIAvatarSupport.displayNameOf(avatar(displayName = null, agentName = null))
        )
    }

    @Test
    fun `matchByWorkspace returns first avatar on that desktop`() {
        val avatars = listOf(
            avatar(id = "av-other", workspaceName = "ins-other"),
            avatar(id = "av-hit", workspaceName = "ins-1", agentName = "虾")
        )
        assertEquals("av-hit", CoffeeAIAvatarSupport.matchByWorkspace(avatars, "ins-1")?.digitalAvatarId)
        assertNull(CoffeeAIAvatarSupport.matchByWorkspace(avatars, "ins-missing"))
        assertNull(CoffeeAIAvatarSupport.matchByWorkspace(avatars, ""))
    }

    @Test
    fun `markMissingAvatar only flips otherwise usable inspect`() {
        val usable = inspect(usable = true, reason = null)
        val marked = CoffeeAIAvatarSupport.markMissingAvatar(usable)
        assertFalse(marked.usable)
        assertEquals(CoffeeAIAvatarSupport.REASON_NO_DIGITAL_AVATAR, marked.reason)

        val alreadyBad = inspect(usable = false, reason = "NOT_RUNNING")
        assertEquals("NOT_RUNNING", CoffeeAIAvatarSupport.markMissingAvatar(alreadyBad).reason)
        assertFalse(CoffeeAIAvatarSupport.markMissingAvatar(alreadyBad).usable)
    }

    @Test
    fun `hermes chatReady false marks otherwise usable inspect`() {
        val marked = CoffeeAIAvatarSupport.applyAvatarReadiness(
            inspect(usable = true, reason = null),
            avatar(chatReady = false, pluginReady = false, agentMode = "HERMES")
        )
        assertFalse(marked.usable)
        assertEquals(CoffeeAIAvatarSupport.REASON_AGENT_CHAT_NOT_READY, marked.reason)
        assertEquals(false, marked.chatReady)
        assertEquals("HERMES", marked.agentMode)
    }

    @Test
    fun `null chatReady does not flip usable`() {
        val kept = CoffeeAIAvatarSupport.applyAvatarReadiness(
            inspect(usable = true, reason = null),
            avatar(chatReady = null, agentMode = "OPENCLAW")
        )
        assertEquals(true, kept.usable)
        assertEquals(null, kept.reason)
        assertEquals("OPENCLAW", kept.agentMode)
    }

    private fun avatar(
        id: String = "av-1",
        workspaceName: String = "ins-1",
        displayName: String? = null,
        agentName: String? = null,
        agentMode: String? = null,
        chatReady: Boolean? = null,
        pluginReady: Boolean? = null
    ) = ImateAvatar(
        digitalAvatarId = id,
        agentId = "agent-$id",
        agentName = agentName,
        workspaceName = workspaceName,
        displayName = displayName,
        agentMode = agentMode,
        chatReady = chatReady,
        pluginReady = pluginReady
    )

    private fun inspect(usable: Boolean, reason: String?) = ImateWorkspaceInspect(
        workspaceName = "ins-1",
        exists = true,
        belongsToUser = true,
        usable = usable,
        reason = reason
    )
}
