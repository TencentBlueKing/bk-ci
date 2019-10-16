package com.tencent.devops.common.wechatwork.model.response

data class UserIdsConvertResponse(
    val errcode: Int,
    val errmsg: String,
    val user_list: List<UserIdNameResponse>
)