package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂项目配置")
data class GitProjectConf(
    @ApiModelProperty("工蜂项目ID")
    val gitProjectId: Long,
    @ApiModelProperty("工蜂项目名")
    val name: String,
    @ApiModelProperty("工蜂项目url")
    val url: String,
    @ApiModelProperty("是否可以启用CI")
    val enable: Boolean,
    @ApiModelProperty("创建时间")
    val createTime: Long?,
    @ApiModelProperty("修改时间")
    val updateTime: Long?
)