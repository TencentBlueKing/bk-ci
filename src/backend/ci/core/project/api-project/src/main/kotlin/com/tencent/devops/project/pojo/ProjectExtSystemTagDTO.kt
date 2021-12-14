package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目ext系统consul tag修改入参")
data class ProjectExtSystemTagDTO(
    @JsonProperty(value = "routerTag", required = true)
    @ApiModelProperty("项目对应的router tags", name = "routerTag")
    val routerTag: String,
    @JsonProperty(value = "projectCodeList", required = false)
    @ApiModelProperty("项目编码集合", name = "projectCodeList")
    val projectCodeList: List<String>,
    @JsonProperty(value = "system", required = true)
    @ApiModelProperty("扩展系统名称, 此处不用枚举方便后续扩展", name = "system")
    val system: String
)
