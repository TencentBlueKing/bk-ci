package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "发送语音请求参数")
data class SendVoiceReq(
    @get:Schema(title = "自动语音读字信息")
    var auto_read_message: String,
    @get:Schema(title =
        "待通知的用户列表，自动语音通知列表，若user_list_information、receiver__username同时存在，" +
                "以user_list_information为准"
    )
    var user_list_information: Collection<String>? = null,
    @get:Schema(title =
        "待通知的用户列表，包含用户名，用户需在蓝鲸平台注册，多个以逗号分隔，" +
                "若user_list_information、receiver__username同时存在，以user_list_information为准"
    )
    var receiver__username: String? = null,

    override var bk_app_code: String? = "",
    override var bk_app_secret: String? = "",
    override var bk_token: String? = "",
    override var bk_username: String? = ""
) : ApiReq(
    bk_app_code, bk_app_secret, bk_token, bk_username
) {
    @Schema(title = "用户信息")
    data class UserListInformation(
        @get:Schema(title = "被通知人")
        var username: String,
        @get:Schema(title = "被通知人手机号")
        var mobile_phone: String? = null
    )
}
