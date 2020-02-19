package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class ServiceVersionListResp(
    @ApiModelProperty("总记录数", required = true)
    val count: Int,
    @ApiModelProperty("数据集合", required = false)
    val records: List<ServiceVersionListItem?>
)