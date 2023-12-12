package com.tencent.devops.environment.pojo.job.req

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行目标信息")
data class ExecuteTarget(
    @ApiModelProperty(value = "环境hashId列表")
    val envHashIdList: List<String>?,
    @ApiModelProperty(value = "节点hashId列表")
    val nodeHashIdList: List<String>?,
    @ApiModelProperty(value = "主机IP信息列表")
    val ipList: List<IpInfo>?,
    @ApiModelProperty(value = "主机ID列表")
    val hostIdList: List<Long>?
)