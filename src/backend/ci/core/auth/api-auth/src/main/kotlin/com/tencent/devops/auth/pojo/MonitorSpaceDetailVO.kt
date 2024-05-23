package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "监控空间详情")
data class MonitorSpaceDetailVO(
    @get:Schema(title = "空间自增id")
    val id: Long?,
    @get:Schema(title = "空间名称")
    @JsonProperty("space_name")
    val spaceName: String?,
    @get:Schema(title = "空间类型")
    @JsonProperty("space_type_id")
    val spaceTypeId: String?,
    @get:Schema(title = "空间ID")
    @JsonProperty("space_id")
    val spaceId: String?,
    @get:Schema(title = "空间UID")
    @JsonProperty("space_uid")
    val spaceUid: String?,
    @get:Schema(title = "空间状态")
    val status: String?,
    @get:Schema(title = "创建人")
    val creator: String?
)
