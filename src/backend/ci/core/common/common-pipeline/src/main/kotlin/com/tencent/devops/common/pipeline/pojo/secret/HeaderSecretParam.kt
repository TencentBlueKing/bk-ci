package com.tencent.devops.common.pipeline.pojo.secret

import okhttp3.Request

data class HeaderSecretParam(
    var headers: Map<String, String>
) : ISecretParam {
    override fun secret(builder: Request.Builder) {
        headers.forEach { header ->
            run {
                builder.header(header.key, header.value)
            }
        }
    }

    companion object {
        const val classType = "header"
    }
}