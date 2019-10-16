package com.tencent.ops.common.wechatwork

import org.springframework.stereotype.Component

@Component
data class WechatWorkConfiguration(
    val corpId: String? = null,
    val serviceId: String? = null,
    val secret: String? = null,
    val token: String? = null,
    val aesKey: String? = null,
    val url: String? = null
)