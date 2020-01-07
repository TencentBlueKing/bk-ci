package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateExtensionServiceDTO(
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val category: String,
    @ApiModelProperty("服务版本")
    val version: String,
    @ApiModelProperty("状态")
    val status: Int,
    @ApiModelProperty("状态对应的描述")
    val statusMsg: String?,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("ICON")
    val icon: String?,
    @ApiModelProperty("扩展服务简介")
    val sunmmary: String?,
    @ApiModelProperty("扩展服务描述")
    val description: String?,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String?,
    @ApiModelProperty("发布时间")
    val publishTime: Long,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Int
)