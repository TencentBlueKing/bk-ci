package com.tencent.devops.environment.pojo

import com.tencent.devops.common.api.pojo.Zone
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DevCloud虚拟机参数")
data class DevCloudVmParam(
    @ApiModelProperty("镜像Id", required = true)
    val imageId: String,
    @ApiModelProperty("机型", required = true)
    val modelId: String,
    @ApiModelProperty("数量", required = true)
    val instanceCount: Int,
    @ApiModelProperty("区域", required = true)
    val zone: Zone,
    @ApiModelProperty("有效期", required = true)
    val validity: Int?
)