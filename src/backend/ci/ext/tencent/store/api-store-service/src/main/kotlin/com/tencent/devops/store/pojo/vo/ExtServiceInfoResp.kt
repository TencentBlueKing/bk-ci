package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceInfoResp(
    @ApiModelProperty("总记录数", required = true)
    val count: Int,
    @ApiModelProperty("当前页码值", required = false)
    val page: Int?,
    @ApiModelProperty("每页记录大小", required = false)
    val pageSize: Int?,
    @ApiModelProperty("数据集合", required = false)
    val records: Set<ExtensionServiceVO?>
)