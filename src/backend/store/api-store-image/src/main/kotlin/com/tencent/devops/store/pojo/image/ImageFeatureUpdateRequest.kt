package com.tencent.devops.store.pojo.image

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像市场-镜像特性信息更新请求报文体")
data class ImageFeatureUpdateRequest(
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,
    @ApiModelProperty("是否为公共镜像， TRUE：是 FALSE：不是", required = false)
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty("是否官方认证， TRUE：是 FALSE：不是", required = false)
    val certificationFlag: Boolean? = null
)