package com.tencent.devops.common.event.pojo.measure

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("流水线关联标签数据")
data class PipelineLabelRelateInfo(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String? = null,
    @ApiModelProperty("标签id")
    val labelId: Long? = null,
    @ApiModelProperty("标签名称")
    val name: String? = null,
    @ApiModelProperty("创建者")
    val createUser: String? = null,
    @ApiModelProperty("创建时间")
    val createTime: LocalDateTime? = null
)
