package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "监控空间详情")
data class MonitorSpaceDetailVO(
    @Schema(name = "空间自增id")
    val id: Long?,
    @Schema(name = "空间名称")
    @JsonProperty("space_name")
    val spaceName: String?,
    @Schema(name = "空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String?,
    @Schema(name = "空间ID")
    @JsonProperty("space_id")
    val spaceId: String?,
    @Schema(name = "空间UID")
    @JsonProperty("space_uid")
    val spaceUid: String?,
    @Schema(name = "空间状态")
    val status: String?,
    @Schema(name = "创建人")
    val creator: String?
)
