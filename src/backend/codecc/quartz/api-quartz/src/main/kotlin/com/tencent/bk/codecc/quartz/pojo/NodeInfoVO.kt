package com.tencent.bk.codecc.quartz.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("节点信息视图")
data class NodeInfoVO(
        @ApiModelProperty("节点序号")
        val nodeNum: Int,
        @ApiModelProperty("服务id")
        val serviceId: String,
        @ApiModelProperty("服务地址")
        val host: String,
        @ApiModelProperty("服务端口")
        val port: Int
)