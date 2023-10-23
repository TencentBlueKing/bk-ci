package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("监控空间更新实体类")
data class MonitorSpaceUpdateInfo(
    @ApiModelProperty("空间名称")
    @JsonProperty("space_name")
    val spaceName: String,
    @ApiModelProperty("空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String,
    @ApiModelProperty("空间UID")
    @JsonProperty("space_uid")
    val spaceUid: String,
    @ApiModelProperty("更新人")
    val updater: String
)
