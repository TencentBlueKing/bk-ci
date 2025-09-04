package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "remote dev 回调")
data class RemoteDevCallBack(
    @get:Schema(title = "请求 ID，第三方自定义，每次请求唯一，用于幂等性处理")
    val requestId: String,
    @get:Schema(title = "用户 id")
    val userId: String,
    @get:Schema(title = "时间戳，毫秒")
    val timestamp: Long,
    @get:Schema(title = "事件类型")
    val event: String,
    @get:Schema(title = "具体参数")
    val ext: RemoteDevCallBackExt?
)

@Schema(title = "remote dev 回调-具体参数")
data class RemoteDevCallBackExt(
    @get:Schema(title = "环境名")
    val name: String,
    @get:Schema(title = "环境状态")
    val status: String
)
