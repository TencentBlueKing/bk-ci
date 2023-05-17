package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.RelatedResourceInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("组权限详情")
data class GroupPermissionDetailVo(
    @ApiModelProperty("操作id")
    val actionId: String,
    @ApiModelProperty("操作名")
    val name: String,
    @ApiModelProperty("关联资源")
    val relatedResourceInfo: RelatedResourceInfo
)
