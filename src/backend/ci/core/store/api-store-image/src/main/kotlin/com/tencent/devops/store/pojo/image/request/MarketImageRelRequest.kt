package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.common.pipeline.type.docker.ImageType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("关联镜像请求报文体")
data class MarketImageRelRequest(
    @ApiModelProperty("项目编码", required = true)
    val projectCode: String,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: ImageType,
    @ApiModelProperty("ticket身份ID", required = false)
    val ticketId: String?
)