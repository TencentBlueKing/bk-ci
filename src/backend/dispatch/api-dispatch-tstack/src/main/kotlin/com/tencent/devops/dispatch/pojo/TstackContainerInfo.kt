package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import com.tencent.devops.dispatch.pojo.enums.TstackContainerStatus

@ApiModel("TStack虚拟机构建状态")
data class TstackContainerInfo(
    @ApiModelProperty("项目 ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线 ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("vmSeqId", required = true)
    val vmSeqId: String,
    @ApiModelProperty("vm ID", required = true)
    var vmId: Long,
    @ApiModelProperty("tstackVmId", required = true)
    var tstackVmId: String,
    @ApiModelProperty("vmIp", required = true)
    var vmIp: String,
    @ApiModelProperty("vmName", required = true)
    var vmName: String,
    @ApiModelProperty("volumeId", required = true)
    var volumeId: String?,
    @ApiModelProperty("vncToken", required = true)
    var vncToken: String?,
    @ApiModelProperty("debugOn", required = true)
    var debugOn: Boolean,
    @ApiModelProperty("status", required = true)
    var status: TstackContainerStatus
)