package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件市场-申请成为插件协作者请求报文体")
data class AtomCollaboratorCreateResp(
    @ApiModelProperty("申请人", required = true)
    val applicant: String,
    @ApiModelProperty("申请状态", required = true)
    val approveStatus: String
)