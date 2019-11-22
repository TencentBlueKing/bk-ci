package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂项目基类")
abstract class Repository(
    @ApiModelProperty("工蜂项目ID")
    open val gitProjectId: Long,
    @ApiModelProperty("工蜂项目名")
    open val name: String,
    @ApiModelProperty("工蜂项目url")
    open val url: String,
    @ApiModelProperty("homepage")
    open val homepage: String,
    @ApiModelProperty("gitHttpUrl")
    open val gitHttpUrl: String,
    @ApiModelProperty("gitSshUrl")
    open val gitSshUrl: String
)
