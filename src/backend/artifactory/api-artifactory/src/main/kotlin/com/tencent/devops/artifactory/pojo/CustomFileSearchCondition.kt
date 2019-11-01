package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自定义仓库查询条件")
data class CustomFileSearchCondition(
    @ApiModelProperty("通配符", required = false)
    val glob: String?,
    @ApiModelProperty("元数据", required = true)
    val properties: Map<String, String>
)