package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("命中动态组情况")
data class PipelineViewMatchDynamic(
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("标签列表")
    val labels: List<LabelInfo>
) {
    @ApiModel("标签信息")
    data class LabelInfo(
        @ApiModelProperty("标签分组id", required = false)
        val groupId: String,
        @ApiModelProperty("标签id列表", required = false)
        val labelIds: List<String>
    )
}
