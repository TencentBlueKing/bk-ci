package com.tencent.devops.process.pojo.classify

import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.pojo.classify.enums.Logic
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线视图表单")
data class PipelineViewForm(
    @ApiModelProperty("ID", required = false)
    val id: String? = null,
    @ApiModelProperty("视图名称", required = false)
    var name: String,
    @ApiModelProperty("是否项目", required = false)
    val projected: Boolean,
    @ApiModelProperty("流水线组类型,1--动态,2--静态")
    var viewType: Int = PipelineViewType.UNCLASSIFIED,
    @ApiModelProperty("逻辑符", required = false)
    val logic: Logic = Logic.AND,
    @ApiModelProperty("流水线视图过滤器列表", required = false)
    val filters: List<PipelineViewFilter> = emptyList(),
    @ApiModelProperty("流水线列表", required = false)
    val pipelineIds: List<String>? = null
)
