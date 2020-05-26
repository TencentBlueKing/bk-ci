package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分发目标资源信息")
class RemoteResourceInfo(
    @ApiModelProperty("账号", required = true)
    val account: String,
    @ApiModelProperty("目标路径", required = true)
    val targetPath: String,
    @ApiModelProperty("获取目标机器类型", required = true)
    val pushType: PushTypeEnum,
    @ApiModelProperty("目标机信息,支持多个目标,以,分割", required = true)
    val targetMachine: String,
    @ApiModelProperty("超时时间")
    val timeout: Long?
)