package com.tencent.devops.store.pojo.container

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("编译环境")
data class ContainerType(
    @ApiModelProperty("流水线容器类型", required = true)
    var type: String,
    @ApiModelProperty("操作系统", required = true)
    var info: List<Map<String, String>>
)
