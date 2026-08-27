package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup

/**
 * 创作流 Stage 审核与 imate 会话锁定的约定。
 *
 * CDS `sessionKey` 是完整 [agent2UserKey]：`userId|groupChatId|conversationId|`。
 * [conversationId] 只是其中第三段，二者不相等。创作流构建变量 `ci.imate_session_id`
 * 按既有 skill 约定保存 agent2UserKey（或带 OpenClaw 前缀的完整 Session，需取最后一个 `:` 之后）。
 */
object ImateStageReview {
    const val SAAS_ID = "bk-ci-creative-stream"
    const val GROUP_NAME = "IMATE"
    const val TASK_ID_PREFIX = "cs-stage"
    private const val TASK_ID_PARTS = 5

    /**
     * 从创作流保存的 session 值得到 CDS 使用的 agent2UserKey。
     * 含 `:` 时取最后一段（兼容 OpenClaw `agent:main:...:user|group|conv|`）；
     * 否则原样返回（已经是 agent2UserKey）。
     */
    fun toAgent2UserKey(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ""
        val colon = value.lastIndexOf(':')
        return if (colon >= 0) value.substring(colon + 1).trim() else value
    }

    /**
     * 从 agent2UserKey 取出 conversationId（第三段）。解析失败返回空串。
     */
    fun conversationIdOf(agent2UserKey: String): String {
        val parts = agent2UserKey.split("|", limit = 4)
        return parts.getOrNull(2).orEmpty()
    }

    fun isImateGroup(name: String?): Boolean = name?.trim() == GROUP_NAME

    fun isImateGroup(group: StageReviewGroup?): Boolean = isImateGroup(group?.name)

    fun needLock(
        channelCode: ChannelCode?,
        groupName: String?,
        imateSessionId: String?
    ): Boolean {
        if (channelCode != ChannelCode.CREATIVE_STREAM) return false
        if (!isImateGroup(groupName)) return false
        return toAgent2UserKey(imateSessionId).isNotBlank()
    }

    fun taskId(projectId: String, buildId: String, stageId: String, executeCount: Int): String {
        return listOf(TASK_ID_PREFIX, projectId, buildId, stageId, executeCount.toString()).joinToString("|")
    }

    fun parseTaskId(taskId: String): TaskIdParts? {
        val parts = taskId.trim().split("|")
        if (parts.size != TASK_ID_PARTS || parts[0] != TASK_ID_PREFIX) return null
        val executeCount = parts[4].toIntOrNull() ?: return null
        if (parts[1].isBlank() || parts[2].isBlank() || parts[3].isBlank()) return null
        return TaskIdParts(
            projectId = parts[1],
            buildId = parts[2],
            stageId = parts[3],
            executeCount = executeCount
        )
    }

    fun sessionMatches(boundSessionId: String?, cdsSessionKey: String?): Boolean {
        val bound = toAgent2UserKey(boundSessionId)
        val cds = toAgent2UserKey(cdsSessionKey)
        return bound.isNotBlank() && bound == cds
    }

    /**
     * IMATE 闸门裁决。调用方仅在 [needLock] 为 true 时使用本方法。
     */
    fun decideLock(
        configured: Boolean,
        queryFailed: Boolean,
        notFound: Boolean,
        isCancel: Boolean,
        cdsStatus: String?,
        cdsSessionKey: String?,
        boundSessionId: String?
    ): ImateLockDecision {
        if (!configured) return ImateLockDecision.NOT_CONFIGURED
        if (queryFailed) return ImateLockDecision.QUERY_FAILED
        if (notFound) return ImateLockDecision.NOT_LOCKED
        if (!sessionMatches(boundSessionId, cdsSessionKey)) return ImateLockDecision.SESSION_MISMATCH
        val status = cdsStatus?.uppercase()
        return when {
            isCancel && status == STATUS_REJECTED -> ImateLockDecision.PASS
            !isCancel && status == STATUS_APPROVED -> ImateLockDecision.PASS
            status == STATUS_REJECTED -> ImateLockDecision.REJECTED
            else -> ImateLockDecision.NOT_LOCKED
        }
    }

    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_REJECTED = "REJECTED"

    data class TaskIdParts(
        val projectId: String,
        val buildId: String,
        val stageId: String,
        val executeCount: Int
    )
}

enum class ImateLockDecision {
    PASS,
    NOT_LOCKED,
    REJECTED,
    SESSION_MISMATCH,
    QUERY_FAILED,
    NOT_CONFIGURED
}
