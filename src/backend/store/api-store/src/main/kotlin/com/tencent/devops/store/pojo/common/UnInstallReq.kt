package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("卸载插件请求包体")
data class UnInstallReq(
    @ApiModelProperty("原因列表")
    val reasonList: List<UnInstallReason?>
)