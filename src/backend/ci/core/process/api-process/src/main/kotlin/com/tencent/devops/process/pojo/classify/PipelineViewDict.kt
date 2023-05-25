package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线组与流水线的对应关系")
data class PipelineViewDict(
    @ApiModelProperty("个人流水线组列表")
    val personalViewList: List<ViewInfo>,
    @ApiModelProperty("项目流水线列表")
    val projectViewList: List<ViewInfo>
) {
    @ApiModel("流水线组信息")
    data class ViewInfo(
        @ApiModelProperty("流水线组ID")
        val viewId: String,
        @ApiModelProperty("流水线组名")
        val viewName: String,
        @ApiModelProperty("流水线列表")
        val pipelineList: List<PipelineInfo>
    ) {
        @ApiModel("流水线信息")
        data class PipelineInfo(
            @ApiModelProperty("流水线ID")
            val pipelineId: String,
            @ApiModelProperty("流水线名称")
            val pipelineName: String,
            @ApiModelProperty("流水线组ID")
            val viewId: String,
            @ApiModelProperty("是否删除")
            val delete: Boolean
        )
    }

    companion object {
        val EMPTY = PipelineViewDict(emptyList(), emptyList())
    }
}
