package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-构建曲线")
data class TrendInfoDto(
    @ApiModelProperty("曲线图谱")
    var trendData: Map<String, List<ArtifactoryInfo>>
)
