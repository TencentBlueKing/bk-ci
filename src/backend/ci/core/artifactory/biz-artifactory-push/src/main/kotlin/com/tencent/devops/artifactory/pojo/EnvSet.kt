package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("目标机器与文件源机器")
data class EnvSet(
    @ApiModelProperty("目标环境hashId串")
    val envHashIds: List<String>,
    @ApiModelProperty("目标节点hashId串")
    val nodeHashIds: List<String>,
    @ApiModelProperty("源文件所在ip")
    val ipLists: List<IpDto>
) {
    data class IpDto(
        val ip: String,
        val source: Int = 1
    )
}