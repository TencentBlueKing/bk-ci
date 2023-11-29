package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发送语音请求参数")
data class SendVoiceReq(
    @ApiModelProperty("自动语音读字信息")
    var auto_read_message: String,
    @ApiModelProperty(
        "待通知的用户列表，自动语音通知列表，若user_list_information、receiver__username同时存在，" +
                "以user_list_information为准"
    )
    var user_list_information: Collection<String>? = null,
    @ApiModelProperty(
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
    @ApiModel("用户信息")
    data class UserListInformation(
        @ApiModelProperty("被通知人")
        var username: String,
        @ApiModelProperty("被通知人手机号")
        var mobile_phone: String? = null
    )
}
