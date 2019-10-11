package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Gcloud")
data class GcloudConfResponse(
    @ApiModelProperty("总数")
    val count: String,
    @ApiModelProperty("当前页数")
    val page: Int,
    @ApiModelProperty("每页数量")
    val pageSize: Int,
    @ApiModelProperty("配置详情")
    val records: List<GcloudConf>,
    @ApiModelProperty("总页数")
    val totalPages: Int

)
