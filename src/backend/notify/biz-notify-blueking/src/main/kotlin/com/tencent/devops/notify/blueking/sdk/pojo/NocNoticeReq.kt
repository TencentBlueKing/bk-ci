package com.tencent.devops.notify.blueking.sdk.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

@ApiModel("公共语音通知模型")
data class NocNoticeReq(
    val auto_read_message: String,
    val receiver__username: String,
    val user_list_information: List<User>?,

    override var bk_app_code: String? = "",
    override var bk_app_secret: String? = "",
    override var bk_token: String? = "",
    override var bk_username: String? = ""
) : ApiReq(bk_app_code, bk_app_secret, bk_token, bk_username) {
    data class User(
            @JsonProperty("username")
            val username: String,
            @JsonProperty("mobile_phone")
            val mobilePhone: String
    )
}