package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件市场-申请成为插件协作者请求报文体")
data class AtomCollaboratorCreateReq(
    @ApiModelProperty("调试项目编码", required = true)
    val testProjectCode: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("申请原因", required = true)
    val applyReason: String
)