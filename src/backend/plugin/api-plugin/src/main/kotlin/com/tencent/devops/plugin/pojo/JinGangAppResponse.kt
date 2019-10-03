package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚扫面任务分页列表")
data class JinGangAppResponse(
    @ApiModelProperty("任务总数")
    val count: Int,
    @ApiModelProperty("当前页数")
    val page: Int,
    @ApiModelProperty("每页数量")
    val pageSize: Int,
    @ApiModelProperty("金刚扫面任务列表")
    val records: List<JinGangApp>,
    @ApiModelProperty("总页数")
    val totalPages: Int

)
