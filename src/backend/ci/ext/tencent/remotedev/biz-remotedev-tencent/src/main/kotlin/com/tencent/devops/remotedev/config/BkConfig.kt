package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 蓝鲸应用相关配置
 */
@Component
class BkConfig {
    // 蓝鲸应用信息
    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    // cc 相关配置
    @Value("\${bkCC.host:}")
    val ccHost: String = ""

    @Value("\${bkCC.userName:}")
    val ccUserName: String = ""

    @Value("\${bkCC.bizId:#{null}}")
    val ccBizId: Int? = null

    // base 相关配置
    @Value("\${bkbase.url:}")
    val baseUrl: String = ""

    @Value("\${bkbase.token:}")
    val baseToken: String = ""
}