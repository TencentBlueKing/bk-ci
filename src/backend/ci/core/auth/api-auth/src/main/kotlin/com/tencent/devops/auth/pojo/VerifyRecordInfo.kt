package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("鉴权记录实体")
data class VerifyRecordInfo(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("资源Code")
    val resourceCode: String,
    @ApiModelProperty("操作")
    val action: String,
    @ApiModelProperty("环境")
    val environmentTag: String,
    @ApiModelProperty("鉴权结果")
    val verifyResult: Boolean
)
