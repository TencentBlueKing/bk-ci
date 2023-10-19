package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("监控空间创建实体类")
data class MonitorSpaceCreateInfo(
    @ApiModelProperty("空间名称")
    @JsonProperty("space_name")
    val spaceName: String,
    @ApiModelProperty("空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String,
    @ApiModelProperty("空间ID")
    @JsonProperty("space_id")
    val spaceId: String,
    @ApiModelProperty("创建人")
    val creator: String
)
