package com.tencent.devops.notify.wework.pojo

data class VoiceSendMessageRequest(
    override val agentId: Int,
    override val duplicateCheckInterval: Int?,
    override val enableDuplicateCheck: Int?,
    override val safe: Int?,
    override val toParty: String,
    override val toTag: String,
    override val toUser: String,
    val voice: MediaMessageContent
) : AbstractSendMessageRequest(
    agentId = agentId,
    duplicateCheckInterval = duplicateCheckInterval,
    enableDuplicateCheck = enableDuplicateCheck,
    msgType = "voice",
    safe = safe,
    toParty = toParty,
    toTag = toTag,
    toUser = toUser
)
