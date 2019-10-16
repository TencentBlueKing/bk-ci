package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CC节点信息")
data class CcNode(
    @ApiModelProperty("节点名称", required = true)
    val name: String,
    @ApiModelProperty("固资编号", required = true)
    val assetID: String,
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
    val osName: String
)