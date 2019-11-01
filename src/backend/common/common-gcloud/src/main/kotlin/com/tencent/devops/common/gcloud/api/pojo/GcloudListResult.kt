package com.tencent.devops.common.gcloud.api.pojo

data class GcloudListResult(
    val message: String,
    val code: Int,
    val result: List<Map<String, Any>>?
)