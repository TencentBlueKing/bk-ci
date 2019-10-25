package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("卸载插件请求包体")
data class unInstallReq(
    @ApiModelProperty("原因列表")
    val reasonList: List<unInstallReason?>
)

data class unInstallReason(
    @ApiModelProperty("原因ID")
    val reasonId: String,
    @ApiModelProperty("原因说明")
    val note: String?
)