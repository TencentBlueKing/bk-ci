package com.tencent.devops.notify.blueking.sdk.pojo

class ApiResp(
        val message: String? = "",
        val code: Int? = -1,
        val result: Boolean? = false,
        val request_id: String? = "",
        val data: Any? = null
)