package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.annotations.ApiModel

@ApiModel("微信发送模型")
data class SendWxReq(
    val receiver: String?,
    val receiver__username: String?,
    val data: Data,

    override var bk_app_code: String? = "",
    override var bk_app_secret: String? = "",
    override var bk_token: String? = "",
    override var bk_username: String? = ""
) : ApiReq(bk_app_code, bk_app_secret, bk_token, bk_username) {
    data class Data(
            val heading: String? = "",
            val message: String? = "",
            val date: String? = "",
            val remark: String? = "",
            val is_message_base64: String? = ""
    )
}

// heading	string	是	通知头部文字
// message	string	是	通知文字
// date	string	否	通知发送时间，默认为当前时间 "YYYY-mm-dd HH:MM"
// remark	string	否	通知尾部文字
// is_message_base64	bool	否	通知文字message是否base64编码，默认False，不编码，若编码请使用base64.b64encode方法