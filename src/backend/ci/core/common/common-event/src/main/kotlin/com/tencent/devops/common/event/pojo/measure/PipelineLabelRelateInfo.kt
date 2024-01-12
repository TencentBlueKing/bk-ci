package com.tencent.devops.common.event.pojo.measure

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "流水线关联标签数据")
data class PipelineLabelRelateInfo(
    @Schema(name = "项目id")
    val projectId: String,
    @Schema(name = "流水线id")
    val pipelineId: String? = null,
    @Schema(name = "标签id")
    val labelId: Long? = null,
    @Schema(name = "标签名称")
    val name: String? = null,
    @Schema(name = "创建者")
    val createUser: String? = null,
    @Schema(name = "创建时间")
    val createTime: LocalDateTime? = null
)
