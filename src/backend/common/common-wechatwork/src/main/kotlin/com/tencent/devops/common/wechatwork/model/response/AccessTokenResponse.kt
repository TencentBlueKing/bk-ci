package com.tencent.devops.common.wechatwork.model.response

data class AccessTokenResponse(
    val errcode: Int,
    val errmsg: String,
    val access_token: String,
    val expires_in: Int
)