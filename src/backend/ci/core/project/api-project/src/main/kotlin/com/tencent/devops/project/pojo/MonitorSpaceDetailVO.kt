package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("监控空间详情")
data class MonitorSpaceDetailVO(
    @ApiModelProperty("空间自增id")
    val id: Long,
    @ApiModelProperty("空间名称")
    @JsonProperty("space_name")
    val spaceName: String,
    @ApiModelProperty("空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String,
    @ApiModelProperty("空间ID")
    @JsonProperty("space_id")
    val spaceId: String,
    @ApiModelProperty("空间UID")
    @JsonProperty("space_uid")
    val spaceUid: String,
    @ApiModelProperty("空间状态")
    val status: String,
    @ApiModelProperty("创建人")
    val creator: String
)
