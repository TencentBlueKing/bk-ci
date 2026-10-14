package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.ImateAvatar

/**
 * 列表与运行范围以 CDS 共享开关为准，不看 remotedev 节点是否迁入项目。
 * 个人项目只保留未共享的个人分身；团队项目只保留已开启共享的分身（不限绑定到哪个项目）。
 * 个人项目：projectId 以 `_` 开头。
 */
object CoffeeAIProjectScope {
    const val REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT = "PERSONAL_AVATAR_IN_TEAM_PROJECT"
    const val REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT = "TEAM_AVATAR_IN_PERSONAL_PROJECT"

    fun isPersonalProject(projectId: String?): Boolean {
        return projectId?.trim()?.startsWith("_") == true
    }

    /**
     * CDS `shared=true` 表示已开启团队共享；旧数据没有该字段时，所属者不是当前用户也视为共享。
     */
    fun isSharedAvatar(avatar: ImateAvatar?, userId: String): Boolean {
        if (avatar == null) {
            return false
        }
        if (avatar.shared == true) {
            return true
        }
        val owner = avatar.owner?.trim().orEmpty()
        return owner.isNotEmpty() && owner != userId
    }

    fun matchesProject(
        avatar: ImateAvatar,
        userId: String,
        pipelineProjectId: String
    ): Boolean {
        return mismatchReason(avatar, userId, pipelineProjectId) == null
    }

    fun mismatchReason(
        avatar: ImateAvatar?,
        userId: String,
        pipelineProjectId: String?
    ): String? {
        val projectId = pipelineProjectId?.trim().orEmpty()
        if (projectId.isEmpty()) {
            return null
        }
        val shared = isSharedAvatar(avatar, userId)
        return if (isPersonalProject(projectId)) {
            if (shared) REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT else null
        } else {
            if (shared) null else REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT
        }
    }
}
