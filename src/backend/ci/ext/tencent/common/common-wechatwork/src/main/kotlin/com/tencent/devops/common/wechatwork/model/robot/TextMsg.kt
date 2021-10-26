package com.tencent.devops.common.wechatwork.model.robot

import com.fasterxml.jackson.annotation.JsonProperty

data class TextMsg(
    val content: String,

    @JsonProperty("mentioned_list")
    var mentionedList: List<String>? = mutableListOf(),

    @JsonProperty("mentioned_mobile_list")
    var mentionedMobileList: List<String>? = mutableListOf(),
)
