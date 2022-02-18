package com.tencent.devops.notify.wework.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class TextSendMessageRequest(
    override val agentId: Int,
    override val duplicateCheckInterval: Int?,
    override val enableDuplicateCheck: Int?,
    /**
     *  表示是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
     */
    @JsonProperty("enable_id_trans")
    val enableIdTrans: Int? = null,
    override val safe: Int?,
    override val toParty: String,
    override val toTag: String,
    override val toUser: String,
    val text: TextMessageContent
) : AbstractSendMessageRequest(
    agentId = agentId,
    duplicateCheckInterval = duplicateCheckInterval,
    enableDuplicateCheck = enableDuplicateCheck,
    msgType = "text",
    safe = safe,
    toParty = toParty,
    toTag = toTag,
    toUser = toUser
)

data class TextMessageContent(
    /**
     *  消息内容，最长不超过2048个字节，超过将截断（支持id转译）
     */
    val content: String
)
