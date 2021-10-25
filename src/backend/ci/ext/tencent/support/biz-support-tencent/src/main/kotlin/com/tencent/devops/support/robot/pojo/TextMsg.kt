package com.tencent.devops.support.robot.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class TextMsg(
    val content: String,

    @JsonProperty("mentioned_list")
    var mentionedList: List<String>? = mutableListOf(),

    @JsonProperty("mentioned_mobile_list")
    var mentionedMobileList: List<String>? = mutableListOf(),
)
