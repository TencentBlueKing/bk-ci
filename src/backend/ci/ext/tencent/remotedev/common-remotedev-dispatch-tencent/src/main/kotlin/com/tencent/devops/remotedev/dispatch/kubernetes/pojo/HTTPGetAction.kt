package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.tencent.bk.sdk.iam.constants.HttpHeader

data class HTTPGetAction(
    val path: String,
    val port: Int,
    val host: String? = null,
    val scheme: String = "HTTP",
    val httpHeaders: List<HttpHeader>
)
