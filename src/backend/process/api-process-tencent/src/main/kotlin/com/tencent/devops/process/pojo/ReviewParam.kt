package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-用户信息")
data class ReviewParam(
//    userId: String, projectId: String, pipelineId: String, buildId: String, elementId: String
//    @ApiModelProperty("主键ID", required = false)
//    var id: Long,
    @ApiModelProperty("项目Id", required = true)
    var projectId: String? = "",
    @ApiModelProperty("流水线Id", required = true)
    var pipelineId: String? = "",
    @ApiModelProperty("构建Id", required = true)
    var buildId: String? = "",
    @ApiModelProperty("审核人", required = true)
    var reviewUsers: MutableList<String> = mutableListOf(),
    @ApiModelProperty("审核结果", required = false)
    var status: ManualReviewAction? = null,
    @ApiModelProperty("描述", required = false)
    var desc: String? = "",
    @ApiModelProperty("审核意见", required = false)
    var suggest: String? = "",
    @ApiModelProperty("参数列表", required = false)
    var params: MutableList<NameAndValue> = mutableListOf()
)