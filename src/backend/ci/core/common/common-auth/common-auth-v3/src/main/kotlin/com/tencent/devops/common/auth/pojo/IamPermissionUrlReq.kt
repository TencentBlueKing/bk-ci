package com.tencent.devops.common.auth.pojo

data class IamPermissionUrlReq(
    val system: String,
    val actions: List<Action>,
    override var bk_app_code: String,
    override var bk_app_secret: String,
    override var bk_username: String,
    override val bk_token: String = ""
) : IamBaseReq(bk_app_code, bk_app_secret, bk_username, bk_token)