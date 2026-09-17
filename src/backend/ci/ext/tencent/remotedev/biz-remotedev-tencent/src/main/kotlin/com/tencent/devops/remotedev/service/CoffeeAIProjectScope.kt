package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType

/**
 * 个人项目只列个人分身；团队项目只列已绑定到该项目的分身（所属者绑定节点或共享给当前用户）。
 * 个人项目：projectId 以 `_` 开头。
 */
object CoffeeAIProjectScope {
    const val REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT = "PERSONAL_AVATAR_IN_TEAM_PROJECT"
    const val REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT = "TEAM_AVATAR_IN_PERSONAL_PROJECT"
    const val REASON_AVATAR_NOT_BOUND_TO_PROJECT = "AVATAR_NOT_BOUND_TO_PROJECT"
    const val REASON_AVATAR_BOUND_TO_OTHER_PROJECT = "AVATAR_BOUND_TO_OTHER_PROJECT"

    fun isPersonalProject(projectId: String?): Boolean {
        return projectId?.trim()?.startsWith("_") == true
    }

    fun isSharedWithViewer(avatar: ImateAvatar, userId: String): Boolean {
        if (avatar.shared == true) {
            return true
        }
        val owner = avatar.owner?.trim().orEmpty()
        return owner.isNotEmpty() && owner != userId
    }

    fun boundProjectId(avatar: ImateAvatar?, workspaceProjectId: String?): String? {
        val fromAvatar = avatar?.selectedBoundProjectId?.trim()?.takeIf { it.isNotEmpty() }
        if (fromAvatar != null) {
            return fromAvatar
        }
        val fromWorkspace = workspaceProjectId?.trim()?.takeIf { it.isNotEmpty() && it != "NO_CHECK" }
        if (fromWorkspace != null && !isPersonalProject(fromWorkspace)) {
            return fromWorkspace
        }
        return null
    }

    fun matchesProject(
        avatar: ImateAvatar,
        userId: String,
        pipelineProjectId: String,
        workspaceProjectId: String? = null,
        workspaceOwnerType: String? = null
    ): Boolean {
        return mismatchReason(
            avatar = avatar,
            userId = userId,
            pipelineProjectId = pipelineProjectId,
            workspaceProjectId = workspaceProjectId,
            workspaceOwnerType = workspaceOwnerType
        ) == null
    }

    fun mismatchReason(
        avatar: ImateAvatar?,
        userId: String,
        pipelineProjectId: String?,
        workspaceProjectId: String?,
        workspaceOwnerType: String?
    ): String? {
        val projectId = pipelineProjectId?.trim().orEmpty()
        if (projectId.isEmpty()) {
            return null
        }
        val personalPipeline = isPersonalProject(projectId)
        val bound = boundProjectId(avatar, workspaceProjectId)
        val shared = avatar?.let { isSharedWithViewer(it, userId) } == true
        val personalWorkspace = workspaceOwnerType.isNullOrBlank() ||
            workspaceOwnerType == WorkspaceOwnerType.PERSONAL.name
        return if (personalPipeline) {
            when {
                shared -> REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT
                bound != null -> REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT
                !personalWorkspace -> REASON_TEAM_AVATAR_IN_PERSONAL_PROJECT
                else -> null
            }
        } else {
            when {
                bound.isNullOrBlank() -> {
                    if (personalWorkspace && !shared) {
                        REASON_PERSONAL_AVATAR_IN_TEAM_PROJECT
                    } else {
                        REASON_AVATAR_NOT_BOUND_TO_PROJECT
                    }
                }
                bound != projectId -> REASON_AVATAR_BOUND_TO_OTHER_PROJECT
                else -> null
            }
        }
    }
}
