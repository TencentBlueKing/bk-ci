package com.tencent.devops.common.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gongfeng.code")
data class GongfengAuthProperties (
    val path : String? = null,
    val privateToken : String? = null
)