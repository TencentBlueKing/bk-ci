package com.tencent.devops.common.notify.blueking.sdk.pojo

abstract class ApiReq(
        open var bk_app_code: String?,
        open var bk_app_secret: String?,
        open var bk_token: String?,
        open val bk_username: String?
)