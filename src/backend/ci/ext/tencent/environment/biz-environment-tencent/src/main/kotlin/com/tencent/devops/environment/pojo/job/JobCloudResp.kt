package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel

@ApiModel("请求上云版job接口的返回")
data class JobCloudResp<T>(
    var code: Int,
    var result: Boolean,
    var jobRequestId: String,
    var message: String?,
    var data: T? = null
)