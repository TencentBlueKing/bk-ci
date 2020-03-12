package com.tencent.devops.store.pojo.vo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtensionServiceVO(
    @ApiModelProperty("扩展服务Id")
    val serviceId: String,
    @ApiModelProperty("扩展服务名称")
    val serviceName: String,
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("版本")
    val version: String,
    @ApiModelProperty("调试项目Code")
    val projectCode: String,
//    @ApiModelProperty("标签")
//    val lable: String,
    @ApiModelProperty("状态")
    val serviceStatus: Int,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String?,
    @ApiModelProperty("修改时间")
    val modifierTime: String
)