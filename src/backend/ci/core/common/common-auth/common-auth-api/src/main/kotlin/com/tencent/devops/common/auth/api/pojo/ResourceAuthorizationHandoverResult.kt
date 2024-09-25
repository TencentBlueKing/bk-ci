package com.tencent.devops.common.auth.api.pojo

import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权移交结果")
data class ResourceAuthorizationHandoverResult(
    @get:Schema(title = "状态")
    val status: ResourceAuthorizationHandoverStatus,
    @get:Schema(title = "信息")
    var message: String? = ""
)
