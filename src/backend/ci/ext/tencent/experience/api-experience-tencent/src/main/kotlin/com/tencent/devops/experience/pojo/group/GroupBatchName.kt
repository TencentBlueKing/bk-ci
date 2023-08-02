package com.tencent.devops.experience.pojo.group

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("体验组--批量名称")
data class GroupBatchName(
    @ApiModelProperty("类型,1--内部人员,3--组织架构")
    val type: Int,
    @ApiModelProperty("名称列表")
    val names: List<String>
)
