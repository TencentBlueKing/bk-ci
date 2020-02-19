package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class StoreServiceItem(
    @ApiModelProperty("扩展项目根Id", required = true)
    val parentItemId: String,
    @ApiModelProperty("扩展项目根code", required = true)
    val parentItemCode: String,
    @ApiModelProperty("扩展项目根名称", required = true)
    val parentItemName: String,
    @ApiModelProperty("扩展项目二级Id", required = true)
    val childItemId: String,
    @ApiModelProperty("扩展项目二级code", required = true)
    val childItemCode: String,
    @ApiModelProperty("扩展项目二级名称", required = true)
    val childItemName: String
)