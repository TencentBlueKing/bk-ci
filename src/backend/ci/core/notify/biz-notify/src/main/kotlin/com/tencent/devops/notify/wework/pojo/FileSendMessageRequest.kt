package com.tencent.devops.notify.wework.pojo

data class FileSendMessageRequest(
    override val agentId: Int,
    override val duplicateCheckInterval: Int?,
    override val enableDuplicateCheck: Int?,
    override val safe: Int?,
    override val toParty: String,
    override val toTag: String,
    override val toUser: String,
    val file: MediaMessageContent
) : AbstractSendMessageRequest(
    agentId = agentId,
    duplicateCheckInterval = duplicateCheckInterval,
    enableDuplicateCheck = enableDuplicateCheck,
    msgType = "file",
    safe = safe,
    toParty = toParty,
    toTag = toTag,
    toUser = toUser
)
