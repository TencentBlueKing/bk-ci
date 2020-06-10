package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像详情")
data class SimpleImageInfo(
    @ApiModelProperty("镜像代码", required = true)
    val code: String,

    @ApiModelProperty("镜像名称", required = true)
    val name: String,

    @ApiModelProperty("镜像版本", required = true)
    val version: String,

    @ApiModelProperty("是否推荐", required = true)
    val recommendFlag: Boolean
)
