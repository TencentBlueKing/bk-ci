package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.annotations.ApiModel

@ApiModel("企业微信发送模型")
data class SendQyWxReq(
        val content: String,

        /**
         * 微信接收者，包含企业微信用户ID，多个以逗号分隔（这里只支持企业微信id）
         */
        val receiver: String?,

        override var bk_app_code: String? = "",
        override var bk_app_secret: String? = "",
        override var bk_token: String? = "",
        override var bk_username: String? = ""
) : ApiReq(bk_app_code, bk_app_secret, bk_token, bk_username)