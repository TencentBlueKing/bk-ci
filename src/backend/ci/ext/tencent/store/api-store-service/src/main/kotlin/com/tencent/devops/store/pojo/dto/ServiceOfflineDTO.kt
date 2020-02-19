package com.tencent.devops.store.pojo.dto

import io.swagger.annotations.ApiModelProperty

data class ServiceOfflineDTO(
    @ApiModelProperty("下架缓冲期，单位：天")
    val bufferDay: Byte,
    @ApiModelProperty("下架原因")
    val reason: String?
)