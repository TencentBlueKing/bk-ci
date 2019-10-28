package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("原因信息")
data class Reason(
    @ApiModelProperty("原因ID", required = true)
    val id: String,
    @ApiModelProperty("原因类型", required = true)
    val type: String,
    @ApiModelProperty("原因内容", required = true)
    val content: String,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("是否启用")
    val enable: Boolean,
    @ApiModelProperty("显示顺序")
    val order: Int
)