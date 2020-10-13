package com.tencent.devops.common.auth.pojo

data class IamCreateApiReq(
    val system: String,
    val type: String,
    val id: String,
    val name: String,
    val creator: String,
    val ancestors: List<AncestorsApiReq>? = emptyList(),
    override var bk_app_code: String,
    override var bk_app_secret: String,
    override var bk_username: String,
    override val bk_token: String = ""
) : IamBaseReq(bk_app_code, bk_app_secret, bk_username, bk_token)