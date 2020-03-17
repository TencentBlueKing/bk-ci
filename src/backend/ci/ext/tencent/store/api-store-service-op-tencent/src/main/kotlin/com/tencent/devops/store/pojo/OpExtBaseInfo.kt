package com.tencent.devops.store.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpExtBaseInfo(
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("扩展点")
    val itemIds: Set<String>?,
    @ApiModelProperty("标签")
    val labels: List<String>?,
    @ApiModelProperty("简介")
    val summary: String? = null,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null
)