package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("增删logo请求")
data class StoreLogoReq(
    @ApiModelProperty("logo链接")
    val logoUrl: String,
    @ApiModelProperty("logo展示顺序")
    val order: Int,
    @ApiModelProperty("点击logo后的跳转链接")
    val link: String?
)