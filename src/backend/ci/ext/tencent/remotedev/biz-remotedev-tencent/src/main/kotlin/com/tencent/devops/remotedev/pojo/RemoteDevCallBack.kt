package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("remote dev 回调")
data class RemoteDevCallBack(
    @ApiModelProperty("请求 ID，第三方自定义，每次请求唯一，用于幂等性处理")
    val requestId: String,
    @ApiModelProperty("用户 id")
    val userId: String,
    @ApiModelProperty("时间戳，毫秒")
    val timestamp: Long,
    @ApiModelProperty("事件类型")
    val event: String,
    @ApiModelProperty("具体参数")
    val ext: RemoteDevCallBackExt?
)

@ApiModel("remote dev 回调-具体参数")
data class RemoteDevCallBackExt(
    @ApiModelProperty("环境名")
    val name: String,
    @ApiModelProperty("环境状态")
    val status: String
)
