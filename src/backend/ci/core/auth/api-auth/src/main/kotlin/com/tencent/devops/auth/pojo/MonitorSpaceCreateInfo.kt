package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "监控空间创建实体类")
data class MonitorSpaceCreateInfo(
    @Schema(description = "空间名称")
    @JsonProperty("space_name")
    val spaceName: String,
    @Schema(description = "空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String,
    @Schema(description = "空间ID")
    @JsonProperty("space_id")
    val spaceId: String,
    @Schema(description = "创建人")
    val creator: String
)
