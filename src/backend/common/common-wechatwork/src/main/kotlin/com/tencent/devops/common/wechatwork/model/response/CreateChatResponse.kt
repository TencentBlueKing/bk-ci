package com.tencent.devops.common.wechatwork.model.response

data class CreateChatResponse(
    val errcode: Int,
    val errmsg: String,
    val chatid: String
)