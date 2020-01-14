package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModelProperty

data class StoreMediaInfo (
    @ApiModelProperty("媒体id")
    val id: String,
    @ApiModelProperty("研发商店类型", required = true)
    val storeCode: String,
    @ApiModelProperty("媒体url", required = true)
    val mediaUrl: String,
    @ApiModelProperty("媒体类型", required = true)
    val mediaType: String,
    @ApiModelProperty("添加用户", required = true)
    val create: String,
    @ApiModelProperty("修改用户", required = true)
    val modifier: String,
    @ApiModelProperty("添加时间", required = true)
    val createTime: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: String
)