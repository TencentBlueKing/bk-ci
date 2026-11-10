package com.tencent.devops.common.security.pojo

data class CryptoKeyRefreshRequest(
    val writer: String? = null,
    val filters: Map<String, String>? = null
)
