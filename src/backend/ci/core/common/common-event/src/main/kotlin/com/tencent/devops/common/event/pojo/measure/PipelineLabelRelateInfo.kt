package com.tencent.devops.common.event.pojo.measure

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线关联标签数据")
data class PipelineLabelRelateInfo(
    @get:Schema(title = "项目id")
    val projectId: String,
    @get:Schema(title = "流水线id")
    val pipelineId: String? = null,
    @get:Schema(title = "标签id")
    val labelId: Long? = null,
    @get:Schema(title = "标签名称")
    val name: String? = null,
    @get:Schema(title = "创建者")
    val createUser: String? = null,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime? = null
)
