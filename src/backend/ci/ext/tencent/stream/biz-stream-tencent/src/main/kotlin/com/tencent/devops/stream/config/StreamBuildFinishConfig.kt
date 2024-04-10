package com.tencent.devops.stream.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StreamBuildFinishConfig {
    @Value("\${rtx.corpid:#{null}}")
    val corpId: String? = null

    @Value("\${rtx.corpsecret:#{null}}")
    val corpSecret: String? = null

    @Value("\${rtx.url:#{null}}")
    val rtxUrl: String? = null

    @Value("\${rtx.gitUrl:#{null}}")
    val gitUrl: String? = null

    @Value("\${rtx.v2GitUrl:#{null}}")
    val v2GitUrl: String? = null
}
