package com.tencent.devops.store.pojo.image.response

import com.tencent.devops.store.pojo.common.MarketItem
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("模版市场-搜索结果")
data class MarketImageResp(
    @ApiModelProperty("总记录数")
    val count: Int,
    @ApiModelProperty("当前页码值")
    val page: Int?,
    @ApiModelProperty("每页记录大小")
    val pageSize: Int?,
    @ApiModelProperty("数据集合")
    val records: List<MarketItem?>
)