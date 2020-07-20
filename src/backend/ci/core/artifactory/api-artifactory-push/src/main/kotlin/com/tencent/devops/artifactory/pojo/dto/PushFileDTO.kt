package com.tencent.devops.artifactory.pojo.dto

import com.tencent.devops.artifactory.pojo.FileResourceInfo
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分发文件入参")
class PushFileDTO(
    @ApiModelProperty("待分发文件信息", required = true)
    val fileInfo: FileResourceInfo,
    @ApiModelProperty("目标机器信息", required = true)
    val remoteResourceInfo: RemoteResourceInfo
)