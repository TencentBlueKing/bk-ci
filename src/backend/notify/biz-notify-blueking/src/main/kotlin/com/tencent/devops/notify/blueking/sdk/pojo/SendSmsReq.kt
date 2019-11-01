package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.annotations.ApiModel

@ApiModel("短信发送模型")
data class SendSmsReq(
        val content: String,
        val receiver: String?,
        val receiver__username: String?,
        val is_content_base64: String?,

        override var bk_app_code: String? = "",
        override var bk_app_secret: String? = "",
        override var bk_token: String? = "",
        override var bk_username: String? = ""
) : ApiReq(bk_app_code, bk_app_secret, bk_token, bk_username)

// receiver	string	否	短信接收者，包含接收者电话号码，多个以逗号分隔，若receiver、receiver__username同时存在，以receiver为准
// receiver__username	string	否	短信接收者，包含用户名，用户需在蓝鲸平台注册，多个以逗号分隔，若receiver、receiver__username同时存在，以receiver为准
// content	string	是	短信内容
// is_content_base64	bool	否	消息内容是否base64编码，默认False，不编码，请使用base64.b64encode方法编码