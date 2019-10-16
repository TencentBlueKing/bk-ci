package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CMDB节点信息")
data class CmdbNode(
    @ApiModelProperty("节点名称", required = true)
    val name: String,
    @ApiModelProperty("责任人", required = true)
    val operator: String,
    @ApiModelProperty("备份责任人", required = true)
    val bakOperator: String,
    @ApiModelProperty("节点IP", required = true)
    val ip: String,
    @ApiModelProperty("显示IP", required = true)
    val displayIp: String,
    @ApiModelProperty("Agent状态", required = true)
    val agentStatus: Boolean,
    @ApiModelProperty("操作系统", required = true)
    val osName: String,
    @ApiModelProperty("所属业务")
    val bizId: Long = -1
)