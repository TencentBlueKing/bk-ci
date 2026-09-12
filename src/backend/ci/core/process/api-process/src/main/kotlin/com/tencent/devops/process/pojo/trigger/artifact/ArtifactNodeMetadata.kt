package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品节点元数据项（bkrepo webhook payload 中 node.nodeMetadata 数组元素）
 */
@Schema(title = "制品节点元数据项")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactNodeMetadata(
    @get:Schema(title = "元数据键")
    val key: String? = null,
    @get:Schema(title = "元数据值")
    val value: Any? = null,
    @get:Schema(title = "是否系统元数据")
    val system: Boolean? = null,
    @get:Schema(title = "描述")
    val description: String? = null
)
