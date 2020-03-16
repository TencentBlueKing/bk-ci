package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("增删Media请求")
data class StoreMediaInfoRequest(
    @ApiModelProperty("研发商店类型", required = true)
    val storeCode: String,
    @ApiModelProperty("媒体url", required = true)
    val mediaUrl: String,
    @ApiModelProperty("媒体类型", required = true)
    val mediaType: String,
    @ApiModelProperty("修改用户", required = true)
    val modifier: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long
)