package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "监控空间更新实体类")
data class MonitorSpaceUpdateInfo(
    @Schema(title = "空间名称")
    @JsonProperty("space_name")
    val spaceName: String,
    @Schema(title = "空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String,
    @Schema(title = "空间UID")
    @JsonProperty("space_uid")
    val spaceUid: String,
    @Schema(title = "更新人")
    val updater: String
)
