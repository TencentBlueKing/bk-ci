package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("操作实体")
data class ActionInfoVo(
    @ApiModelProperty("actionId")
    val actionId: String,
    @ApiModelProperty("动作名")
    val actionName: String,
    @ApiModelProperty("动作归属的资源")
    val resourceType: String
)
