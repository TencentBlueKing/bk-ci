package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工作空间信息缓存")
data class WorkSpaceCacheInfo(
    @ApiModelProperty("工作空间关联秘钥")
    val sshKey: String,
    @ApiModelProperty("工作空间Host")
    val environmentHost: String,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String,
    val namespace: String,
    val curLaunchId: Int?
)
