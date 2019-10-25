package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件开发语言相关的环境变量")
data class AtomDevLanguageEnvVar(
    @ApiModelProperty("环境变量key值", required = true)
    val envKey: String,
    @ApiModelProperty("环境变量value值", required = true)
    val envValue: String,
    @ApiModelProperty("开发语言", required = true)
    val language: String,
    @ApiModelProperty("适用构建机类型", required = true)
    val buildHostType: String,
    @ApiModelProperty("适用构建机操作系统", required = true)
    val buildHostOs: String
)