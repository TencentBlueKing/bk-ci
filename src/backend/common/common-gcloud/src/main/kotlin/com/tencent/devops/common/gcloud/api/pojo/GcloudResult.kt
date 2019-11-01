package com.tencent.devops.common.gcloud.api.pojo

data class GcloudResult(
    val message: String,
    val code: Int,
    val result: Map<String, Any>?
)