package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("研发商店-工作台-插件语言")
data class AtomDevLanguage(
    @ApiModelProperty("语言")
    val language: String
)