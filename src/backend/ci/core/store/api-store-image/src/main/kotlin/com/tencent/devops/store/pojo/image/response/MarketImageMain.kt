package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("市场镜像分页")
data class MarketImageMain(

    @ApiModelProperty("镜像分类代码", required = true)
    val key: String,

    @ApiModelProperty("镜像分类名称", required = true)
    val label: String,

    @ApiModelProperty("MarketImageItem数组", required = true)
    val records: List<MarketImageItem>

)