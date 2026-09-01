package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect

/**
 * 数字分身列表与 inspect 的纯函数，供构建态提前拦「只有云桌面、没有入驻 Agent」。
 */
object CoffeeAIAvatarSupport {
    const val REASON_NO_DIGITAL_AVATAR = "NO_DIGITAL_AVATAR"

    fun displayNameOf(avatar: ImateAvatar): String {
        return avatar.displayName?.takeIf { it.isNotBlank() }
            ?: avatar.agentName?.takeIf { it.isNotBlank() }
            ?: avatar.workspaceName
    }

    fun matchByWorkspace(avatars: List<ImateAvatar>, workspaceName: String): ImateAvatar? {
        if (workspaceName.isBlank()) {
            return null
        }
        return avatars.firstOrNull { it.workspaceName == workspaceName }
    }

    fun markMissingAvatar(inspect: ImateWorkspaceInspect): ImateWorkspaceInspect {
        if (!inspect.usable) {
            return inspect
        }
        return inspect.copy(usable = false, reason = REASON_NO_DIGITAL_AVATAR)
    }
}
