package com.tencent.devops.environment.pojo.job

data class JobCloudResp<T>(
    var code: Int,
    var result: Boolean,
    var jobRequestId: String,
    var message: String?,
    var data: T? = null
)