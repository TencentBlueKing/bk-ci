package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-搜索元数据")
data class SearchProps(
    @ApiModelProperty("匹配文件列表(支持模糊匹配)", required = true)
    val fileNames: List<String>?,
    @ApiModelProperty("元数据列表", required = true)
    val props: Map<String, String>
)