package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("媒体信息添加类")
data class MediaInfoReq (
    @ApiModelProperty("研发商店类型", required = true)
    val storeCode: String,
    @ApiModelProperty("媒体url", required = true)
    val mediaUrl: String,
    @ApiModelProperty("媒体类型", required = true)
    val mediaType: String
)