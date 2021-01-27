package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("机型筛选项列表")
data class WeTestSectionList(
    @ApiModelProperty("筛选项信息")
    val records: List<WeTestSections>,
    @ApiModelProperty("数量")
    val count: Int
)
