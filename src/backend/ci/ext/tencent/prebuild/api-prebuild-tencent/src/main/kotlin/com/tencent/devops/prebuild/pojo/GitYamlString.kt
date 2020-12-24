package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Yaml内容")
data class GitYamlString(
    @ApiModelProperty("YAML文件内容")
    val yaml: String
)
