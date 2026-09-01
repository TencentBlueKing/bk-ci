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

    private fun avatar(
        id: String = "av-1",
        workspaceName: String = "ins-1",
        displayName: String? = null,
        agentName: String? = null
    ) = ImateAvatar(
        digitalAvatarId = id,
        agentId = "agent-$id",
        agentName = agentName,
        workspaceName = workspaceName,
        displayName = displayName
    )

    private fun inspect(usable: Boolean, reason: String?) = ImateWorkspaceInspect(
        workspaceName = "ins-1",
        exists = true,
        belongsToUser = true,
        usable = usable,
        reason = reason
    )
}
