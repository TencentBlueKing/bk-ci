package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("权限批量校验实体")
data class PermissionBatchValidateDTO(
    @ApiModelProperty("资源类型", required = true)
    val resourceType: String,
    @ApiModelProperty("资源code", required = true)
    val resourceCode: String,
    @ApiModelProperty("action类型列表", required = true)
    val actionList: List<String>
)
