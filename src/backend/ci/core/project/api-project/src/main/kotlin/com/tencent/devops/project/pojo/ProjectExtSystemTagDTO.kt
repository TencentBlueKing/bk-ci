package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目ext系统consul tag修改入参")
data class ProjectExtSystemTagDTO(
    @JsonProperty(value = "routerTag", required = true)
    @Schema(description = "项目对应的router tags", name = "routerTag")
    val routerTag: String,
    @JsonProperty(value = "projectCodeList", required = false)
    @Schema(description = "项目编码集合", name = "projectCodeList")
    val projectCodeList: List<String>,
    @JsonProperty(value = "system", required = true)
    @Schema(description = "扩展系统名称, 此处不用枚举方便后续扩展", name = "system")
    val system: String
)
