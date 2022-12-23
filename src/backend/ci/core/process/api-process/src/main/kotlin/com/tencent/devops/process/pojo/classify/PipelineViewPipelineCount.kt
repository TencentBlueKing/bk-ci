package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线组--详细数目")
data class PipelineViewPipelineCount(
    @ApiModelProperty("可查看流水线数目")
    val normalCount: Int,
    @ApiModelProperty("已删除流水线数目")
    val deleteCount: Int
) {
    companion object {
        val DEFAULT = PipelineViewPipelineCount(0, 0)
    }
}
