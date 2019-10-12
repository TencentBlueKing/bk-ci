package com.tencent.devops.project.pojo.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目道代码")
enum class ProjectChannelCode {
    @ApiModelProperty("蓝盾")
    BS,
    @ApiModelProperty("PREBULD")
    PREBUILD,
    @ApiModelProperty("CI")
    CI
}