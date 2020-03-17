package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.common.enums.MediaTypeEnum
import io.swagger.annotations.ApiModelProperty

data class OpMediaInfo(
    @ApiModelProperty("媒体url", required = true)
    val mediaUrl: String,
    @ApiModelProperty("媒体类型", required = true)
    val mediaType: MediaTypeEnum
)