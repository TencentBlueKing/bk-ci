package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目ext系统consul tag修改入参")
data class ProjectExtSystemTagDTO(
    @JsonProperty(value = "routerTag", required = true)
    @get:Schema(title = "项目对应的router tags", description = "routerTag")
    val routerTag: String,
    @JsonProperty(value = "projectCodeList", required = false)
    @get:Schema(title = "项目编码集合", description = "projectCodeList")
    val projectCodeList: List<String>,
    @JsonProperty(value = "system", required = true)
    @get:Schema(title = "扩展系统名称, 此处不用枚举方便后续扩展", description = "system")
    val system: String
)
