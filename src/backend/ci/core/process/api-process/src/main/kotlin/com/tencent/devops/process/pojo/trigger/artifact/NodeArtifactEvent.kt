package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 节点事件（二进制文件制品）
 *
 * bkrepo webhook payload 含顶层 `node` 对象。
 */
@Schema(title = "节点事件（二进制文件制品）")
@JsonIgnoreProperties(ignoreUnknown = true)
data class NodeArtifactEvent(
    override val eventType: String,
    override val user: ArtifactEventUser,
    @get:Schema(title = "节点信息")
    val node: ArtifactNode
) : ArtifactEvent() {
    override fun getAssociationId(): String = "${node.projectId}:${node.repoName}"
}
