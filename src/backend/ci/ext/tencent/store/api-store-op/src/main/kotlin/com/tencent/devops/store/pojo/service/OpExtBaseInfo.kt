package com.tencent.devops.store.pojo.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpExtBaseInfo(
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("扩展点")
    val itemIds: List<String>?,
    @ApiModelProperty("标签")
    val lables: List<String>?,
    @ApiModelProperty("简介")
    val sunmmary: String? = null,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null
)