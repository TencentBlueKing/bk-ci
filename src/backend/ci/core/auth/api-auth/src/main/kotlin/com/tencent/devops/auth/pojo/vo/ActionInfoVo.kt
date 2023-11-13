package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("操作实体")
data class ActionInfoVo(
    @ApiModelProperty("action")
    val action: String,
    @ApiModelProperty("动作名")
    @BkFieldI18n
    val actionName: String,
    @ApiModelProperty("蓝盾-关联资源类型")
    val resourceType: String,
    @ApiModelProperty("IAM-关联资源类型")
    val relatedResourceType: String
)
