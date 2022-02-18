package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("编译加速选择下拉框视图")
data class TurboListSelectVO(
    @ApiModelProperty("编译加速方案信息")
    var planInfo: Map<String, String?>,
    @ApiModelProperty("流水线信息")
    var pipelineInfo: Map<String, String?>,
    @ApiModelProperty("客户端ip信息")
    var clientIpInfo: List<String>,
    @ApiModelProperty("状态信息")
    var statusInfo: Map<String, String?>
)
