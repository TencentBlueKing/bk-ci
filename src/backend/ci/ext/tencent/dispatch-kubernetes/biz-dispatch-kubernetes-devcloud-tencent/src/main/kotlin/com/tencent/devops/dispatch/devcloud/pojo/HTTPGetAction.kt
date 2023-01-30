package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.bk.sdk.iam.constants.HttpHeader


data class HTTPGetAction(
    val path: String,
    val port: Int,
    val host: String? = null,
    val scheme: String = "HTTP",
    val httpHeaders: List<HttpHeader>
)
