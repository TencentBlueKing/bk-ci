package com.tencent.devops.artifactory.pojo

import com.tencent.devops.common.archive.pojo.TaskReport
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线产出物")
data class PipelineOutput(
    @ApiModelProperty("构件产出物")
    val artifacts: List<FileInfo>,
    @ApiModelProperty("报告产出物")
    val reports: List<TaskReport>
)
