package com.tencent.devops.store.pojo.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线信息")
data class AtomPipeline(
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    var pipelineName: String,
    @ApiModelProperty("项目标识", required = true)
    val projectCode: String,
    @ApiModelProperty("所属项目")
    val projectName: String,
    @ApiModelProperty("所属BG")
    val bgName: String,
    @ApiModelProperty("所属部门")
    val deptName: String,
    @ApiModelProperty("所属中心")
    val centerName: String
)
