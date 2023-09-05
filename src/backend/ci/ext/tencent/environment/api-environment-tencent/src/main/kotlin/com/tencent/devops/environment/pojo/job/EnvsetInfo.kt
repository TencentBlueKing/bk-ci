package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("环境集合信息")
data class EnvsetInfo(
    @ApiModelProperty(value = "环境hashId集合", required = true)
    val envHashIds: List<String>,
    @ApiModelProperty(value = "节点hashId集合", required = true)
    val nodeHashIds: List<String>,
    @ApiModelProperty(value = "IP集合", required = true)
    val ipList: List<IPInfo>
)